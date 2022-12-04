package cp2022.solution;

import java.util.*;
import java.util.stream.Collectors;

// Doesn't let too many people in. The number of yet available slots can be
// limited in each thread,  and the limit can be lifted in the same thread.
class Metasemaphore {
  private final Map<Integer, Integer> limits = new TreeMap<>();
  private final List<XWorker> waitingToAcquire = new ArrayList<>();
  HashMap<XWorker, Integer> timeOfLimit = new HashMap<>();
  private int time = 1;

  public Metasemaphore() {
  }

  // At most n permits will be given out before stopWaiting() is called by
  // the thread.
  public void startWaiting(XWorker worker, int n) {
    assert n > 0 : "Invalid limit";
    assert timeOfLimit.get(worker) == null : "The worker was already waiting";

    assert Log.info(this, "startWaiting.begin");

    timeOfLimit.put(worker, time);
    limits.put(time, n);
    ++time;

    assert Log.info(this, "startWaiting.end");
  }

  public void acquirePermit(XWorker worker) throws InterruptedException {
    assert timeOfLimit.get(worker) != null : "A thread must be waiting to acquire a permit";

    assert Log.info(this, "acquirePermit.begin");

    ++time;
    waitingToAcquire.add(worker);

    while (!canAcquirePermit(timeOfLimit.get(worker))) {
      worker.wakeup.await();
    }

    waitingToAcquire.remove(worker);
    limits.replaceAll((a, b) -> a < timeOfLimit.get(worker) ? b - 1 : b);

    assert Log.info(this, "acquirePermit.end");
  }

  public void stopWaiting(XWorker worker) {
    assert timeOfLimit.containsKey(worker) : "The worker was not waiting";

    assert Log.info(this, "stopWaiting.begin");

    limits.remove(timeOfLimit.get(worker));
    timeOfLimit.remove(worker);

    // We wake up the thread that was waiting the longest.
    if (!waitingToAcquire.isEmpty()) {
      waitingToAcquire.get(0).wakeup.signal();
    }

    assert Log.info(this, "stopWaiting.end");
  }

  private boolean canAcquirePermit(int whenStartedWaiting) {
    return limits.entrySet().stream().filter(entry -> entry.getKey() < whenStartedWaiting).allMatch(entry -> entry.getValue() > 0);
  }

  private String getWaiting() {
    var waiting = timeOfLimit.entrySet().stream().map(entry -> Map.entry(entry.getKey(),
                                                                         limits.get(entry.getValue()))).collect(Collectors.toSet());
    return waiting.toString().replace("=", ": ");
  }

  @Override
  public String toString() {
    return (Log.PURPLE + "[metasemaphore, " + Log.RESET) + (getWaiting() + Log.PURPLE + "]" + Log.RESET);
  }

  public String getDetailedString() {
    return (Log.PURPLE + "metasemaphore" + "\n") + (Log.PURPLE + "  limits = " + getWaiting() +
            "\n") + (Log.PURPLE + "  waiting to acquire = " + waitingToAcquire + "\n");
  }
}
