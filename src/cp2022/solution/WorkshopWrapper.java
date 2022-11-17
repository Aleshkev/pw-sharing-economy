package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;
import cp2022.base.Workshop;

public class WorkshopWrapper implements Workshop {
    Workshop real;

    public WorkshopWrapper(Workshop real) {
        this.real = real;
    }

    @Override
    public Workplace enter(WorkplaceId wid) {
        return real.enter(wid);
    }

    @Override
    public Workplace switchTo(WorkplaceId wid) {
        return real.switchTo(wid);
    }

    @Override
    public void leave() {
        real.leave();
    }
}
