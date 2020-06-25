package de.upb.swt.tbviewer;

import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import de.foellix.aql.datastructure.Answer;
import de.foellix.aql.datastructure.handler.AnswerHandler;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

/** @author Linghui Luo */
public class InputValidation {

  /*
   * Loosely check if all required attributes for a TAF file are there.
   */
  public static boolean isTAFformat(File file) {
    JsonParser parser = new JsonParser();
    JsonObject obj;
    try {
      obj = parser.parse(new FileReader(file)).getAsJsonObject();
      if (!obj.has("fileName")) return false;
      if (!obj.has("findings")) return false;
      JsonArray findings = obj.getAsJsonArray("findings");
      for (int i = 0; i < findings.size(); i++) {
        JsonObject finding = findings.get(i).getAsJsonObject();
        if (!finding.has("ID")) return false;
        if (!finding.has("description")) return false;
        if (!finding.has("attributes")) return false;
        if (!finding.has("source")) return false;
        else {
          JsonObject source = finding.get("source").getAsJsonObject();
          if (!source.has("statement")) return false;
          if (!source.has("methodName")) return false;
          if (!source.has("className")) return false;
        }
        if (!finding.has("sink")) return false;
        else {
          JsonObject sink = finding.get("sink").getAsJsonObject();
          if (!sink.has("statement")) return false;
          if (!sink.has("methodName")) return false;
          if (!sink.has("className")) return false;
        }
        if (!finding.has("intermediateFlows")) return false;
        else {
          JsonArray flows = finding.getAsJsonArray("intermediateFlows");
          for (int j = 0; j < flows.size(); j++) {
            JsonObject inter = flows.get(j).getAsJsonObject();
            if (!inter.has("ID")) return false;
            if (!inter.has("statement")) return false;
            if (!inter.has("methodName")) return false;
            if (!inter.has("className")) return false;
          }
        }
      }

    } catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
      e.printStackTrace();
    }
    return true;
  }

  public static boolean isAQLformat(File file) {
    Answer answer = AnswerHandler.parseXML(file);
    if (answer == null) return false;
    return true;
  }
}
