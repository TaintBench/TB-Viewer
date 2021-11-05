The TB-Viewer reads TAF-file (findings) and displays the taint flows in VS  code.
# How to build it?
- install AQL-System to your local .m2 repository by run the following command in the root folder.

`mvn install:install-file -Dfile=AQL-System-2.0.0-SNAPSHOT.jar -DgroupId=de.foellix -DartifactId=AQL-System -Dversion=2.0.0-SNAPSHOT -Dpackaging=jar -DgeneratePom=true`

- build jar file with `mvn install -DskipTests`
- `cd vscode`
- `npm install` (if the first time)
- `npm install -g vsce` (if the first time)
- `vsce package` (this will create vscode extension under vscode directory)
- `code --install-extension TaintBench-Viewer-0.0.1.vsix` (install the extension)
