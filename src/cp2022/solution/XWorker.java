package cp2022.solution;

import java.util.concurrent.locks.Condition;

public class XWorker {
  private final long id;
  private final String name;
  public Condition sleep;
  private XWorkplace currentWorkplace = null;
  private XWorkplace awaitedWorkplace = null;

  public XWorker(XWorkshop workshop, long id, String name) {
    this.sleep = workshop.getLock().newCondition();
    this.id = id;
    this.name = name;
  }

  public XWorkplace getCurrentWorkplace() {
    return currentWorkplace;
  }

  public void setCurrentWorkplace(XWorkplace currentWorkplace) {
    this.currentWorkplace = currentWorkplace;
  }

  public XWorkplace getAwaitedWorkplace() {
    return awaitedWorkplace;
  }

  public void setAwaitedWorkplace(XWorkplace awaitedWorkplace) {
    this.awaitedWorkplace = awaitedWorkplace;
  }

  @Override
  public String toString() {
    return Log.GREEN + "(" + id + ")" + Log.RESET;
  }

  public String getTrace() {
    return Log.GREEN + "worker " + this + Log.GREEN + " \"" + name + "\"" + "\n" +
            Log.GREEN + "  current workplace = " + currentWorkplace + "\n" +
            Log.GREEN + "  awaited workplace = " + awaitedWorkplace + "\n";
  }
}
