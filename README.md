The taintviewer reads json file from the findings directory and display the taint flows in the editor.
# How to build it?
- build jar file with `mvn install -DskipTests`
- `cd vscode`
- `npm install` (if the first time)
- `npm install -g vsce` (if the first time)
- `vsce package` (this will create vscode extension under vscode directory)
- `code --install-extension TaintBench-Viewer-0.0.1.vsix` (install the extension)
