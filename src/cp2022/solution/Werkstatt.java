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
  private final Map<WorkplaceId, Semaphore> waitForWorkplace;  // Immutable.
  private final Doorman doorman = new Doorman();  // A monitor.
  private final ThreadLocal<WorkplaceId> currentWorkplace = new ThreadLocal<>();  // Local.

  public Werkstatt(Collection<Workplace> workplaces) {
    this.workplaces = workplaces.stream().collect(Collectors.toUnmodifiableMap(Workplace::getId, x -> x));
    this.waitForWorkplace = workplaces.stream().collect(Collectors.toUnmodifiableMap(Workplace::getId, x -> new Semaphore(1, true)));
  }

  private RuntimeException getFavoriteException() {
    return new RuntimeException("panic: unexpected thread interruption");
  }

  @Override
  public Workplace enter(WorkplaceId workplaceId) {
    // Wait in a queue until everybody is ok with us entering the workshop.
    try {
      doorman.awaitSlot();
    } catch (InterruptedException e) {
      throw getFavoriteException();
    }

    acquireWorkplace(workplaceId);

    return workplaces.get(workplaceId);
  }

  @Override
  public Workplace switchTo(WorkplaceId workplaceId) {
    releaseCurrentWorkplace();

    // Ensure that fewer than 2N people can enter before we get to the workplace.
    doorman.limitSlots(2 * workplaces.size() - 1);
    acquireWorkplace(workplaceId);
    doorman.unlimitSlots();

    return workplaces.get(workplaceId);
  }

  @Override
  public void leave() {
    releaseCurrentWorkplace();
  }

  // Wait in the queue for the workplace to become available.
  private void acquireWorkplace(WorkplaceId workplaceId) {
    assert currentWorkplace.get() == null;

    var semaphore = waitForWorkplace.get(workplaceId);
    try {
      semaphore.acquire();
    } catch (InterruptedException e) {
      throw getFavoriteException();
    }

    currentWorkplace.set(workplaceId);
  }

  // Free the workplace.
  private void releaseCurrentWorkplace() {
    assert currentWorkplace.get() != null;

    var semaphore = waitForWorkplace.get(currentWorkplace.get());
    semaphore.release();
    currentWorkplace.set(null);
  }
}
