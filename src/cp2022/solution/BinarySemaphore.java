package cp2022.solution;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;

public class BinarySemaphore<T> {

  private final Condition isEmpty;
  private final List<T> waiting = new ArrayList<>();
  private final String name;
  private XWorkshop workshop;
  private T owner = null;

  public BinarySemaphore(XWorkshop workshop, String name) {
    this.workshop = workshop;
    this.name = name;
    isEmpty = workshop.getLock().newCondition();
  }

  public void acquire(T newOwner) throws InterruptedException {
    waiting.add(newOwner);
    while (owner != null) {
//      System.out.println("thread " + newOwner + " starts waiting on " + this);
//      if (isEmpty.await(1, TimeUnit.SECONDS))
      workshop.getLock().unlock();
      Thread.sleep(1);
      workshop.getLock().lock();
//      System.out.println("thread " + newOwner + " notified by " + this);
    }
    waiting.remove(newOwner);
    owner = newOwner;
  }

  public void release(T oldOwner) {
    if (oldOwner != owner)
      throw new IllegalStateException("Permit for \"" + name + "\" owned by " + owner + ", not " + oldOwner);
    owner = null;
//    System.out.println("notifiy all by " + this);
    isEmpty.signalAll();
  }

  public void fixNextOwner(T nextOwner) {

  }

  public String toString() {
    return Log.BLUE + "BinSem[\"" + name + "\", " +
            owner + Log.BLUE + "... " + waiting + Log.BLUE + "]" + Log.RESET;
  }
}
