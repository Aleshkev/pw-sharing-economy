package cp2022.solution;

interface InterruptableAction {
  static void run(InterruptableAction f) {
    try {
      f.run();
    } catch (InterruptedException e) {
      throw new RuntimeException("panic: unexpected thread interruption");
    }
  }

  void run() throws InterruptedException;
}
