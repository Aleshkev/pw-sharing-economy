package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;
import cp2022.base.Workshop;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;


// Names of implementations of interfaces are in German.
public class Werkstatt implements Workshop {
  // All fields are async-friendly.
  private final Map<WorkplaceId, Workplace> workplaces;  // Immutable.
  private final Map<WorkplaceId, Semaphore> waitToMove;  // Immutable.
  private final Map<WorkplaceId, Semaphore> waitToWork;  // Immutable.
  private final Semaphore waitToEnterTheWorkshop;
  //  private final Doorman doorman = new Doorman();  // A monitor.
  private final ThreadLocal<WorkplaceId> currentWorkplace = new ThreadLocal<>();  // Local.

  public Werkstatt(Collection<Workplace> workplaces) {
    this.workplaces = workplaces.stream().collect(Collectors.toUnmodifiableMap(Workplace::getId, x -> x));
    this.waitToMove = workplaces.stream().collect(Collectors.toUnmodifiableMap(Workplace::getId, x -> new Semaphore(1, true)));
    this.waitToWork = workplaces.stream().collect(Collectors.toUnmodifiableMap(Workplace::getId, x -> new Semaphore(1, true)));
    this.waitToEnterTheWorkshop = new Semaphore(2 * workplaces.size() - 1);
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
    catchInterrupts(() -> waitToEnterTheWorkshop.acquire());

    catchInterrupts(() -> waitToMove.get(newWorkplace).acquire());
    catchInterrupts(() -> waitToWork.get(newWorkplace).acquire());
    currentWorkplace.set(newWorkplace);

    return workplaces.get(newWorkplace);
  }

  @Override
  public Workplace switchTo(WorkplaceId newWorkplace) {
    WorkplaceId oldWorkplace = currentWorkplace.get();

    if(oldWorkplace == newWorkplace) return workplaces.get(newWorkplace);

    waitToMove.get(oldWorkplace).release();

    catchInterrupts(() -> waitToMove.get(newWorkplace).acquire());
    currentWorkplace.set(newWorkplace);

    Runnable delayedActions = () -> {
      waitToWork.get(oldWorkplace).release();
      catchInterrupts(() -> waitToWork.get(newWorkplace).acquire());
    };

    return new WorkplaceWrapper(workplaces.get(newWorkplace), delayedActions, null);
  }

  @Override
  public void leave() {
    WorkplaceId oldWorkplace = currentWorkplace.get();
    waitToMove.get(oldWorkplace).release();
    waitToWork.get(oldWorkplace).release();
    currentWorkplace.set(null);

    waitToEnterTheWorkshop.release();
  }

  private interface InterruptableAction {
    void run() throws InterruptedException;
  }
}
