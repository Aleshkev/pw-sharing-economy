package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;
import cp2022.base.Workshop;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

public class Werkstatt implements Workshop {

  // All fields are async-friendly.
  private final Map<WorkplaceId, Workplace> workplaces;  // Immutable.
  private final Map<WorkplaceId, Semaphore> waitForWorkplace;  // Immutable.
  private final Doorman waitBeforeEntering = new Doorman();  // A monitor.
  private final ThreadLocal<WorkplaceId> currentWorkplace = new ThreadLocal<>();

  public Werkstatt(Collection<Workplace> workplaces) {
    this.workplaces = workplaces.stream().collect(Collectors.toUnmodifiableMap(Workplace::getId, x -> x));
    this.waitForWorkplace = workplaces.stream().collect(Collectors.toUnmodifiableMap(Workplace::getId, x -> new Semaphore(1, true)));
  }

  private RuntimeException getFavoriteException() {
    return new RuntimeException("panic: unexpected thread interruption");
  }

  @Override
  public Workplace enter(WorkplaceId workplaceId) {
    try {
      waitBeforeEntering.awaitSlot();
    } catch (InterruptedException e) {
      throw getFavoriteException();
    }

    acquireWorkplace(workplaceId);

    return workplaces.get(workplaceId);
  }

  @Override
  public Workplace switchTo(WorkplaceId workplaceId) {

    releaseCurrentWorkplace();

    waitBeforeEntering.limitSlots(2 * workplaces.size() - 1);
    acquireWorkplace(workplaceId);
    waitBeforeEntering.unlimitSlots();

    return workplaces.get(workplaceId);
  }

  @Override
  public void leave() {
    releaseCurrentWorkplace();
  }

  private void acquireWorkplace(WorkplaceId workplaceId) {
    assert currentWorkplace.get() == null;

    // Wait in the queue.
    var semaphore = waitForWorkplace.get(workplaceId);
    try {
      semaphore.acquire();
    } catch (InterruptedException e) {
      throw getFavoriteException();
    }

    currentWorkplace.set(workplaceId);
  }

  private void releaseCurrentWorkplace() {
    assert currentWorkplace.get() != null;

    // Free the workplace.
    var semaphore = waitForWorkplace.get(currentWorkplace.get());
    semaphore.release();
    currentWorkplace.set(null);
  }
}
