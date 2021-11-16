The TB-Viewer reads TAF-file (findings) and displays the taint flows in VS  code.
# How to build it?
- Make sure you have [Oracle JDK 8](https://www.oracle.com/de/java/technologies/javase/javase8u211-later-archive-downloads.html) and [Maven](https://maven.apache.org/download.cgi) installed. 
- (first time) install AQL-System to your local .m2 repository by running `libs/install_libs.sh` (linux or mac) or `libs/install_libs.bat`
- build jar file with `mvn install -DskipTests`
- `cd vscode`
- `npm install` (if the first time)
- `npm install -g vsce` (if the first time)
- `vsce package` (this will create vscode extension under vscode directory)
- `code --install-extension TaintBench-Viewer-A.B.C.vsix` (install the extension). Don't forget to change `A.B.C` to the current version number. 

