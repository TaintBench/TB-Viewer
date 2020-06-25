package de.upb.swt.tbviewer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

/** @author Linghui Luo */
public class Logger {
  private static PrintWriter writer;
  private static FileOutputStream logStream;

  static {
    String tempDir = System.getProperty("java.io.tmpdir");
    String seperator = System.getProperty("file.separator");
    if (!tempDir.endsWith(seperator)) {
      tempDir += seperator;
    }
    File log = new File(tempDir + "taintBench_trace.log");
    try {
      logStream = new FileOutputStream(log);
      writer = new PrintWriter(logStream);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  public static void log(String location, String msg) {
    String output = location + ": " + msg;
    System.err.println(output);
    writer.println(output);
    writer.flush();
  }
}
