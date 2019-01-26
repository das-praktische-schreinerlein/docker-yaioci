class JobFramework {
    def dslFactory;

    def JobFramework(def dslFactory) {
        this.dslFactory = dslFactory;
    }

    def configureProjectJobs(def projects, def localRepoBasePath, def gitHubUser, def prefix, def type) {
        def jobConfigs = [];

        projects.keySet().each {
            def jobName = it

            def jobConfig = [name     : jobName,
                             full_name: jobName,
                             config   : projects.get(it),
                             repos    : [],
                             'prefix' : prefix,
                             'type': type]
            if (localRepoBasePath) {
                def fileUrl = localRepoBasePath + '/' + jobName + "/.git"
                if (jobConfig.config.localDirName) {
                    fileUrl = localRepoBasePath + '/' + jobConfig.config.localDirName + "/.git"
                }
                jobConfig.repos << fileUrl;
            }
            if (gitHubUser) {
                def gitHttpUrl = "https://github.com/" + gitHubUser + "/" + jobName + ".git"
                jobConfig.repos << gitHttpUrl;
            }

            jobConfigs << jobConfig
        }

        return jobConfigs
    }

    def configureGithubProjectJobs(def filterProjects, def localRepoBasePath, def gitHubUser, def prefix, def type) {
        def githubApi = new URL("https://api.github.com/users/" + gitHubUser + "/repos")
        def gitProjects = new groovy.json.JsonSlurper().parse(githubApi.newReader())

        def jobConfigs = [];

        gitProjects.each {
            def jobName=it.name
            def githubName=it.full_name
            def gitUrl=it.ssh_url
            def specialConfig
            if (filterProjects) {
                specialConfig = filterProjects.get(it)
            }

            def jobConfig = [name     : jobName,
                             full_name: githubName,
                             config   : specialConfig,
                             repos    : [],
                             ssh_url   : gitUrl,
                             'prefix' : prefix,
                             'type': type]
            if (localRepoBasePath) {
                def fileUrl = localRepoBasePath + '/' + jobName + "/.git"
                if (jobConfigs.config.localDirName) {
                    fileUrl = localRepoBasePath + '/' + jobConfigs.config.localDirName + "/.git"
                }
                jobConfig.repos << fileUrl;
            }
            if (gitHubUser) {
                def gitHttpUrl = "https://github.com/" + gitHubUser + "/" + jobName + ".git"
                jobConfig.repos << gitHttpUrl;
            }

            // no filter defined or filter matches
            if (!filterProjects || filterProjects.get(jobName)) {
                jobConfigs << jobConfig
            }
        }

        return jobConfigs
    }

    def createProjectJobs(def jobConfigs) {
        def buildJobs = []
        def metricJobs = []

        jobConfigs.each {
            def jobConfig = it
            // create project
            if (jobConfig.type == 'maven') {
                def jobResult = this.createMavenJob(jobConfig)
                buildJobs << jobResult.buildJobName
                metricJobs << jobResult.metricJobName
            } else if (jobConfig.type == 'js') {
                def jobResult = this.createJsJob(jobConfig)
                buildJobs << jobResult.buildJobName
                metricJobs << jobResult.metricJobName
            } else if (jobConfig.type == 'ts') {
                def jobResult = this.createTsJob(jobConfig)
                buildJobs << jobResult.buildJobName
                metricJobs << jobResult.metricJobName
            } else {
                println "Unknown Type: No Jobs ${jobName} for ${gitUrl} created"
            }
        }

        def jobs = [:];
        jobs.buildJobs = buildJobs
        jobs.metricJobs = metricJobs

        return jobs;
    }

    def createMavenJob(def config) {
        def jobName=config.name
        def buildJobName = "Build-${config.prefix}-Java-${jobName}"
        def metricJobName = "Metrics-${config.prefix}-Java-${jobName}"

        def result = [:];
        result.buildJobName = buildJobName
        result.metricJobName = metricJobName

        def defaultBranch = 'develop'
        if (config.config.defaultBranch) {
            defaultBranch = config.config.defaultBranch
        }

        println "Creating ${buildJobName} ${metricJobName}"
        dslFactory.job(buildJobName) {
            logRotator(-1, 10)
            parameters {
                stringParam('BRANCH', defaultBranch)
                choiceParam('GITURL', config.repos)
            }
            scm {
                git {
                    branch '$BRANCH'
                    remote {
                        url('${GITURL}')
                    }
                    createTag(false)
                }
            }
            configure { project->
                project / buildWrappers / 'hudson.plugins.sonar.SonarBuildWrapper' {
                    installationName 'sonarcube'
                }
            }
            steps {
                maven {
                    goals('clean')
                    goals('verify')
                    mavenInstallation('Maven 3.3.3')
                }
            }
            configure {
                it / publishers << 'hudson.plugins.parameterizedtrigger.BuildTrigger' {
                    configs {
                        'hudson.plugins.parameterizedtrigger.BuildTriggerConfig' {
                            configs {
                                'hudson.plugins.parameterizedtrigger.CurrentBuildParameters' ""
                            }
                            projects "${metricJobName}"
                            condition "SUCCESS"
                            triggerWithNoParameters false
                        }
                    }
                }
            }
        }
        dslFactory.job(metricJobName) {
            logRotator(-1, 10)
            parameters {
                stringParam('BRANCH', 'master')
                choiceParam('GITURL', config.repos)
            }
            scm {
                git {
                    branch '$BRANCH'
                    remote {
                        url('${GITURL}')
                    }
                    createTag(false)
                }
            }
            configure { project->
                project / buildWrappers / 'hudson.plugins.sonar.SonarBuildWrapper' {
                    installationName 'sonarcube'
                }
            }

            steps {
                maven {
                    goals('clean')
                    goals('verify')
                    if (!config.config.skipCobertura) {
                        goals("cobertura:cobertura-integration-test")
                    }
                    goals('$SONAR_MAVEN_GOAL')
                    properties(
                            [ 'sonar.host.url':'$SONAR_HOST_URL',
                              'sonar.jdbc.url':'$SONAR_JDBC_URL',
                              'sonar.jdbc.username':'$SONAR_JDBC_USERNAME',
                              'sonar.jdbc.password':'$SONAR_JDBC_PASSWORD',
                              'maven.test.failure.ignore':'true',
                              'cobertura.report.format':'xml'
                            ])
                    mavenInstallation('Maven 3.3.3')
                }
            }
            publishers {
                if (!config.config.skipCobertura) {
                    cobertura('**/target/site/cobertura/coverage.xml') {
                        autoUpdateHealth(false)
                        autoUpdateStability(true)
                        sourceEncoding('UTF_8')

                        // the following targets are added by default to check the method, line and conditional level coverage
                        methodTarget(80, 0, 0)
                        lineTarget(80, 0, 0)
                        conditionalTarget(70, 0, 0)
                    }
                }
            }
        }

        return result
    }

    def createJsJob(def config) {
        def jobName=config.name

        def buildJobName = "Build-${config.prefix}-JS-${jobName}"
        def metricJobName = "Metrics-${config.prefix}-JS-${jobName}"

        def defaultBranch = 'develop'
        if (config.config.defaultBranch) {
            defaultBranch = config.config.defaultBranch
        }

        def result = [:];
        result.buildJobName = buildJobName
        result.metricJobName = metricJobName

        println "Creating ${buildJobName} ${metricJobName}"
        dslFactory.job(buildJobName) {
            logRotator(-1, 10)
            parameters {
                stringParam('BRANCH', defaultBranch)
                choiceParam('GITURL', config.repos)
            }
            scm {
                git {
                    branch '$BRANCH'
                    remote {
                        url('${GITURL}')
                    }
                    createTag(false)
                }
            }
            configure { project->
                project / buildWrappers / 'hudson.plugins.sonar.SonarBuildWrapper' {
                    installationName 'sonarcube'
                }
                project / buildWrappers / 'jenkins.plugins.nodejs.tools.NpmPackagesBuildWrapper' {
                    nodeJSInstallationName 'nodejs'
                }
            }
            configure {
                it / 'builders' << 'hudson.tasks.Shell' {
                    command """npm install
npm run build
"""
                }
            }
            configure {
                it / publishers << 'hudson.plugins.parameterizedtrigger.BuildTrigger' {
                    configs {
                        'hudson.plugins.parameterizedtrigger.BuildTriggerConfig' {
                            configs {
                                'hudson.plugins.parameterizedtrigger.CurrentBuildParameters' ""
                            }
                            projects "${metricJobName}"
                            condition "SUCCESS"
                            triggerWithNoParameters false
                        }
                    }
                }
            }
        }
        dslFactory.job(metricJobName) {
            logRotator(-1, 10)
            parameters {
                stringParam('BRANCH', 'master')
                choiceParam('GITURL', config.repos)
            }
            scm {
                git {
                    branch '$BRANCH'
                    remote {
                        url('${GITURL}')
                    }
                    createTag(false)
                }
            }
            configure { project->
                project / buildWrappers / 'hudson.plugins.sonar.SonarBuildWrapper' {
                    installationName 'sonarcube'
                }
                project / buildWrappers / 'jenkins.plugins.nodejs.tools.NpmPackagesBuildWrapper' {
                    nodeJSInstallationName 'nodejs'
                }
            }
            configure {
                it / 'builders' << 'hudson.tasks.Shell' {
                    command """npm install
npm run build
"""
                }
            }
            configure {
                it / 'builders' << 'hudson.plugins.sonar.SonarRunnerBuilder' {
                    properties """
sonar.projectKey=de.yaio.${jobName}
sonar.projectName=${jobName}
sonar.projectVersion=\$BUILD_NUMBER
sonar.sources=src/main
sonar.tests=src/test/javascript
#sonar.language=js
sonar.javascript.lcov.reportPath=coverage/report-lcov/lcov.info
"""
                    javaOpts ''
                    jdk '(Inherit From Job)'
                    project ''
                    task ''
                }
            }
            publishers {
                if (!config.config.skipCobertura) {
                    cobertura('coverage/cobertura.xml') {
                        autoUpdateHealth(false)
                        autoUpdateStability(true)
                        sourceEncoding('UTF_8')

                        // the following targets are added by default to check the method, line and conditional level coverage
                        methodTarget(80, 0, 0)
                        lineTarget(80, 0, 0)
                        conditionalTarget(70, 0, 0)
                    }
                }
            }
        }

        return result
    }

    def createTsJob(def config) {
        def jobName=config.name

        def buildJobName = "Build-${config.prefix}-TS-${jobName}"
        def metricJobName = "Metrics-${config.prefix}-SS-${jobName}"

        def defaultBranch = 'develop'
        if (config.config.defaultBranch) {
            defaultBranch = config.config.defaultBranch
        }

        def result = [:];
        result.buildJobName = buildJobName
        result.metricJobName = metricJobName

        println "Creating ${buildJobName} ${metricJobName}"
        dslFactory.job(buildJobName) {
            logRotator(-1, 10)
            parameters {
                stringParam('BRANCH', defaultBranch)
                choiceParam('GITURL', config.repos)
            }
            scm {
                git {
                    branch '$BRANCH'
                    remote {
                        url('${GITURL}')
                    }
                    createTag(false)
                }
            }
            configure { project->
                project / buildWrappers / 'hudson.plugins.sonar.SonarBuildWrapper' {
                    installationName 'sonarcube'
                }
                project / buildWrappers / 'jenkins.plugins.nodejs.tools.NpmPackagesBuildWrapper' {
                    nodeJSInstallationName 'nodejs'
                }
            }
            configure {
                it / 'builders' << 'hudson.tasks.Shell' {
                    command """npm install
npm run build
#npm test
"""
                }
            }
            configure {
                it / publishers << 'hudson.plugins.parameterizedtrigger.BuildTrigger' {
                    configs {
                        'hudson.plugins.parameterizedtrigger.BuildTriggerConfig' {
                            configs {
                                'hudson.plugins.parameterizedtrigger.CurrentBuildParameters' ""
                            }
                            projects "${metricJobName}"
                            condition "SUCCESS"
                            triggerWithNoParameters false
                        }
                    }
                }
            }
        }
        dslFactory.job(metricJobName) {
            logRotator(-1, 10)
            parameters {
                stringParam('BRANCH', 'master')
                choiceParam('GITURL', config.repos)
            }
            scm {
                git {
                    branch '$BRANCH'
                    remote {
                        url('${GITURL}')
                    }
                    createTag(false)
                }
            }
            configure { project->
                project / buildWrappers / 'hudson.plugins.sonar.SonarBuildWrapper' {
                    installationName 'sonarcube'
                }
                project / buildWrappers / 'jenkins.plugins.nodejs.tools.NpmPackagesBuildWrapper' {
                    nodeJSInstallationName 'nodejs'
                }
            }
            configure {
                it / 'builders' << 'hudson.tasks.Shell' {
                    command """npm install
npm run build
#npm test
#npm run coverage
"""
                }
            }
            configure {
                it / 'builders' << 'hudson.plugins.sonar.SonarRunnerBuilder' {
                    properties """
sonar.projectKey=de.yaio.${jobName}
sonar.projectName=${jobName}
sonar.projectVersion=\$BUILD_NUMBER
sonar.sources=src/
sonar.exclusions=**/node_modules/**,**/*.spec.ts
sonar.tests=src
sonar.test.inclusions=**/*.spec.ts
#sonar.language=ts

sonar.ts.tslint.configPath=tslint.json
#sonar.ts.coverage.lcovReportPath=coverage/report-lcov/lcov.info
"""
                    javaOpts ''
                    jdk '(Inherit From Job)'
                    project ''
                    task ''
                }
            }
            publishers {
                if (!config.config.skipCobertura) {
                    cobertura('coverage/cobertura.xml') {
                        autoUpdateHealth(false)
                        autoUpdateStability(true)
                        sourceEncoding('UTF_8')

                        // the following targets are added by default to check the method, line and conditional level coverage
                        methodTarget(80, 0, 0)
                        lineTarget(80, 0, 0)
                        conditionalTarget(70, 0, 0)
                    }
                }
            }
        }

        return result
    }

    def createJobAll(def jobName, def jobs) {
        def jobNames = jobs.join(',')
        dslFactory.job(jobName) {
            logRotator(-1, 1)
            configure {
                it / 'publishers' << 'hudson.tasks.BuildTrigger' {
                    childProjects """${jobNames}"""
                    threshold {
                        name "FAILURE"
                        ordinal "2"
                        color "RED"
                        completeBuild "true"
                    }
                }
            }
        }
    }
}
