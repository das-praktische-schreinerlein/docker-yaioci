#!/usr/bin/env bash
#############
# common docker-shortcuts
#############

# configure shared-base-src: all shared-projects are in that path
# the mapping to the docker-image will be:
# yourhost:DIRLOCAL_WINSHARED_BASE -> ALIAS_SHARED_BASE
# docker-machine:ALIAS_SHARED_BASE -> DIRDOCKER_SHARED_BASE
# now you can mount this into the docker-container with: "docker run -v DIRDOCKER_SHARED_BASE/project:PROJECT_DIR_IN_DOCKER_CONTAINER"
# and use it in docker docker via DIR_IN_DOCKER_CONTAINER
DIRLOCAL_WINSHARED_BASE="D:\\AppData\\docker\\shared\\"    # configure the local shared-path for virtualbox (on windows in windows-notation)
ALIAS_SHARED_BASE=d_docker_shared                 # alias for mapping into docker-machine
DIRDOCKER_SHARED_BASE=/docker_shared              # dir in the docker-machine

######
# add eventuelly other paths than your home-dir
######
# yourhost:DIRLOCAL_WIN_D_PROJEKTE_BASE -> ALIAS_D_PROJEKTE_BASE
# docker-machine:ALIAS_D_PROJEKTE_BASE -> DIRDOCKER_D_PROJEKTE_BASE
# now you can mount this into the docker-container with: "docker run -v DIRDOCKER_D_PROJEKTE_BASE/project:PROJECT_DIR_IN_DOCKER_CONTAINER"
# and use it in docker docker via DIR_IN_DOCKER_CONTAINER
DIRLOCAL_WIN_D_PROJEKTE_BASE="f:\\Projekte\\"     # configure the local jenkins_home-path for virtualbox (on windows in windows-notation)
ALIAS_D_PROJEKTE_BASE=d_projekte                  # alias for mapping into docker-machine
DIRDOCKER_D_PROJEKTE_BASE=/d_projekte             # dir in the docker-machine


##
# start virtualbox
##
do_vbox_start()
{
    echo "START do_vbox_start: start VirtualBox"
    VirtualBox &
    echo "DONE do_vbox_start: start VirtualBox"
}

##
# start virtualbox
##
do_vbox_manage()
{
    echo "START do_vbox_manage: manage VirtualBox $1 $2 $3 $4 $5 $6 $7 $8 $9"
    CMD="VBoxManage $1 $2 $3 $4 $5 $6 $7 $8 $9"
    echo "RUN do_vbox_manage: $CMD"
    $CMD
    echo "DONE do_vbox_manage: manage VirtualBox $1 $2 $3 $4 $5 $6 $7 $8 $9"
}

dom_configure_sharedfolder()
{
    MACHINE=$1
    DIRLOCAL=$2
    ALIAS=$3

    echo "START dom_configure_sharedfolder ${MACHINE}: step 2: delete+create shared folder ${DIRLOCAL} - ${ALIAS}"
    do_vbox_manage sharedfolder remove ${MACHINE} --name ${ALIAS}
    do_vbox_manage sharedfolder add ${MACHINE} --name ${ALIAS} --hostpath ${DIRLOCAL} --automount
    # enable symlinks
    do_vbox_manage setextradata ${MACHINE} VBoxInternal2/SharedFoldersEnableSymlinksCreate/${ALIAS}  1
    do_vbox_manage getextradata ${MACHINE} enumerate
    echo "DONE dom_configure_sharedfolder ${MACHINE}: step 2: delete+create shared folder ${DIRLOCAL} - ${ALIAS}"
}

dom_mount_sharedfolder()
{
    MACHINE=$1
    DIRDOCKER=$2
    ALIAS=$3

    echo "START dom_mount_sharedfolder ${MACHINE}: step 2: mount shared folder ${DIRDOCKER} - ${ALIAS}"
    dom_ssh ${MACHINE} "sudo mkdir ${DIRDOCKER}; sudo mount -t vboxsf ${ALIAS} ${DIRDOCKER};"
    dom_ssh ${MACHINE} "ls -l ${DIRDOCKER}"
    echo "DONE dom_mount_sharedfolder ${MACHINE}: step 2: mount shared folder ${DIRDOCKER} - ${ALIAS}"
}


dom_configure()
{
    MACHINE=$1
    echo "START dom_configure ${MACHINE}: configure docker"
    if [ "${MACHINE}" == "" ]; then
        echo "ERROR: parameter MACHINE not set!!!!!"
        return 1;
    fi

    echo "INFO dom_configure ${MACHINE}: step 1: stop docker-machine ${MACHINE}"
    dom_stop ${MACHINE}

    echo "INFO dom_configure ${MACHINE}: step 2: configure docker-machine ${MACHINE}"
    dom_configure_sharedfolder ${MACHINE} ${DIRLOCAL_WINSHARED_BASE} ${ALIAS_SHARED_BASE}
    dom_configure_sharedfolder ${MACHINE} ${DIRLOCAL_WIN_D_PROJEKTE_BASE} ${ALIAS_D_PROJEKTE_BASE}

    echo "INFO dom_configure ${MACHINE}: step 3: start docker-machine"
    dom_restart ${MACHINE}
    echo "DONE dom_configure ${MACHINE}: configure docker"
}


dom_start()
{
    MACHINE=$1
    echo "START dom_start ${MACHINE}: start docker-machine"
    if [ "${MACHINE}" == "" ]; then
        echo "ERROR: parameter MACHINE not set!!!!!"
        return 1;
    fi

    docker-machine start ${MACHINE}

    dom_mount_sharedfolder ${MACHINE} ${DIRDOCKER_SHARED_BASE} ${ALIAS_SHARED_BASE}
    dom_mount_sharedfolder ${MACHINE} ${DIRDOCKER_D_PROJEKTE_BASE} ${ALIAS_D_PROJEKTE_BASE}

    echo "INFO dom_start ${MACHINE}: configure EXTRA_ARGS"
    dom_ssh ${MACHINE} "sudo cat /var/lib/boot2docker/profile"
    dom_init ${MACHINE}
    echo "DONE dom_start ${MACHINE}: start docker-machine"
}

dom_stop()
{
    MACHINE=$1
    echo "START dom_stop ${MACHINE}: stop docker-machine"
    if [ "${MACHINE}" == "" ]; then
        echo "ERROR: parameter MACHINE not set!!!!!"
        return 1;
    fi

    docker-machine stop ${MACHINE}
    echo "DONE dom_stop ${MACHINE}: stop docker-machine"
}
dom_restart()
{
    MACHINE=$1
    echo "START dom_restart ${MACHINE}: restart docker-machine"
    if [ "${MACHINE}" == "" ]; then
        echo "ERROR: parameter MACHINE not set!!!!!"
        return 1;
    fi

    dom_stop ${MACHINE}
    dom_start ${MACHINE}
    echo "DONE dom_restart ${MACHINE}: restart docker-machine"
}

dom_init()
{
    MACHINE=$1
    echo "START dom_init ${MACHINE}: init docker-env"
    if [ "${MACHINE}" == "" ]; then
        echo "ERROR: parameter MACHINE not set!!!!!"
        return 1;
    fi

    # Run this command to configure your shell:
    eval "$(docker-machine env ${MACHINE} --shell bash)"
    CONFIG=$(docker-machine env ${MACHINE} --shell bash)

    # or manually done with "docker-machine env ${MACHINE}"
    #export DOCKER_TLS_VERIFY="1"
    #export DOCKER_HOST="tcp://169.254.85.100:2376"
    #export DOCKER_CERT_PATH="C:\Users\micha\.docker\machine\machines\${MACHINE}"
    #export DOCKER_MACHINE_NAME="${MACHINE}"

    # extract ip
    export DOCKER_IP=$(echo ${DOCKER_HOST}| sed -e 's/tcp:\/\/\(.*\):.*/\1/g')
    echo "RESULT dom_init ${MACHINE}: for ip=${DOCKER_IP}: ${CONFIG}"
}

dom_ssh()
{
    MACHINE=$1
    COMMAND=$2
    echo "START dom_ssh ${MACHINE}: ssh docker-machine with command ${COMMAND}"
    if [ "${MACHINE}" == "" ]; then
        echo "ERROR: parameter MACHINE not set!!!!!"
        return 1;
    fi

    docker-machine ssh ${MACHINE} "${COMMAND}"
    echo "DONE dom_ssh ${MACHINE}: ssh docker-machine with command ${COMMAND}"
}

###########
# docker image commands
###########
doi_build()
{
    echo "START doi_build: build dockerimage ${1}"
    docker build -t  ${1} .
    echo "DONE doi_build: build dockerimage ${1}"
}
doi_rmi_all()
{
    echo "START doi_rmi_all: remove all docker-images"
    # docker rmi $(docker images -a -q)
    echo "DONE doi_rmi_all: remove all docker-images"
}
doi_rmi_all_untagged()
{
    echo "START doi_rmi_all_untagged: remove all untagged docker-images"
    docker images -a | grep "<none>" | awk '{print $3}' | xargs docker rmi -f
    echo "DONE doi_rmi_all_untagged: remove all untagged docker-images"
}
doi_rmi_all_unused()
{
    echo "START doi_rmi_all_unused: remove all unused docker-images"
    docker rmi $(docker images --filter "dangling=true" -q --no-trunc)
    echo "DONE doi_rmi_all_unused: remove all unused docker-images"
}
doi_rm_unused_volumes()
{
    echo "START doi_rm_unused_volumes: remove all unused docker-volumes"
    docker volume ls -qf dangling=true | xargs -r docker volume rm
    echo "START doi_rm_unused_volumes: remove all unused docker-volumes"
}
doi_run()
{
    echo "START doi_run: run docker-image ${1}"
    CMD="docker run -e DOCKER_IP=${DOCKER_IP} -d -p 8666:8666 ${1}"
    echo "INFO doi_run: $CMD"
    $CMD
    echo "DONE doi_run: run docker-image ${1}"
}
doi_full()
{
   echo "START doi_full: kill,build and run docker-image ${1}"
   do_kill ${1}
   doi_build ${1}
   doi_run ${1}
   echo "DONE doi_full: kill,build and run docker-image ${1}"
}

###########
# docker container commands
###########
do_stop_all()
{
    echo "START do_stop_all: stop all running docker-container"
    docker stop $(docker ps -a -q)
    echo "DONE do_stop_all: stop all running docker-container"
}
do_kill_all()
{
    echo "START do_kill_all: kill all running docker-container"
    docker kill $(docker ps -a -q)
    echo "DONE do_kill_all: kill all running docker-container"
}
do_rm_all()
{
    echo "START do_rm_all: remove all docker-container"
    docker rm --volumes $(docker ps -a -q)
    echo "DONE do_rm_all: remove all docker-container"
}
do_rm_all_ended()
{
    echo "START do_rm_all_ended: remove all ended docker-containers"
    docker ps -a | grep "Exited" | awk '{print $1}' | xargs docker rm --volumes
    echo "DONE do_rm_all_ended: remove all ended docker-containers"
}
do_id()
{
    echo "START do_id: save container-id of ${1} in do_id"
    do_id=`docker ps -a | grep  ${1} | sed -e 's/\s.*$//'`
    echo "RESULT do_id: id for ${1}=$do_id"
}
do_exec()
{
    echo "START do_exec: exec command '$2 $3 $4 $5 $6 $7 $8 $9' for image ${1}"
    do_id  ${1}
    if [ ! -z "$do_id" ]; then
        CMD="docker exec -it ${do_id} $2 $3 $4 $5 $6 $7 $8 $9"
        echo "INFO do_exec $CMD"
        $CMD
        echo "DONE do_exec: exec command '$2 $3 $4 $5 $6 $7 $8 $9' for image ${1}"
    else
        echo "WARNING do_exec: image ${1} not running"
    fi
}
do_remove()
{
    echo "START do_remove: remove container ${1}"
    do_id  ${1}
    if [ ! -z "$do_id" ]; then
        do_kill ${1}
        CMD="docker rm --volumes ${do_id}"
        echo "INFO do_remove $CMD"
        $CMD
        echo "DONE do_remove: rm -it for image ${1}"
    else
        echo "WARNING do_remove: image ${1} not exists"
    fi
}
do_bash()
{
    echo "START do_bash: start bash for image ${1}"
    do_exec $1 bash
    echo "DONE do_bash: start bash for image ${1}"
}
do_logs()
{
    echo "START do_logs: show logs for image ${1}"
    do_id ${1}
    if [ ! -z "$do_id" ]; then
        docker logs -f --tail=$2 ${do_id}
        echo "DONE do_logs: show logs for image ${1}"
    else
        echo "WARNING do_logs: image ${1} not running"
    fi
}

do_stop()
{
    echo "START do_stop: stop docker-container ${1}"
    do_id ${1}
    if [ ! -z "$do_id" ]; then
        docker stop ${do_id}
        docker ps
        echo "DONE do_stop: stop docker-container ${1}"
    else
        echo "WARNING do_stop: container ${1} not running"
    fi
}

do_start()
{
    echo "START do_start: start docker-container ${1}"
    do_id ${1}
    if [ ! -z "$do_id" ]; then
        docker start $do_id
        docker ps
        echo "DONE do_start: start docker-container ${1}"
    else
        echo "WARNING do_start: container ${1} not exists"
    fi
    docker ps -a
    echo "DONE do_start: start docker-container ${1}"
}

do_kill()
{
    echo "START do_kill: kill docker-container ${1}"
    do_id ${1}
    if [ ! -z "$do_id" ]; then
        docker kill ${do_id}
        docker ps
        echo "DONE do_kill: kill docker-container ${1}"
    else
        echo "WARNING do_kill: container ${1} not running"
    fi
}


###########
# docker volume commands
###########
dov_rm_all_dangling()
{
    echo "START dov_rm_all_dangling: remove all dangling docker-volumes"
    docker volume rm `docker volume ls -q -f dangling=true`
    echo "DONE dov_rm_all_dangling: remove all dangling docker-volumes"
}



