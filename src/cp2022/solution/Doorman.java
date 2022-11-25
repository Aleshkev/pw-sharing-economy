package cp2022.solution;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// Doesn't let too many people in. The number of yet available slots can be limited in each thread,
// and the limit can be lifted in the same thread.
class Doorman {
  private final ReentrantLock lock = new ReentrantLock(true);
  private final Condition slotAvailable = lock.newCondition();

  // availability[x] > 0 implies that slots with index >= x are not available.
  private final Map<Integer, Integer> availabilities = new HashMap<>();

  private final ThreadLocal<Boolean> hasLimitBeenSet = ThreadLocal.withInitial(() -> false);
  private final ThreadLocal<Integer> whereToDecrease = new ThreadLocal<>();
  private int nextSlot = 0;

  public Doorman() {
  }

  // At most n slots unassigned at this moment will be assigned.
  public void limitSlots(int n) {
    if (n <= 0)
      throw new IllegalArgumentException("Invalid number of slots to reserve");
    if (hasLimitBeenSet.get())
      throw new IllegalStateException("There already is a limit set by this thread");

    lock.lock();
    try {
      var whereToChange = nextSlot + n;
      availabilities.merge(whereToChange, 1, (a, b) -> a + 1);
      whereToDecrease.set(whereToChange);
      hasLimitBeenSet.set(true);
    } finally {
      lock.unlock();
    }
  }

  // Wait for a slot.
  public void awaitSlot() throws InterruptedException {
    lock.lock();
    try {
      while (availabilities.getOrDefault(nextSlot, 0) > 0)
        slotAvailable.await();
      ++nextSlot;
    } finally {
      lock.unlock();
    }
  }

  // Remove the limit placed by the current thread.
  public void unlimitSlots() {
    if (!hasLimitBeenSet.get())
      throw new IllegalStateException("There is no limit set by the current thread");
    hasLimitBeenSet.set(false);
    assert whereToDecrease.get() != null;

    lock.lock();
    try {
      availabilities.merge(whereToDecrease.get(), -1, (a, b) -> (a == 1 ? null : b));
      whereToDecrease.set(null);

      slotAvailable.signal();
    } finally {
      lock.unlock();
    }
  }
}
