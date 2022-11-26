package cp2022.solution.ver2;

public class Worker {
  private final long id;
  private WorkplaceNode currentWorkplace = null;
  private WorkplaceNode awaitedWorkplace = null;

  public Worker(long id) {
    this.id = id;
  }

  public WorkplaceNode getCurrentWorkplace() {
    return currentWorkplace;
  }

  public void setCurrentWorkplace(WorkplaceNode currentWorkplace) {
    this.currentWorkplace = currentWorkplace;
  }

  public WorkplaceNode getAwaitedWorkplace() {
    return awaitedWorkplace;
  }

  public void setAwaitedWorkplace(WorkplaceNode awaitedWorkplace) {
    this.awaitedWorkplace = awaitedWorkplace;
  }

  public long getId() {
    return id;
  }

}
