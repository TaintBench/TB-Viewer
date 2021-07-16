# TB-Viewer
This extension displays the baseline ground truth defined in the [TaintBench](https://taintbench.github.io/) suite for benchmarking Android taint analyses. 
# Usage
- Short introduction video: https://youtu.be/UQSHwN_aC9g  
- For online usage: 
    - Find access to use it in GitPod on https://taintbench.github.io/taintbenchSuite
- For local usage:
    - Install the extension in Visual Studio Code
    - Clone a benchmark repository from https://taintbench.github.io/taintbenchSuite
    - Open the folder of your local benchmark repository in Visual Studio Code. Make sure the folder you opened in Visual Studio Code is the root folder of the local repository which contains the ***findings.json file.
    - Open any java file in local repository


The baseline ground truth are displayed on the left panel of the editor as shown in the screenshot below. Clicking the magnifying glass on the items in the left panel will open and highlight the relevant code in the editor. 
![screenshot](https://github.com/TaintBench/TB-Viewer/blob/develop/vscode/media/screenshot.PNG?raw=true)
