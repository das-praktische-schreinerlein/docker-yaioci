#!/usr/bin/env bash
###################################
#
#
# Docker-YaioCI
#
#
###################################

DIR_DO_YAIOCI=/cygdrive/D/Projekte/docker-yaioci

do_yaioci_cd()
{
    cd ${DIR_DO_YAIOCI}
}
do_yaioci_start()
{
    do_yaioci_cd
    docker-compose up
}
do_yaioci_startfull()
{
    dom_start yaiovm
    do_yaioci_start
}
