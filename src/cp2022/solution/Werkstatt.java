package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;
import cp2022.base.Workshop;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

public class Werkstatt implements Workshop {

  final Map<WorkplaceId, Workplace> workplaces;  // Immutable.
  final Map<WorkplaceId, Semaphore> semaphoreOfWorkplace;  // Immutable.
  final Map<Long, WorkplaceId> currentWorkplaceOfThread;  // Concurrent.

  public Werkstatt(Collection<Workplace> workplaces) {
    this.workplaces = workplaces.stream().collect(Collectors.toUnmodifiableMap(Workplace::getId, x -> x));
    this.semaphoreOfWorkplace = workplaces.stream().collect(Collectors.toUnmodifiableMap(Workplace::getId, x -> new Semaphore(1, true)));
    this.currentWorkplaceOfThread = new ConcurrentHashMap<>();
  }

  @Override
  public Workplace enter(WorkplaceId workplaceId) {
    acquireWorkplace(workplaceId);
    return workplaces.get(workplaceId);
  }

  @Override
  public Workplace switchTo(WorkplaceId workplaceId) {
    releaseCurrentWorkplace();
    acquireWorkplace(workplaceId);
    return workplaces.get(workplaceId);
  }

  @Override
  public void leave() {
    releaseCurrentWorkplace();
  }

  private void acquireWorkplace(WorkplaceId workplaceId) {
    var semaphore = semaphoreOfWorkplace.get(workplaceId);
    try {
      semaphore.acquire();
    } catch (InterruptedException e) {
      throw new RuntimeException("panic: unexpected thread interruption");
    }
    currentWorkplaceOfThread.put(getThreadId(), workplaceId);
  }

  private void releaseCurrentWorkplace() {
    var semaphore = semaphoreOfWorkplace.get(currentWorkplaceOfThread.get(getThreadId()));
    semaphore.release();
    currentWorkplaceOfThread.remove(getThreadId());
  }

  private long getThreadId() {
    return Thread.currentThread().getId();
  }
}
