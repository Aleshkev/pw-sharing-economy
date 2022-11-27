package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;

public class XWorkplace {
  private final XWorkshop workshop;
  private final Workplace workplace;
  private final BinarySemaphore<XWorker> movePermit;
  private final BinarySemaphore<XWorker> usePermit;

  public XWorkplace(XWorkshop workshop, Workplace workplace) {
    this.workshop = workshop;
    this.workplace = workplace;
    movePermit = new BinarySemaphore<>(workshop, workplace.getId() + "/move");
    usePermit = new BinarySemaphore<>(workshop, workplace.getId() + "/use");
  }

  public WorkplaceId getId() {
    return workplace.getId();
  }

  public Workplace getWorkplace() {
    return workplace;
  }

  public void acquireMovePermit(XWorker worker) throws InterruptedException {
    movePermit.acquire(worker);
  }

  public void releaseMovePermit(XWorker worker) {
    movePermit.release(worker);
  }

  public void acquireUsePermit(XWorker worker) throws InterruptedException {
    usePermit.acquire(worker);
  }

  public void releaseUsePermit(XWorker worker) {
    usePermit.release(worker);
  }

  @Override
  public String toString() {
    return Log.YELLOW + "[" +
            "id=" + getId() +
            ", " + movePermit + Log.YELLOW +
            ", " + usePermit + Log.YELLOW +
            ']' + Log.RESET;
  }

  public void trace() {
    System.out.println(toString());
    System.out.println("    " + movePermit);
    System.out.println("    " + usePermit);
  }
}
