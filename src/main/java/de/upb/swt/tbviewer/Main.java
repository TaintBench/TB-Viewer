/*
 * @author Linghui Luo
 */
package de.upb.swt.tbviewer;

import magpiebridge.core.IProjectService;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.core.ServerConfiguration;
import magpiebridge.core.ToolAnalysis;
import magpiebridge.projectservice.java.JavaProjectService;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/** @author Linghui Luo */
public class Main {
  public static void main(String... args) {
    ServerConfiguration config = new ServerConfiguration();
    config.setDoAnalysisByOpen(false);
    config.setDoAnalysisBySave(false);
    TaintLanguageServer server = new TaintLanguageServer(config);
    String language = "java";
    IProjectService javaProjectService = new JavaProjectService();
    server.addProjectService(language, javaProjectService);
    ServerAnalysis analysis = new TaintServerAnalysis();
    Either<ServerAnalysis, ToolAnalysis> either = Either.forLeft(analysis);
    server.addAnalysis(either, language);
    server.launchOnStdio();
    // server.launchOnSocket(5007);
  }
}
