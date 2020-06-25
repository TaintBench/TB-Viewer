package de.upb.swt.tbviewer;

import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.services.LanguageClient;

/** @author Linghui Luo */
public interface TaintLanguageClient extends LanguageClient {

  @JsonNotification("taintbench/groundtruth")
  void publishGroundTruth(PublishDiagnosticsParams diagnostics);

  @JsonNotification("taintbench/aqlresults")
  void publishAQLResult(PublishDiagnosticsParams diagnostics);

  @JsonNotification("taintbench/matchedresults")
  void publishMatchedResult(PublishDiagnosticsParams diagnostics);

  @JsonNotification("taintbench/unmatchedresults")
  void publishUnMatchedResult(PublishDiagnosticsParams diagnostics);
}
