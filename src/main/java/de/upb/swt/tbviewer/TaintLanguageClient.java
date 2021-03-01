package de.upb.swt.tbviewer;

import magpiebridge.core.MagpieClient;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;

/** @author Linghui Luo */
public interface TaintLanguageClient extends MagpieClient {

  @JsonNotification("taintbench/groundtruth")
  void publishGroundTruth(PublishDiagnosticsParams diagnostics);

  @JsonNotification("taintbench/aqlresults")
  void publishAQLResult(PublishDiagnosticsParams diagnostics);

  @JsonNotification("taintbench/matchedresults")
  void publishMatchedResult(PublishDiagnosticsParams diagnostics);

  @JsonNotification("taintbench/unmatchedresults")
  void publishUnMatchedResult(PublishDiagnosticsParams diagnostics);
}
