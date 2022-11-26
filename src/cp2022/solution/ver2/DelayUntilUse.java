package cp2022.solution.ver2;

import cp2022.base.Workplace;

public class DelayUntilUse extends Workplace {
  private final Workplace wrapped;
  private final Runnable delayedAction;
  private boolean usedUp = false;

  public DelayUntilUse(Workplace wrapped, Runnable delayedAction) {
    super(wrapped.getId());
    this.wrapped = wrapped;
    assert delayedAction != null;
    this.delayedAction = delayedAction;
  }

  @Override
  public void use() {
    // TODO: According to the problem statement, the use() method should be called only once. When tests are corrected, remove the check
    if (!usedUp)
      delayedAction.run();
    usedUp = true;

    wrapped.use();
  }
}
