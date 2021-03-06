# inspired by https://github.com/stevenalexander/docker-selenium and https://github.com/SeleniumHQ/docker-selenium
FROM selenium/base:3.0.1-aluminum
MAINTAINER Selenium <selenium-developers@googlegroups.com>

ENV DEBIAN_FRONTEND noninteractive
ENV DEBCONF_NONINTERACTIVE_SEEN true

#===================
# Timezone settings
# Possible alternative: https://github.com/docker/docker/issues/3359#issuecomment-32150214
#===================
ENV TZ "US/Pacific"
RUN echo "US/Pacific" | sudo tee /etc/timezone \
  && dpkg-reconfigure --frontend noninteractive tzdata

#==============
# PhantomJS
#==============
ARG PHANTOMJS_VERSION=2.1.1
RUN apt-get update -y
RUN apt-get install bzip2 libfreetype6 libfontconfig1  -y
RUN wget https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-$PHANTOMJS_VERSION-linux-x86_64.tar.bz2
RUN tar -xvjf phantomjs-$PHANTOMJS_VERSION-linux-x86_64.tar.bz2 && rm phantomjs-$PHANTOMJS_VERSION-linux-x86_64.tar.bz2
RUN mv /phantomjs-$PHANTOMJS_VERSION-linux-x86_64 /usr/local/phantomjs-$PHANTOMJS_VERSION-linux-x86_64
RUN ln -s /usr/local/phantomjs-$PHANTOMJS_VERSION-linux-x86_64/bin/phantomjs /usr/local/bin/phantomjs

#==============================
# Scripts to run Selenium Node
#==============================
COPY entry_point.sh /opt/bin/entry_point.sh
COPY config.json /opt/selenium/config.json
RUN chmod +x /opt/bin/entry_point.sh
RUN chown -R seluser:seluser /opt/selenium

#============================
# Some configuration options
#============================
ENV SCREEN_WIDTH 1360
ENV SCREEN_HEIGHT 1020
ENV SCREEN_DEPTH 24
ENV DISPLAY :99.0

USER seluser

# Following line fixes
# https://github.com/SeleniumHQ/docker-selenium/issues/87
ENV DBUS_SESSION_BUS_ADDRESS=/dev/null

ENTRYPOINT ["/opt/bin/entry_point.sh"]