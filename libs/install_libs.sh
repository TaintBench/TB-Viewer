#!/bin/bash
LPATH="$( cd "$(dirname "$0")" ; pwd -P )"
mvn install:install-file -Dfile=$LPATH/AQL-System-2.0.0-SNAPSHOT.jar -DgroupId=de.foellix -DartifactId=AQL-System -Dversion=2.0.0-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LPATH/magpiebridge-0.0.6.jar -DgroupId=magpiebridge -DartifactId=magpiebridge -Dversion=0.0.6 -Dpackaging=jar -DgeneratePom=true