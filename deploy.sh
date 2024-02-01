#!/bin/bash

# Se déplacer dans le dossier /gps_assilassime
cd gps_assilassime/

# Mettre à jour le dépôt git avec la branche main
git pull git@github.com:darrell96kidjo/assilassime-gps.git main

# Se déplacer dans le dossier utils
cd src/main/java/com/sicmagroup/assilassimegps/utils

# Se rendre dans le répertoire /home/gprwebserver/depot/cloned/gps_assilassime
cd /home/gprwebserver/depot/cloned/gps_assilassime

# Compiler le projet Spring Boot en JAR en sautant les tests
mvn clean package

# Se rendre dans le dossier target
cd target

# Demander si c'est pour le test ou la production
read -p "S'agit-il d'un environnement de test (1) ou de production (2) ? " gprEnv

# Demander le nom de l'institution
read -p "Entrez le nom de l'institution. Exp: sicma : " instit

#créer le dossier insti
echo "Creation du dossier de $instit";
mkdir -p "$instit"

# Copier le fichier jar dans le répertoire approprié
if [ $gprEnv -eq 1 ]; then
    mkdir -p /home/gprwebserver/tests/$instit
    cp gpr-2022.1.26_build_182751.jar /home/gprwebserver/tests/$instit/
else
    mkdir -p /home/gprwebserver/prod/$instit
    cp gpr-2022.1.26_build_182751.jar /home/gprwebserver/prod/$instit/
fi

# Relancer les services en cours sur le VPS
systemctl daemon-reload

# Se déplacer dans le dossier Utils
cd ../
cd src/main/java/com/sicma/gpr/utils

ls