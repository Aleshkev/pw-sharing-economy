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
    this.workplaces = workplaces.stream().collect(Collectors.toUnmodifiableMap(Workplace::getId,
                                                                               XWorkplace::new));
    metasemaphore = new Metasemaphore();

    mostRecentlyCreatedInstance = this;
//    DeadlockDetection.startIfEnabled();
  }

  public ReentrantLock getLock() {
    return lock;
  }

  @Override
  public Workplace enter(WorkplaceId newWorkplaceId) {
    lock.lock();

    var worker = workers.computeIfAbsent(getWorkerId(), id -> new XWorker(this, id,
                                                                          Thread.currentThread().getName()));
    var newWorkplace = workplaces.get(newWorkplaceId);

//    Log.info(worker, "enter.begin", newWorkplaceId);

    metasemaphore.startWaiting(worker, 2 * workplaces.size() - 1);

//    Log.info(worker, "enter.acquireEntryPermit", newWorkplace, metasemaphore);
    InterruptableAction.run(() -> metasemaphore.acquirePermit(worker));

//    Log.info(worker, "enter.acquireUsePermit", newWorkplace.usePermit);
    worker.setAwaitedWorkplace(newWorkplace);
    InterruptableAction.run(() -> newWorkplace.acquireUsePermit(worker));
    worker.setAwaitedWorkplace(null);
    worker.setCurrentWorkplace(newWorkplace);

//    Log.info(worker, "enter.end", newWorkplace);
    return new DelayUntilUse(newWorkplace.getWorkplace(), () -> {
//      Log.info(worker, "enter-delayed.begin", newWorkplace);

      metasemaphore.stopWaiting(worker);

//      Log.info(worker, "enter-delayed.end", newWorkplace);
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

//    Log.info(worker, "switchTo.begin", oldWorkplace, "→", newWorkplace);

    // Can't do anything else.
    if (oldWorkplace == newWorkplace) {
      lock.unlock();
      return newWorkplace.getWorkplace();
    }

    // Handling a cycle.
    var cycle = findCycle(oldWorkplace, newWorkplace);
    boolean isSolvingCycle = (cycle != null);
    if (isSolvingCycle) {
      handleCycle(worker, oldWorkplace, newWorkplace, cycle);
    }

    metasemaphore.startWaiting(worker, 2 * workplaces.size() - 1);

    worker.setAwaitedWorkplace(newWorkplace);
    if (!isSolvingCycle) {
//      Log.info(worker, "switchTo.acquireUsePermit", "not solving a cycle", newWorkplace.usePermit);
      InterruptableAction.run(() -> newWorkplace.acquireUsePermit(worker));
    }
    worker.setAwaitedWorkplace(null);
    worker.setCurrentWorkplace(newWorkplace);

//    Log.info(worker, "switchTo.end", newWorkplace);
    return new DelayUntilUse(newWorkplace.getWorkplace(), () -> {
//      Log.info(worker, "switchTo-delayed.begin");

      metasemaphore.stopWaiting(worker);
//      Log.info(worker, "switchTo-delayed.releaseOldUsePermit", oldWorkplace.usePermit);
      oldWorkplace.releaseUsePermit(worker);

      if (isSolvingCycle) {
//        Log.info(worker, "switchTo-delayed.acquireUsePermit", "was solving a cycle",
//                 newWorkplace.usePermit);
        InterruptableAction.run(() -> newWorkplace.acquireUsePermit(worker));
      }

//      Log.info(worker, "switchTo-delayed.end");
      lock.unlock();
    });
  }

  // Tries to find a cycle of dependencies containing the edge (oldWorkplace,
  // newWorkplace). Returns the nodes in the cycle except oldWorkplace and
  // newWorkplace.
  private List<XWorkplace> findCycle(XWorkplace oldWorkplace, XWorkplace newWorkplace) {
    var cycle = new ArrayList<XWorkplace>();

    var node = newWorkplace;
    while (node != null && node != oldWorkplace) {
      if (node.usePermit.getOwner() == null || node.usePermit.getOwner().getAwaitedWorkplace() == null) {
        break;
      }
      cycle.add(node);
      node = node.usePermit.getOwner().getAwaitedWorkplace();
    }
    if (node == oldWorkplace) {
      return cycle;
    }
    return null;
  }

  private void handleCycle(XWorker worker, XWorkplace oldWorkplace, XWorkplace newWorkplace,
                           List<XWorkplace> cycle) {
    assert cycle != null;
//    Log.info(worker, Log.RED + "handleCycle" + Log.RESET, "cycle =", oldWorkplace, "→", cycle,
//             "→", oldWorkplace);

    for (var i = 0; i < cycle.size(); ++i) {
      cycle.get(i).usePermit.fixNextOwner(i == 0 ? worker : cycle.get(i - 1).usePermit.getOwner());
    }
    oldWorkplace.usePermit.fixNextOwner(cycle.get(cycle.size() - 1).usePermit.getOwner());
  }

  @Override
  public void leave() {
    lock.lock();
    var worker = workers.get(getWorkerId());
    var oldWorkplace = worker.getCurrentWorkplace();

//    Log.info(worker, "leave.begin");

    oldWorkplace.releaseUsePermit(worker);
    worker.setCurrentWorkplace(null);

//    Log.info(worker, "leave.end");
    lock.unlock();
  }

  @Override
  public synchronized String toString() {
    return Log.CYAN + "[[workshop with " + workplaces.size() + " workplaces " + "and " + workers.size() + " workers]]" + Log.RESET;
  }

  public String getDetailedString() {
    var s = new StringJoiner("");
    s.add(this + "\n");
    workplaces.values().forEach(x -> s.add(x.getDetailedString()));
    workers.values().forEach(x -> s.add(x.getDetailedString()));
    s.add(metasemaphore.getDetailedString());
    return s.toString();
  }

  private long getWorkerId() {
    return Thread.currentThread().getId();
  }
}
