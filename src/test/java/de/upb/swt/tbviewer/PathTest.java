package de.upb.swt.tbviewer;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.nio.file.Paths;
import org.junit.Test;

public class PathTest {

  @Test
  public void test() {
    TaintServerAnalysis analysis = new TaintServerAnalysis();
    String folder = Paths.get("src/test/java").toAbsolutePath().toString();
    URL url = analysis.searchFile(folder, "de.upb.swt.tbviewer.PathTest");
    assertEquals(
        "file://E:/Git/Github/taintbench/GitPod-GITHUB/FrameworkTools/TB-Viewer/src/test/java/de/upb/swt/tbviewer/PathTest.java",
        url.toString());
  }
}
