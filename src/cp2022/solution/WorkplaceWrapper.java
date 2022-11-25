package cp2022.solution;

import cp2022.base.Workplace;

public class WorkplaceWrapper extends Workplace {
  private final Workplace wrapped;
  private final Runnable beforeUse, afterUse;

  public WorkplaceWrapper(Workplace wrapped, Runnable beforeUse, Runnable afterUse) {
    super(wrapped.getId());
    this.wrapped = wrapped;
    this.beforeUse = beforeUse;
    this.afterUse = afterUse;
  }

  @Override
  public void use() {
    if (beforeUse != null) beforeUse.run();
    wrapped.use();
    if (afterUse != null) afterUse.run();
  }
}
