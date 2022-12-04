package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;

public class XWorkplace {
  //  public final List<XWorker> workers = new ArrayList<>();
  public final BinarySemaphore movePermit;
  public final BinarySemaphore usePermit;
  private final XWorkshop workshop;
  private final Workplace workplace;

  public XWorkplace(XWorkshop workshop, Workplace workplace) {
    this.workshop = workshop;
    this.workplace = workplace;
    movePermit = new BinarySemaphore(workplace.getId().toString().replaceAll("^wId\\((.*)\\)$", "$1") + ".move");
    usePermit = new BinarySemaphore(workplace.getId().toString().replaceAll("^wId\\((.*)\\)$", "$1") + ".use");
  }

  public WorkplaceId getId() {
    return workplace.getId();
  }

  public Workplace getWorkplace() {
    return workplace;
  }

  public void acquireUsePermit(XWorker worker) throws InterruptedException {
    usePermit.acquire(worker);
  }

  public void releaseUsePermit(XWorker worker) {
    usePermit.release(worker);
  }

  @Override
  public String toString() {
    return Log.YELLOW + "[" + getId().toString().replace("wId(", "").replace(")", "") +
            ']' + Log.RESET;
  }

  public String getTrace() {
    return Log.YELLOW + "workplace " + this + Log.YELLOW + " \"" + getId() + "\"" + "\n" +
            Log.YELLOW + "  move permit = " + movePermit + "\n" +
            Log.YELLOW + "  use permit  = " + usePermit + "\n";
  }
}
