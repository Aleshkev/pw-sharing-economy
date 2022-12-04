package cp2022.solution;

import java.util.ArrayList;
import java.util.List;

// A custom implementation of a binary semaphore. Assumes the threads work in  monitor-like fashion.
// Has a special operation to fix who will get the permit next.
public class BinarySemaphore {
  private final List<XWorker> waiting = new ArrayList<>();
  private final String name;
  private XWorker owner = null;
  private XWorker nextOwner = null;

  public BinarySemaphore(String name) {
    this.name = name;
  }

  public XWorker getOwner() {
    return owner;
  }

  public void acquire(XWorker worker) throws InterruptedException {
    waiting.add(worker);
    while (owner != null || (nextOwner != null && worker != nextOwner)) {
      worker.wakeup.await();
    }
    waiting.remove(worker);
    owner = worker;
    nextOwner = null;
  }

  public void release(XWorker oldOwner) {
//    if (oldOwner != owner) {
//      throw new IllegalStateException(this + ": Permit owned by " + owner + ", not " + oldOwner);
//    }

    owner = null;
    if (!waiting.isEmpty()) {
      if (nextOwner != null) {
        nextOwner.wakeup.signal();
      } else {
        waiting.get(0).wakeup.signal();
      }
    }
  }

  public void fixNextOwner(XWorker nextOwner) {
//    if (this.nextOwner != null) {
//      throw new IllegalStateException(this + ": Next owner has already been " + "fixed to " + nextOwner);
//    }

    this.nextOwner = nextOwner;
//    Log.info(this, "fixNextOwner", nextOwner);
  }

  public String toString() {
    return Log.BLUE + "[" + name + " (→" + nextOwner + Log.BLUE + ") " + owner + Log.BLUE + " :: "
            + waiting + Log.BLUE + "]" + Log.RESET;
  }
}
