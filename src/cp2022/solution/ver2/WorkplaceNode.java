package cp2022.solution.ver2;

import cp2022.base.Workplace;

import java.util.concurrent.locks.Condition;

public class WorkplaceNode {
  private final WorkshopImpl workshop;
  private final Workplace workplace;
  private final Condition movePermitAvailable;
  private final Condition usePermitAvailable;
  private Worker movePermitOwner = null;
  private Worker usePermitOwner = null;
  public WorkplaceNode(WorkshopImpl workshop, Workplace workplace) {
    this.workshop = workshop;
    this.workplace = workplace;
    movePermitAvailable = workshop.getLock().newCondition();
    usePermitAvailable = workshop.getLock().newCondition();
  }

  public Workplace getWorkplace() {
    return workplace;
  }

  public void acquireMovePermit(Worker worker) throws InterruptedException {
    while (movePermitOwner != null)
      movePermitAvailable.await();
    movePermitOwner = worker;
  }

  public void releaseMovePermit(Worker worker) {
    if (movePermitOwner != worker)
      throw new IllegalStateException("Permit owned by " + movePermitOwner + ", not " + worker);
    movePermitOwner = null;
    movePermitAvailable.signal();
  }

  public void acquireUsePermit(Worker worker) throws InterruptedException {
    while (usePermitOwner != null)
      usePermitAvailable.await();
    usePermitOwner = worker;
  }

  public void releaseUsePermit(Worker worker) {
    if (usePermitOwner != worker)
      throw new IllegalStateException("Permit owned by " + usePermitOwner + ", not " + worker);
    usePermitOwner = null;
    usePermitAvailable.signal();
  }
}
