package cp2022.solution;

class Log {
  public static final String RESET = "\u001B[0m";
  public static final String BLACK = "\u001B[30m";
  public static final String RED = "\u001B[31m";
  public static final String GREEN = "\u001B[32m";
  public static final String YELLOW = "\u001B[33m";
  public static final String BLUE = "\u001B[34m";
  public static final String PURPLE = "\u001B[35m";
  public static final String CYAN = "\u001B[36m";
  public static final String WHITE = "\u001B[37m";

  public static final boolean deadlockDetectionEnabled = true;
  public static final boolean printingEnabled = false;

  private static final Object lock = new Object();
  private static Thread deadlockDetectionThread = null;
  private static long lastLog = System.currentTimeMillis();

  public static synchronized void info(Object source, String path, Object... s) {
    if (printingEnabled) {

      var start = GREEN + "(" + Thread.currentThread().getId() + ")." + RESET + path + ": ";
      var string = new StringBuilder().append(start).append(" ".repeat(Math.max(0, 36 - start.length())));
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
                if (System.currentTimeMillis() - lastLog > 3000) {
                  System.err.print(RED);
                  for (var entry : Thread.getAllStackTraces().entrySet()) {
                    var exception = new Exception("Thread " + entry.getKey().getId() + " \"" + entry.getKey().getName() + "\"");
                    exception.setStackTrace(entry.getValue());
                    exception.printStackTrace();
                  }
                  XWorkshop.mostRecentlyCreatedInstance.trace();
//                  throw new IllegalStateException("nothing happens ...");
                  System.err.print(RED);
                  (new IllegalStateException("Nothing happened for a long time")).printStackTrace();
                  System.err.print(RESET);
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
