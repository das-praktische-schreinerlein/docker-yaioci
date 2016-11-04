docker-yaioci - the ci-server for Le petit D'Artagnan
=====================================================

# Desc
A set of docker-images for a ci-system based on jenkins, sonarcube and postgres.

Inspired by an article of [Marcel Birkner](https://blog.codecentric.de/en/2015/10/continuous-integration-platform-using-docker-container-jenkins-sonarqube-nexus-gitlab)

Load it from [docker-yaioci on Github](https://github.com/das-praktische-schreinerlein/docker-yaioci).

# Installation

Download

    git clone https://github.com/das-praktische-schreinerlein/docker-yaioci.git
    cd docker-yaioci

Configure your VM and environment (cygwin recommend) as seen in tools/README.md
Maybe you must change the IP-Address: 192.168.99.100 to the one of your VM. 

Start ci-stack with bash-script

    do_yaioci_startfull
    
or manually with

    docker compose

See Jenkins on [http://192.168.99.100:18080/](http://192.168.99.100:18080/) with admin/admin
See SonarCube on [http://192.168.99.100:19000/](http://192.168.99.100:19000/) with admin/admin


