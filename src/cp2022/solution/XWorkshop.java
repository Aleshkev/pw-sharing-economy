package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;
import cp2022.base.Workshop;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


public class XWorkshop implements Workshop {
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

  private void catchInterrupts(InterruptableAction f) {
    try {
      f.run();
    } catch (InterruptedException e) {
      throw new RuntimeException("panic: unexpected thread interruption");
    }
  }

  @Override
  public Workplace enter(WorkplaceId newWorkplaceId) {
    lock.lock();

    var worker = workers.computeIfAbsent(getWorkerId(), XWorker::new);
    var newWorkplace = workplaces.get(newWorkplaceId);
    Log.info(worker, "enter.begin", newWorkplaceId);

    metasemaphore.startWaiting(2 * workplaces.size() - 1);

    Log.info(worker, "enter.acquireEntryPermit", newWorkplace);
    catchInterrupts(() -> metasemaphore.acquirePermit());

    Log.info(worker, "enter.acquireMovePermit", newWorkplace);
    catchInterrupts(() -> newWorkplace.acquireMovePermit(worker));
    worker.setCurrentWorkplace(newWorkplace);

    Log.info(worker, "enter.end", newWorkplace);
    return new DelayUntilUse(newWorkplace.getWorkplace(), () -> {
      Log.info(worker, "enter-delayed.begin", newWorkplace);
      metasemaphore.stopWaiting();

      Log.info(worker, "enter-delayed.acquireUsePermit", newWorkplace);
      catchInterrupts(() -> newWorkplace.acquireUsePermit(worker));

      Log.info(worker, "enter-delayed.end", newWorkplace);
      lock.unlock();
    });
  }

  @Override
  public Workplace switchTo(WorkplaceId newWorkplaceId) {
    lock.lock();

    Log.info(workers, "switchTo.begin", newWorkplaceId);
    var worker = workers.get(getWorkerId());
    var oldWorkplace = worker.getCurrentWorkplace();
    var newWorkplace = workplaces.get(newWorkplaceId);

    if (oldWorkplace == newWorkplace) {
      lock.unlock();
      return newWorkplace.getWorkplace();
    }

    oldWorkplace.releaseMovePermit(worker);

    metasemaphore.startWaiting(2 * workplaces.size() - 1);

    Log.info(worker, "enter.begin.acquireMovePermit", newWorkplace);
    catchInterrupts(() -> newWorkplace.acquireMovePermit(worker));
    worker.setCurrentWorkplace(newWorkplace);

    Log.info(worker, "switchTo.end", newWorkplace);
    return new DelayUntilUse(newWorkplace.getWorkplace(), () -> {
      Log.info(worker, "switchTo-delayed.begin");

      metasemaphore.stopWaiting();
      oldWorkplace.releaseUsePermit(worker);

      Log.info(worker, "switchTo-delayed.acquireWorkPermit");
      catchInterrupts(() -> newWorkplace.acquireUsePermit(worker));

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

    oldWorkplace.releaseMovePermit(worker);
    oldWorkplace.releaseUsePermit(worker);
    worker.setCurrentWorkplace(null);

    Log.info(worker, "leave.end");
    lock.unlock();
  }

  @Override
  public synchronized String toString() {
    return "Workshop[]";
  }

  public void trace() {
    System.err.print(Log.RED);
    for (var w : workplaces.values()) {
      w.trace();
    }
    for (var w : workers.values()) {
      w.trace();
    }
    System.err.print(Log.RESET);
  }

  private long getWorkerId() {
    return Thread.currentThread().getId();
  }

  private interface InterruptableAction {
    void run() throws InterruptedException;
  }
}
