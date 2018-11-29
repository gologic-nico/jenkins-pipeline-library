# jenkins-pipeline-library
Ce projet vous permet de tester la livraison continue à l'aide de l'outil Robot Framework

Différents outils seront mis à contribution tel que :
* [Ansible](https://www.ansible.com/) 
* [Jenkins](https://jenkins.io/)
* [Docker](https://www.docker.com/)
* [Maven](https://maven.apache.org/)
* [Robot Framework](http://robotframework.org/)
* [Projet spring-boot-hello-world](https://github.com/gologic-nico/spring-boot-hello-world.git)

## Requis
* VM CentOs 7 vanille avec la base d'environnement "Infrastructure Server"
* Accès root

## Étape à suivre

### Connectez vous en ssh avec le user root
`ssh root@IpAddress`

### Ajouter le repo du package de Ansible

`yum install epel-release`

### Installer le package Ansible

`yum install ansible`

### Installer le package Git : 

`yum install git`

### Cloner le dépot du projet jenkins-pipeline-library pour mettre en place l'environnement de livraison continue

`git clone https://github.com/gologic-nico/jenkins-pipeline-library.git /tmp/jenkins-pipeline-library`

### Accéder le répertoire du playbook Ansible 

`cd /tmp/jenkins-pipeline-library/ansible`

### Lancer le playbook Ansible de Jenkins 
`ansible-playbook -i hosts install_jenkins.yml`

