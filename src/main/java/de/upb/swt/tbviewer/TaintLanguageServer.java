/*
 * @author Linghui Luo
 */
package de.upb.swt.tbviewer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import magpiebridge.core.AnalysisResult;
import magpiebridge.core.MagpieClient;
import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerConfiguration;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher.Builder;
import org.eclipse.lsp4j.services.LanguageClient;

/** @author Linghui Luo */
public class TaintLanguageServer extends MagpieServer {

  private TaintLanguageClient taintClient;
  private Map<URL, List<Diagnostic>> unmatched;

  public TaintLanguageServer(ServerConfiguration config) {
    super(config);
    this.unmatched = new HashMap<>();
  }

  @Override
  public void initialized(InitializedParams params) {
    // set the rootPath for project service if it is not set yet.
    if (this.rootPath.isPresent()) {
      if (this.getProjectService("java").isPresent()) {
        this.getProjectService("java").get().setRootPath(this.rootPath.get());
      }
    }
    this.client.showMessage(
        new MessageParams(MessageType.Info, "The analyzer started analyzing the code."));
    doAnalysis("java", true);
  }

  public void consumeGroundTruth() {
    for (URL clientUri : this.diagnostics.keySet()) {
      List<Diagnostic> diagList = this.diagnostics.get(clientUri);
      List<Diagnostic> groundTruthPositiveList =
          diagList.stream()
              .filter(
                  d ->
                      d.getSeverity().equals(DiagnosticSeverity.Error)
                          || d.getSeverity().equals(DiagnosticSeverity.Information))
              .collect(Collectors.toList());
      if (!groundTruthPositiveList.isEmpty()) {
        PublishDiagnosticsParams pdp = new PublishDiagnosticsParams();
        pdp.setDiagnostics(groundTruthPositiveList);
        pdp.setUri(clientUri.toString());
        taintClient.publishGroundTruth(pdp);
      }
    }
  }

  public void consumeAQLResults() {
    for (URL clientUri : this.diagnostics.keySet()) {
      List<Diagnostic> diagList = this.diagnostics.get(clientUri);
      List<Diagnostic> aqlList =
          diagList.stream()
              .filter(d -> d.getSeverity().equals(DiagnosticSeverity.Warning))
              .collect(Collectors.toList());
      if (!aqlList.isEmpty()) {
        PublishDiagnosticsParams pdp = new PublishDiagnosticsParams();
        pdp.setDiagnostics(aqlList);
        pdp.setUri(clientUri.toString());
        taintClient.publishAQLResult(pdp);
      }
    }
  }

  public void consumeMatchedResults(Collection<AnalysisResult> matchedResults) {
    this.unmatched.clear();
    Set<Diagnostic> matchedAll = new HashSet<>();
    for (URL clientUri : this.diagnostics.keySet()) {
      List<Diagnostic> diagList = this.diagnostics.get(clientUri);
      List<Diagnostic> matchedList = new ArrayList<>();
      for (Diagnostic d : diagList) {
        if (!matchedResults.isEmpty()) {
          for (AnalysisResult r : matchedResults) {
            if (d.getMessage().equals(r.toString(false))) {
              matchedList.add(d);
              matchedAll.add(d);
            }
          }
        }
      }
      if (!matchedList.isEmpty()) {
        PublishDiagnosticsParams pdp = new PublishDiagnosticsParams();
        pdp.setDiagnostics(matchedList);
        pdp.setUri(clientUri.toString());
        taintClient.publishMatchedResult(pdp);
      }
    }

    for (URL clientUri : this.diagnostics.keySet()) {
      List<Diagnostic> diagList = this.diagnostics.get(clientUri);
      List<Diagnostic> unmatchedList = new ArrayList<>();
      for (Diagnostic d : diagList) {
        if (!containsDiagnostic(matchedAll, d)
            && d.getSeverity().equals(DiagnosticSeverity.Warning)) unmatchedList.add(d);
      }
      this.unmatched.put(clientUri, unmatchedList);
    }
    consumeUnMatchedResults();
  }

  private boolean containsDiagnostic(Set<Diagnostic> list, Diagnostic b) {
    for (Diagnostic a : list) {
      if (isSameDiagnostic(a, b)) return true;
    }
    return false;
  }

  private boolean isSameDiagnostic(Diagnostic a, Diagnostic b) {
    if (!a.getMessage().equals(b.getMessage())) return false;
    if (!a.getSeverity().equals(b.getSeverity())) return false;
    if (!a.getRange().equals(b.getRange())) return false;
    return true;
  }

  private void consumeUnMatchedResults() {
    for (URL clientUri : this.unmatched.keySet()) {
      if (!this.unmatched.get(clientUri).isEmpty()) {
        PublishDiagnosticsParams pdp = new PublishDiagnosticsParams();
        pdp.setDiagnostics(this.unmatched.get(clientUri));
        pdp.setUri(clientUri.toString());
        taintClient.publishUnMatchedResult(pdp);
      }
    }
  }

  @Override
  public void connect(LanguageClient client) {
    super.connect(client);
    this.taintClient = (TaintLanguageClient) client;
  }

  @Override
  public void launchOnStream(InputStream in, OutputStream out) {
    Launcher<TaintLanguageClient> launcher =
        new Builder<TaintLanguageClient>()
            .setLocalService(this)
            .setRemoteInterface(TaintLanguageClient.class)
            .setInput(in)
            .setOutput(out)
            .setExecutorService(Executors.newCachedThreadPool())
            .wrapMessages(this.logger.getWrapper())
            .create();
    connect(launcher.getRemoteProxy());
    launcher.startListening();
  }

  /**
   * Launch on socket port at local host. Will create a new {@link MagpieServer} instance for each
   * new connecting client using the given supplier. Example:
   *
   * <pre>
   * <code>
   *  Supplier&#60;MagpieServer&#62; supplier = ()-&#62;{
   *    MagpieServer server = new MagpieServer(new ServerConfiguration());
   *    String language = "java";
   *    IProjectService javaProjectService = new JavaProjectService();
   *    server.addProjectService(language, javaProjectService);
   *    return server.
   *  }
   * </code>
   * </pre>
   *
   * @param port the port
   * @param createServer the server supplier
   */
  public static void launchOnSocketPort(int port, Supplier<MagpieServer> createServer) {
    try {
      if (serverSocket != null) {
        serverSocket.close();
      }
      serverSocket = new ServerSocket(port);
      while (!serverSocket.isClosed()) {
        MagpieServer server = createServer.get();
        Socket connectionSocket = serverSocket.accept();
        Launcher<MagpieClient> launcher =
            new Builder<MagpieClient>()
                .setLocalService(server)
                .setRemoteInterface(TaintLanguageClient.class)
                .setInput(connectionSocket.getInputStream())
                .setOutput(connectionSocket.getOutputStream())
                .setExecutorService(THREAD_POOL)
                .create();
        server.connect(launcher.getRemoteProxy());
        new Thread(
                () -> {
                  try {
                    // wait for future to return, signaling connection was closed
                    launcher.startListening().get();
                  } catch (InterruptedException | ExecutionException e) {
                    MagpieServer.ExceptionLogger.log(e);
                    e.printStackTrace();
                  }
                },
                connectionSocket.getRemoteSocketAddress() + " connected")
            .start();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
