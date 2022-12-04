package cp2022.solution;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.stream.Collectors;

// Doesn't let too many people in. The number of yet available slots can be limited in each thread,
// and the limit can be lifted in the same thread.
class Metasemaphore {
  private final Condition anyAvailable;
  private final HashMap<Integer, Integer> permits = new HashMap<>();
  HashMap<XWorker, Integer> lastLimit = new HashMap<>();
  private int time = 1;

  public Metasemaphore(XWorkshop workshop) {
    anyAvailable = workshop.getLock().newCondition();
  }

  // At most n permits will be given out before stopWaiting() is called by the thread.
  public void startWaiting(XWorker worker, int n) {
    if (n <= 0)
      throw new IllegalArgumentException("Invalid limit on permits");
    if (lastLimit.get(worker) != null)
      throw new IllegalStateException("The worker was already waiting");

    Log.info(this, "startWaiting/begin");

    lastLimit.put(worker, time);
    permits.put(time, n);
    ++time;

    Log.info(this, "startWaiting/end");

  }

  public void acquirePermit(XWorker worker) throws InterruptedException {
    if (lastLimit.get(worker) == null)
      throw new IllegalStateException("A thread must be waiting to acquire a permit");

    Log.info(this, "acquirePermit/begin");

    ++time;

    while (!isSlotAvailable(lastLimit.get(worker)))
      anyAvailable.await();

    permits.replaceAll((a, b) -> a < lastLimit.get(worker) ? b - 1 : b);

    Log.info(this, "acquirePermit/end");

  }

  public void stopWaiting(XWorker worker) {
    if (!lastLimit.containsKey(worker))
      throw new IllegalStateException("The worker was not waiting");

    Log.info(this, "stopWaiting/begin");

    permits.remove(lastLimit.get(worker));
    lastLimit.remove(worker);

    anyAvailable.signal();

    Log.info(this, "stopWaiting/end");

  }

  private boolean isSlotAvailable(int time) {
    return permits.entrySet().stream().filter(entry -> entry.getKey() < time).allMatch(entry -> entry.getValue() > 0);
  }

  @Override
  public String toString() {
    return Log.PURPLE + "[metasemaphore, " + Log.RESET + getWaiting() + Log.PURPLE + "]" + Log.RESET;
  }

  private String getWaiting() {
    var waiting = lastLimit.entrySet().stream().map(entry -> Map.entry(entry.getKey(), permits.get(entry.getValue()))).collect(Collectors.toSet());
    return waiting.toString().replace("=", ": ");
  }

  public String getTrace() {
    return Log.PURPLE + "metasemaphore" + "\n" +
            "  limits = " + getWaiting() + "\n";
  }
}
