package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;

class XWorkplace {
  public final BinarySemaphore usePermit;
  public final Workplace workplace;

  public XWorkplace(Workplace workplace) {
    this.workplace = workplace;
    usePermit = new BinarySemaphore(workplace.getId().toString().replaceAll(
            "^wId\\((.*)\\)$", "$1") + ".use");
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
    return Log.YELLOW + "[" + getId().toString().replace("wId(", "").replace(
            ")", "") + ']' + Log.RESET;
  }

  public String getDetailedString() {
    return Log.YELLOW + "workplace " + this + Log.YELLOW + " \"" + getId() +
            "\"" + "\n" + Log.YELLOW + "  use permit  = " + usePermit + "\n";
  }
}
