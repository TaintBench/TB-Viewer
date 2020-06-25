package de.upb.swt.tbviewer;

import java.util.ArrayList;

/** @author Linghui Luo */
public class TaintFlow {

  private Location source;
  private Location sink;
  private ArrayList<Location> intermediate;

  public TaintFlow(Location source, Location sink, ArrayList<Location> intermediate) {
    super();
    this.source = source;
    this.sink = sink;
    this.intermediate = intermediate;
  }

  public TaintFlow(Location source, Location sink) {
    super();
    this.source = source;
    this.sink = sink;
    this.intermediate = new ArrayList<>();
  }

  public boolean mayEqual(TaintFlow other, boolean compareIntermediate) {
    if (Location.maybeEqual(this.source, other.source, false)
        && Location.maybeEqual(this.sink, other.sink, false))
      if (!compareIntermediate) return true;
      else {
        if (this.intermediate.size() == other.intermediate.size()) {
          boolean allEqual = true;
          for (int i = 0; i < this.intermediate.size(); i++) {
            if (!Location.maybeEqual(this.intermediate.get(i), other.intermediate.get(i), false)) {
              allEqual = false;
            }
          }
          return allEqual;
        } else return false;
      }
    else return false;
  }

  @Override
  public String toString() {
    return "TaintFlow [source="
        + source
        + ", sink="
        + sink
        + ", intermediate="
        + intermediate
        + "]";
  }
}
