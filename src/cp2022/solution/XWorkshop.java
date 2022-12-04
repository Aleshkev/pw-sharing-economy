package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;
import cp2022.base.Workshop;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


class XWorkshop implements Workshop {
  static XWorkshop mostRecentlyCreatedInstance;

  private final ReentrantLock lock = new ReentrantLock(true);
  private final Map<WorkplaceId, XWorkplace> workplaces;  // Immutable.
  private final Map<Long, XWorker> workers = new HashMap<>();

  private final Metasemaphore metasemaphore;

  public XWorkshop(Collection<Workplace> workplaces) {
    this.workplaces = workplaces.stream().collect(Collectors.toUnmodifiableMap(Workplace::getId, x -> new XWorkplace(this, x)));
    metasemaphore = new Metasemaphore(this);
    mostRecentlyCreatedInstance = this;
  }

  public ReentrantLock getLock() {
    return lock;
  }

  @Override
  public Workplace enter(WorkplaceId newWorkplaceId) {
    lock.lock();

    var worker = workers.computeIfAbsent(getWorkerId(), id -> new XWorker(this, id, Thread.currentThread().getName()));
    var newWorkplace = workplaces.get(newWorkplaceId);

    Log.info(worker, "enter.begin", newWorkplaceId);

    metasemaphore.startWaiting(worker, 2 * workplaces.size() - 1);

    Log.info(worker, "enter.acquireEntryPermit", newWorkplace, metasemaphore);

    InterruptableAction.run(() -> metasemaphore.acquirePermit(worker));

    Log.info(worker, "enter.acquireUsePermit", newWorkplace.usePermit);

    worker.setAwaitedWorkplace(newWorkplace);
    InterruptableAction.run(() -> newWorkplace.acquireUsePermit(worker));
    worker.setAwaitedWorkplace(null);
    worker.setCurrentWorkplace(newWorkplace);

    Log.info(worker, "enter.end", newWorkplace);

    return new DelayUntilUse(newWorkplace.getWorkplace(), () -> {

      Log.info(worker, "enter-delayed.begin", newWorkplace);

      metasemaphore.stopWaiting(worker);

      Log.info(worker, "enter-delayed.end", newWorkplace);

      lock.unlock();
    });
  }

  @Override
  public Workplace switchTo(WorkplaceId newWorkplaceId) {
    lock.lock();

    var worker = workers.get(getWorkerId());
    var oldWorkplace = worker.getCurrentWorkplace();
    var newWorkplace = workplaces.get(newWorkplaceId);
    assert newWorkplace != null;

    Log.info(worker, "switchTo.begin", oldWorkplace, "→", newWorkplace);

    if (oldWorkplace == newWorkplace) {
      lock.unlock();
      return newWorkplace.getWorkplace();
    }

    var maybeCycle = new ArrayList<XWorkplace>();

    var node = newWorkplace;
    while (node != null && node != oldWorkplace) {
      if (node.usePermit.getOwner() == null || node.usePermit.getOwner().getAwaitedWorkplace() == null)
        break;
      maybeCycle.add(node);
      node = node.usePermit.getOwner().getAwaitedWorkplace();
    }
    boolean isSolvingCycle;
    if (node == oldWorkplace) {
      isSolvingCycle = true;
      var cycle = maybeCycle;

      Log.info(worker, Log.RED + "switchTo.solveCycle" + Log.RESET, "cycle =", oldWorkplace, "→", cycle, "→", oldWorkplace);

      for (var i = 0; i < cycle.size(); ++i) {
        cycle.get(i).usePermit.fixNextOwner(i == 0 ? worker : cycle.get(i - 1).usePermit.getOwner());
      }
      oldWorkplace.usePermit.fixNextOwner(cycle.get(cycle.size() - 1).usePermit.getOwner());
    } else {
      isSolvingCycle = false;
    }


    metasemaphore.startWaiting(worker, 2 * workplaces.size() - 1);

    worker.setAwaitedWorkplace(newWorkplace);
    if (!isSolvingCycle) {
      Log.info(worker, "switchTo.acquireUsePermit", "not solving a cycle", newWorkplace.usePermit);
      InterruptableAction.run(() -> newWorkplace.acquireUsePermit(worker));
    }
    worker.setAwaitedWorkplace(null);
    worker.setCurrentWorkplace(newWorkplace);

    Log.info(worker, "switchTo.end", newWorkplace);
    return new DelayUntilUse(newWorkplace.getWorkplace(), () -> {
      Log.info(worker, "switchTo-delayed.begin");

      metasemaphore.stopWaiting(worker);
      Log.info(worker, "switchTo-delayed.releaseOldUsePermit", oldWorkplace.usePermit);
      oldWorkplace.releaseUsePermit(worker);

      if (isSolvingCycle) {
        Log.info(worker, "switchTo-delayed.acquireUsePermit", "was solving a cycle", newWorkplace.usePermit);
        InterruptableAction.run(() -> newWorkplace.acquireUsePermit(worker));
      }


      Log.info(worker, "switchTo-delayed.end");
      lock.unlock();
    });
  }

  @Override
  public void leave() {
    lock.lock();
    var worker = workers.get(getWorkerId());
    var oldWorkplace = worker.getCurrentWorkplace();

    Log.info(worker, "leave.begin");

    oldWorkplace.releaseUsePermit(worker);
    worker.setCurrentWorkplace(null);

    Log.info(worker, "leave.end");
    lock.unlock();
  }

  @Override
  public synchronized String toString() {
    return "Workshop[]";
  }

  public String getTrace() {
    var s = new StringJoiner("");
    workplaces.values().forEach(x -> s.add(x.getTrace()));
    workers.values().forEach(x -> s.add(x.getTrace()));
    s.add(metasemaphore.getTrace());
    return s.toString();
  }

  private long getWorkerId() {
    return Thread.currentThread().getId();
  }

}
