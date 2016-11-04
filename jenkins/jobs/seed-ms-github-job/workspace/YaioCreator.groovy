def me = new LocalCreator(this);
def allJobConfigs = me.configureDefaultProjects('/home/d_projekte/', "das-praktische-scheinerlein", "MS")
me.createAllForProjectJobs(allJobConfigs)

/**
 def mavenProjects = ["yaio-commons": [defaultLayout: true]]
 def jsProjects = ["your-markdown-fellow": [defaultLayout: true]]

 def mavenJobConfigs = jobFramework.configureProjects(mavenProjects, localRepoBasePath, gitHubUser, prefix, 'maven');
 def jsJobConfigs = jobFramework.configureProjects(jsProjects, localRepoBasePath, gitHubUser, prefix, 'js');

 def allJobConfigs = []
 allJobConfigs.addAll(mavenJobConfigs)
 allJobConfigs.addAll(jsJobConfigs)
 me.createAllForProjectJobs(allJobConfigs)
 */


class LocalCreator {

    def jobFramework
    def viewFramework

    def LocalCreator(dslFactory) {
        jobFramework = new JobFramework(dslFactory)
        viewFramework = new ViewFramework(dslFactory)
    }

    def configureDefaultProjects(def localRepoBasePath, def gitHubUser, def prefix) {
        def mavenProjects = [
                "apppropagator": [defaultLayout: true,
                                  defaultBranch: 'master'],
                "yaio-webshot-service": [defaultLayout: true],
                "jsh-pegdown": [defaultLayout: true],
                "yaio-commons": [defaultLayout: true],
                "yaio-dms-service": [defaultLayout: true],
                "yaio-markdown-service": [defaultLayout: true],
                "yaio-metaextract-service": [defaultLayout: true],
                "yaio-plantuml-service": [defaultLayout: true],
                "yitf-dpsfinancetools": [defaultLayout: true,
                                         localDirName: 'DPSFinance',
                                         defaultBranch: 'master'],
                "your-all-in-one": [defaultLayout: false,
                                    localDirName: "yaio-playground",
                                    skipCobertura: true]
        ]
        def jsProjects = [
                "yaio-explorerapp": [defaultLayout: true],
                "your-markdown-fellow": [defaultLayout: false,
                                         localDirName: "ymf"],
                "schreinerleins-demo-desk": [defaultLayout: true]
        ]

        def mavenJobConfigs = jobFramework.configureProjectJobs(mavenProjects, localRepoBasePath, gitHubUser, prefix, 'maven');
        def jsJobConfigs = jobFramework.configureProjectJobs(jsProjects, localRepoBasePath, gitHubUser, prefix, 'js');

        def allJobConfigs = []
        allJobConfigs.addAll(mavenJobConfigs)
        allJobConfigs.addAll(jsJobConfigs)

        return allJobConfigs
    }

    def createAllForProjectJobs(def jobConfigs) {
        def jobs = jobFramework.createProjectJobs(jobConfigs)
        createBuildAllJob(jobs.buildJobs)
        createMetricsAllJob(jobs.metricJobs)
        createViews()
    }

    def createBuildAllJob(def buildJobs) {
        jobFramework.createJobAll("Build-MS-All", buildJobs)
    }

    def createMetricsAllJob(def metricJobs) {
        jobFramework.createJobAll("Metrics-MS-All", metricJobs)
    }

    def createViews() {
        viewFramework.createDefaultView('30-Build MS', /Build-MS-.*/)
        viewFramework.createDefaultView('40-Metrics MS', /Metrics-MS-.*/)
    }
}
