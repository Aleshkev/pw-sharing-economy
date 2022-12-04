package cp2022.solution;

class DeadlockDetection {
  private static final boolean isEnabled = Log.areAssertionsEnabled();
  private static final boolean doPrintAllStackTraces = false;
  private static long lastEvent = System.currentTimeMillis();

  private static Thread thread;

  public static synchronized void startIfEnabled() {
    if (!Log.areAssertionsEnabled()) {
      throw new IllegalStateException("Assertions must be enabled to enable deadlock detection.");
    }

    if (!isEnabled || thread != null) {
      return;
    }
    thread = new Thread(() -> {
      while (true) {
        InterruptableAction.run(() -> Thread.sleep(1000));
        if (System.currentTimeMillis() - lastEvent > 5000) {
          deadlock();
        }
      }
    });
    thread.setDaemon(true);
    thread.setName("DeadlockDetection");
    thread.start();
  }

  private static synchronized void deadlock() {
    assert isEnabled;
    System.out.print(XWorkshop.mostRecentlyCreatedInstance.getDetailedString());

    if (doPrintAllStackTraces) {
      for (var entry : Thread.getAllStackTraces().entrySet()) {
        var exception =
                new Exception("Thread " + entry.getKey().getId() + " \"" + entry.getKey().getName() + "\"");
        exception.setStackTrace(entry.getValue());
        exception.printStackTrace();
      }
    }

    System.err.print(Log.RED);
    (new IllegalStateException("Nothing happened for a long time")).printStackTrace();
    System.exit(1);
  }

  public static synchronized void somethingHappened() {
    if (!Log.areAssertionsEnabled()) {
      throw new IllegalStateException("Assertions must be enabled to enable deadlock detection.");
    }
    if (!isEnabled) {
      return;
    }
    lastEvent = System.currentTimeMillis();
  }
}
