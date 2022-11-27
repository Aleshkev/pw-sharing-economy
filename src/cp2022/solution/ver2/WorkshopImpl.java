package cp2022.solution.ver2;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;
import cp2022.base.Workshop;
import cp2022.solution.XWorker;
import cp2022.solution.XWorkplace;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class WorkshopImpl implements Workshop {

  private final ReentrantLock lock = new ReentrantLock(true);
  private final Map<WorkplaceId, XWorkplace> workplaces;  // Immutable.
  private final Map<Long, XWorker> workers = new HashMap<>();

  public WorkshopImpl(Collection<Workplace> workplaces) {
    this.workplaces = workplaces.stream().collect(Collectors.toUnmodifiableMap(Workplace::getId, x -> new XWorkplace(this, x)));
  }

  public ReentrantLock getLock() {
    return lock;
  }

  @Override
  public Workplace enter(WorkplaceId newWorkplaceId) {
    lock.lock();
    var worker = workers.computeIfAbsent(getWorkerId(), XWorker::new);
    var newWorkplace = workplaces.get(newWorkplaceId);

    catchInterrupts(() -> newWorkplace.acquireMovePermit(worker));
    worker.setCurrentWorkplace(newWorkplace);

    Runnable delayedActions = () -> {
      catchInterrupts(() -> newWorkplace.acquireUsePermit(worker));
      lock.unlock();
    };
    return new DelayUntilUse(newWorkplace.getWorkplace(), delayedActions);
  }

  @Override
  public Workplace switchTo(WorkplaceId newWorkplaceId) {
    lock.lock();
    var worker = workers.get(getWorkerId());
    var oldWorkplace = worker.getCurrentWorkplace();
    XWorkplace newWorkplace = workplaces.get(newWorkplaceId);

    if (oldWorkplace == newWorkplace)
      return newWorkplace.getWorkplace();

    catchInterrupts(() -> newWorkplace.acquireMovePermit(worker));
    worker.setCurrentWorkplace(newWorkplace);

    Runnable delayedActions = () -> {
      oldWorkplace.releaseUsePermit(worker);

      catchInterrupts(() -> newWorkplace.acquireUsePermit(worker));

      lock.unlock();
    };
    return new DelayUntilUse(newWorkplace.getWorkplace(), delayedActions);
  }

  @Override
  public void leave() {
    lock.lock();

    var worker = workers.get(getWorkerId());
    var oldWorkplace = worker.getCurrentWorkplace();

    worker.setCurrentWorkplace(null);
    oldWorkplace.releaseMovePermit(worker);
    oldWorkplace.releaseUsePermit(worker);

    lock.unlock();
  }

  private long getWorkerId() {
    return Thread.currentThread().getId();
  }

  private void catchInterrupts(InterruptableAction f) {
    try {
      f.run();
    } catch (InterruptedException e) {
      throw new RuntimeException("panic: unexpected thread interruption");
    }
  }

  private interface InterruptableAction {
    void run() throws InterruptedException;
  }
}
