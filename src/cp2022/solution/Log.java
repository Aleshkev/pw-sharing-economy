package cp2022.solution;

class Log {
  private static final boolean deadlockDetectionEnabled = true;
  private static final boolean printingEnabled = true;

  private static final Object lock = new Object();
  private static Thread deadlockDetectionThread = null;
  private static long lastLog = System.currentTimeMillis();

  public static synchronized void info(Object source, String path, Object... s) {
    if (printingEnabled) {

      var string = new StringBuilder().append("thread(").append(Thread.currentThread().getId()).append(")/").append(path).append(":: ");
      for (var o : s)
        string.append(o).append(" ");
      string.append("@" + source);
      System.out.println(string);
    }

    if (deadlockDetectionEnabled) {
      synchronized (lock) {
        if (deadlockDetectionThread == null) {
          deadlockDetectionThread = new Thread(() -> {
            for (; ; ) {
              try {
                Thread.sleep(1000);
                if (System.currentTimeMillis() - lastLog > 4000) {
                  for (var entry : Thread.getAllStackTraces().entrySet()) {
                    var exception = new Exception("Thread " + entry.getKey().getId() + " \"" + entry.getKey().getName() + "\"");
                    exception.setStackTrace(entry.getValue());
                    exception.printStackTrace();
                  }
                  (new IllegalStateException("Nothing happened for a long time")).printStackTrace();
                  WorkshopImpl.mostRecentlyCreatedInstance.printState();
                  System.exit(1);
                }
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
            }
          });
          deadlockDetectionThread.setDaemon(true);
          deadlockDetectionThread.setName("LogDeadlockDetection");
          deadlockDetectionThread.start();
        }
        lastLog = System.currentTimeMillis();
      }
    }
  }
}
