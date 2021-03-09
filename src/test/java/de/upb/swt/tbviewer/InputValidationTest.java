package de.upb.swt.tbviewer;

import java.io.File;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import org.junit.Test;

public class InputValidationTest {
  @Test
  public void test() throws XMLStreamException, IOException {
    String fileName =
        "E:\\Git\\Github\\taintbench\\GitPod-GITHUB\\AppRepos\\backflash\\backflash_fd_result.xml";
    // String fileName=
    // "C:\\Users\\linghui\\Downloads\\Raw-Results-of-FlowDroid-2.7.1-GenCG-Spring\\backflash_result.xml";
    File file = new File(fileName);
    InputValidation.isFlowDroidFormat(file);

    FlowDroidResultParser.convertToAQL(FlowDroidResultParser.readResultsWithPath(fileName));
  }
}
