package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;
import cp2022.base.Workshop;
import cp2022.solution.ver2.Worker;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;


public class WorkshopImpl implements Workshop {
  static WorkshopImpl mostRecentlyCreatedInstance;

  private final Map<WorkplaceId, Workplace> workplaces;  // Immutable.
  private final Map<WorkplaceId, BinarySemaphore<Worker>> waitToMove;  // Immutable.
  private final Map<WorkplaceId, BinarySemaphore<Worker>> waitToWork;  // Immutable.
  private final Metasemaphore metasemaphore = new Metasemaphore();  // A monitor.
  private final ThreadLocal<WorkplaceId> currentWorkplace = new ThreadLocal<>();  // Local.

  private final Semaphore limitPopulation;

  public WorkshopImpl(Collection<Workplace> workplaces) {
    this.workplaces = workplaces.stream().collect(Collectors.toUnmodifiableMap(Workplace::getId, x -> x));
    this.waitToMove = workplaces.stream().collect(Collectors.toUnmodifiableMap(Workplace::getId, x -> new BinarySemaphore<>("move-" + x)));
    this.waitToWork = workplaces.stream().collect(Collectors.toUnmodifiableMap(Workplace::getId, x -> new BinarySemaphore<>("work-" + x)));
    limitPopulation = new Semaphore(2 * workplaces.size() - 1, true);
    mostRecentlyCreatedInstance = this;
  }

  private void catchInterrupts(InterruptableAction f) {
    try {
      f.run();
    } catch (InterruptedException e) {
      throw new RuntimeException("panic: unexpected thread interruption");
    }
  }

  @Override
  public Workplace enter(WorkplaceId newWorkplace) {
    Log.info(this, "enter/begin", newWorkplace);

    metasemaphore.startWaiting(2 * workplaces.size() - 1);

    catchInterrupts(() -> limitPopulation.acquire());
    catchInterrupts(() -> metasemaphore.acquirePermit());
    Log.info(this, "enter/begin/acquiredEntryPermit", newWorkplace);

    catchInterrupts(() -> waitToMove.get(newWorkplace).acquire());
    Log.info(this, "enter/begin/acquiredMovePermit", newWorkplace);


    currentWorkplace.set(newWorkplace);

    Runnable delayedActions = () -> {
      Log.info(this, "enter/delayedEnd", newWorkplace);
      metasemaphore.stopWaiting();

      catchInterrupts(() -> waitToWork.get(newWorkplace).acquire());
      Log.info(this, "enter/begin/acquiredWorkPermit", newWorkplace);
    };

    Log.info(this, "enter/end", newWorkplace);
    return new DelayUntilUse(workplaces.get(newWorkplace), delayedActions);
  }

  @Override
  public Workplace switchTo(WorkplaceId newWorkplace) {
    Log.info(this, "switchTo/begin", newWorkplace);
    WorkplaceId oldWorkplace = currentWorkplace.get();

    if (oldWorkplace == newWorkplace) return workplaces.get(newWorkplace);

    waitToMove.get(oldWorkplace).release();

    metasemaphore.startWaiting(2 * workplaces.size() - 1);

    catchInterrupts(() -> waitToMove.get(newWorkplace).acquire());
    currentWorkplace.set(newWorkplace);
    Log.info(this, "enter/begin/acquiredMovePermit", newWorkplace);

    Runnable delayedActions = () -> {
      Log.info(this, "switchTo/delayedEnd", newWorkplace);

      metasemaphore.stopWaiting();
      waitToWork.get(oldWorkplace).release();

      catchInterrupts(() -> waitToWork.get(newWorkplace).acquire());
      Log.info(this, "enter/begin/acquiredWorkPermit", newWorkplace);
    };

    Log.info(this, "switchTo/end", newWorkplace);
    return new DelayUntilUse(workplaces.get(newWorkplace), delayedActions);
  }

  @Override
  public void leave() {
    Log.info(this, "leave/begin");

    WorkplaceId oldWorkplace = currentWorkplace.get();
    limitPopulation.release();
    waitToMove.get(oldWorkplace).release();
    waitToWork.get(oldWorkplace).release();
    currentWorkplace.set(null);

    Log.info(this, "leave/end");
  }

  @Override
  public synchronized String toString() {
    return "Workshop<Thread<" + Thread.currentThread().getId() + "currentWorkplace=" + currentWorkplace.get() + ">>";
  }

  public void printState() {
    for (var s : waitToMove.values()) {
      Log.info(this, "print", s);
    }
    for (var s : waitToWork.values()) {
      Log.info(this, "print", s);
    }
  }

  private interface InterruptableAction {
    void run() throws InterruptedException;
  }
}
