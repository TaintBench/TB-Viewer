The TB-Viewer reads TAF-file (findings) and displays the taint flows together with (decomplied) source code of an APK in VS Code. This extension is created for [TaintBench](https://taintbench.github.io).

# Usage
Watch this short [introduction video](https://www.youtube.com/watch?v=UQSHwN_aC9g&feature=youtu.be).

# How to build it?
- build jar file with `mvn install -DskipTests`
- `cd vscode`
- `npm install` (if the first time)
- `npm install -g vsce` (if the first time)
- `vsce package` (this will create vscode extension under vscode directory)
- `code --install-extension TaintBench-Viewer-0.0.1.vsix` (install the extension)
