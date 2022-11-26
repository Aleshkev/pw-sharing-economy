package cp2022.solution;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BinarySemaphore<T> {

  private final ReentrantLock lock = new ReentrantLock();
  private final Condition isEmpty = lock.newCondition();
  private final List<T> waiting = new ArrayList<>();
  private final String name;
  private T owner = null;

  public BinarySemaphore(String name) {
    this.name = name;
  }

  public void acquire(T acquirer) throws InterruptedException {
    lock.lock();
    waiting.add(acquirer);
    while (owner != null)
      isEmpty.await();
    waiting.remove(acquirer);
    owner = acquirer;
    lock.unlock();
  }

  public void release() {
    lock.lock();
    owner = null;
    isEmpty.signal();
    lock.unlock();
  }

  public String toString() {
    lock.lock();
    try {
      return "BinarySemaphore[\"" + name + "\", acquiredBy=" + owner + ", waiting=" + waiting + "]";
    } finally {
      lock.unlock();
    }
  }
}
