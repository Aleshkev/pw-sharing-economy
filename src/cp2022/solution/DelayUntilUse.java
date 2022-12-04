package cp2022.solution;

import cp2022.base.Workplace;

public class DelayUntilUse extends Workplace {
  private final Workplace wrapped;
  private final Runnable delayedAction;

  public DelayUntilUse(Workplace wrapped, Runnable delayedAction) {
    super(wrapped.getId());
    this.wrapped = wrapped;
    assert delayedAction != null;
    this.delayedAction = delayedAction;
  }

  @Override
  public void use() {
    delayedAction.run();
    wrapped.use();
  }
}
