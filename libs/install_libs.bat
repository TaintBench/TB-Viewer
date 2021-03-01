call mvn install:install-file -Dfile=AQL-System-2.0.0-SNAPSHOT.jar -DgroupId=de.foellix -DartifactId=AQL-System -Dversion=2.0.0-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
call mvn install:install-file -Dfile=magpiebridge-0.0.6.jar -DgroupId=magpiebridge -DartifactId=magpiebridge -Dversion=0.0.6 -Dpackaging=jar -DgeneratePom=true
pause