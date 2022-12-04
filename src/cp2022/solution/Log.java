package cp2022.solution;

class Log {
  public static final boolean isEnabled = false;

  public static final String RESET = "\u001B[0m";
  public static final String RED = "\u001B[31m";
  public static final String GREEN = "\u001B[32m";
  public static final String YELLOW = "\u001B[33m";
  public static final String BLUE = "\u001B[34m";
  public static final String PURPLE = "\u001B[35m";
  public static final String CYAN = "\u001B[36m";

//  public static synchronized void info(Object source, String path, Object... s) {
//    DeadlockDetection.somethingHappened();
//
//    if (!isEnabled) {
//      return;
//    }
//    var start = GREEN + "(" + Thread.currentThread().getId() + ")." + RESET + path + ": ";
//    var string = new StringBuilder().append(start).append(" ".repeat(Math.max(0,
//                                                                              40 - start.length())));
//    for (var o : s) {
//      string.append(o).append(" ");
//    }
//    string.append(" in ").append(source);
//    System.out.println(string);
//  }
}
