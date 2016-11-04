#
# Creates a docker container with SonarQube, incl. several plugins
# Since the original Dockerfile does not support plugins, I
# had to extend the Dockerfile
#
# Original: https://hub.docker.com/_/sonarqube/
#

FROM java:openjdk-8u45-jdk

MAINTAINER Marcel Birkner <marcel.birkner@codecentric.de>

ENV SONARQUBE_HOME /opt/sonarqube

# Http port
EXPOSE 9000

# H2 Database port
EXPOSE 9092

# Database configuration
# Defaults to using H2
ENV SONARQUBE_JDBC_USERNAME sonar
ENV SONARQUBE_JDBC_PASSWORD sonar
ENV SONARQUBE_JDBC_URL jdbc:h2:tcp://localhost:9092/sonar

ENV SONAR_VERSION 5.6.3

ENV SONAR_DOWNLOAD_URL https://sonarsource.bintray.com/Distribution

# pub   2048R/D26468DE 2015-05-25
#      Key fingerprint = F118 2E81 C792 9289 21DB  CAB4 CFCA 4A29 D264 68DE
# uid       [ unknown] sonarsource_deployer (Sonarsource Deployer) <infra@sonarsource.com>
# sub   2048R/06855C1D 2015-05-25
RUN gpg --keyserver ha.pool.sks-keyservers.net --recv-keys F1182E81C792928921DBCAB4CFCA4A29D26468DE

RUN set -x \
    && cd /opt \
    && curl -o sonarqube.zip -fSL https://sonarsource.bintray.com/Distribution/sonarqube/sonarqube-$SONAR_VERSION.zip \
    && curl -o sonarqube.zip.asc -fSL https://sonarsource.bintray.com/Distribution/sonarqube/sonarqube-$SONAR_VERSION.zip.asc \
    && gpg --verify sonarqube.zip.asc \
    && unzip sonarqube.zip \
    && mv sonarqube-$SONAR_VERSION sonarqube \
    && rm sonarqube.zip* \
    && rm -rf $SONARQUBE_HOME/bin/*

# Installing Plugins
RUN cd /opt/sonarqube/extensions/plugins/ \
    && curl -o sonar-cobertura-plugin-1.7.jar -fSL https://github.com/SonarQubeCommunity/sonar-cobertura/releases/download/1.7/sonar-cobertura-plugin-1.7.jar \
    && curl -o sonar-css-plugin-2.1.jar -fSL https://github.com/SonarQubeCommunity/sonar-css/releases/download/2.1/sonar-css-plugin-2.1.jar \
    && curl -o sonar-findbugs-plugin-3.4.4.jar -fSL https://github.com/SonarQubeCommunity/sonar-findbugs/releases/download/3.4.4/sonar-findbugs-plugin-3.4.4.jar \
    && curl -o sonar-java-plugin-4.2.jar -fSL $SONAR_DOWNLOAD_URL/sonar-java-plugin/sonar-java-plugin-4.2.jar \
    && curl -o sonar-jproperties-plugin-2.2.jar -fSL https://github.com/racodond/sonar-jproperties-plugin/releases/download/2.2/sonar-jproperties-plugin-2.2.jar \
    && curl -o sonar-json-plugin-2.1.jar -fSL https://github.com/racodond/sonar-json-plugin/releases/download/2.1/sonar-json-plugin-2.1.jar \
    && curl -o sonar-php-plugin-2.8.jar -fSL $SONAR_DOWNLOAD_URL/sonar-php-plugin/sonar-php-plugin-2.8.jar \
    && curl -o sonar-pmd-plugin-2.6.jar -fSL https://github.com/SonarQubeCommunity/sonar-pmd/releases/download/2.6/sonar-pmd-plugin-2.6.jar \
    && curl -o sonar-scm-git-plugin-1.1.jar -fSL http://downloads.sonarsource.com/plugins/org/codehaus/sonar-plugins/sonar-scm-git-plugin/1.1/sonar-scm-git-plugin-1.1.jar \
    && curl -o sonar-web-plugin-2.5.0.476.jar -fSL $SONAR_DOWNLOAD_URL/sonar-web-plugin/sonar-web-plugin-2.5.0.476.jar \
    && curl -o sonar-xml-plugin-1.4.1.jar -fSL $SONAR_DOWNLOAD_URL/sonar-xml-plugin/sonar-xml-plugin-1.4.1.jar \
    && cd /tmp

#    && rm /opt/sonarqube/extensions/plugins/qualinsight-plugins-sonarqube-smell-api-3.0.0.jar \
#    && curl -o qualinsight-plugins-sonarqube-smell-api-3.0.0.jar -fSL https://github.com/QualInsight/qualinsight-plugins-sonarqube-smell/releases/download/qualinsight-plugins-sonarqube-smell-3.0.0/qualinsight-plugins-sonarqube-smell-api-3.0.0.jar \
#    && curl -o sonar-checkstyle-plugin-2.4.jar -fSL $SONAR_DOWNLOAD_URL/sonar-checkstyle-plugin/sonar-checkstyle-plugin-2.5.jar \


VOLUME ["$SONARQUBE_HOME/data", "$SONARQUBE_HOME/extensions"]

WORKDIR $SONARQUBE_HOME
COPY run.sh $SONARQUBE_HOME/bin/
RUN chmod +x $SONARQUBE_HOME/bin/run.sh

ENTRYPOINT ["./bin/run.sh"]
