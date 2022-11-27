package cp2022.solution;

public class XWorker {
  private final long id;
  private XWorkplace currentWorkplace = null;
  private XWorkplace awaitedWorkplace = null;

  public XWorker(long id) {
    this.id = id;
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

  public long getId() {
    return id;
  }

  @Override
  public String toString() {
    return  Log.GREEN + "(" + id + ")" + Log.RESET;
//    return "XWorker[" +
//            "id=" + id +
//            ", currentWorkplace=" + currentWorkplace +
//            ", awaitedWorkplace=" + awaitedWorkplace +
//            ']';
  }

  public void trace() {
    System.out.println("XWorker[" +
            "id=" + id +
            ", currentWorkplace=" + currentWorkplace +
            ", awaitedWorkplace=" + awaitedWorkplace +
            "]"
    );
  }
}
