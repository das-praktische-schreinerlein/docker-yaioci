# Docker Common Utils

## Requires

- a running docker
- a bash-shell

## Install

Install docker [Windows-Installation](https://docs.docker.com/installation/windows/) or [MacOSX-Installation](https://docs.docker.com/installation/mac/)
and start the docker-machine

Include the bashprofiles with some docker-shortcuts into your .bashrc or load it with

    source PATH/docker-yaioci/tools/utils-docker-common.bash
    source PATH/docker-yaioci/tools/do_yaioci.bash

Configure the profile in PATH/devtools/docker/utils-docker-common.bash

    DIRLOCAL_WIN_D_PROJEKTE_BASE="D:\\Projekte\\"     # configure the local jenkins_home-path for virtualbox (on windows in windows-notation)
    ALIAS_D_PROJEKTE_BASE=d_projekte                  # alias for mapping into docker-machine
    DIRDOCKER_D_PROJEKTE_BASE=/d_projekte             # dir in the docker-machine

Configure on first start the sharedfolders on your docker-machine (windows...)

    dom_configure yaiovm

Start machine

    dom_start yaiovm

Stop machine

    dom_stop yaiovm
