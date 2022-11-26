package cp2022.solution;

import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// Doesn't let too many people in. The number of yet available slots can be limited in each thread,
// and the limit can be lifted in the same thread.
class Metasemaphore {
  private final ReentrantLock lock = new ReentrantLock(true);
  private final Condition anyAvailable = lock.newCondition();
  private final HashMap<Integer, Integer> permits = new HashMap<>();
  ThreadLocal<Integer> lastLimit = new ThreadLocal<>();
  boolean verbose = false;
  private int time = 1;

  public Metasemaphore() {
  }

  private Long getThreadId() {
    return Thread.currentThread().getId();
  }

  // At most n permits will be given out before stopWaiting() is called by the thread.
  public void startWaiting(int n) {
    if (n <= 0)
      throw new IllegalArgumentException("Invalid limit on permits");
    if (lastLimit.get() != null)
      throw new IllegalStateException("The thread was already waiting");

    lock.lock();
    try {
      Log.info(this, "startWaiting/begin");

      lastLimit.set(time);
      permits.put(time, n);
      ++time;

      Log.info(this, "startWaiting/end");
    } finally {
      lock.unlock();
    }
  }

  public void acquirePermit() throws InterruptedException {
    if (lastLimit.get() == null)
      throw new IllegalStateException("A thread must be waiting to acquire a permit");

    lock.lock();
    try {
      Log.info(this, "acquirePermit/begin");

      ++time;

      if (verbose)
        System.out.println(".await_slot start " + this);
      while (!isSlotAvailable(lastLimit.get()))
        anyAvailable.await();

      permits.replaceAll((a, b) -> a < lastLimit.get() ? b - 1 : b);

      Log.info(this, "acquirePermit/end");
    } finally {
      lock.unlock();
    }
  }

  public void stopWaiting() {
    if (lastLimit.get() == null)
      throw new IllegalStateException("The thread was not waiting");

    lock.lock();
    try {
      Log.info(this, "stopWaiting/begin");

      permits.remove(lastLimit.get());
      lastLimit.set(null);

      anyAvailable.signalAll();

      Log.info(this, "stopWaiting/end");
    } finally {
      lock.unlock();
    }
  }

  private boolean isSlotAvailable(int time) {
    assert lock.isLocked();
    return permits.entrySet().stream().filter(entry -> entry.getKey() < time).allMatch(entry -> entry.getValue() > 0);
  }

  @Override
  public String toString() {
    lock.lock();
    try {
      return "Metasemaphore{...}";
//      return "Doorman{" +
//              "permits=" + permits + ", thread=" + getThreadId() + ", " + lastLimit.get() + "}";
    } finally {
      lock.unlock();
    }
  }
}
