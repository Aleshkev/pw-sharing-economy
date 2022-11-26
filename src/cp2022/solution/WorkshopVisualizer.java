package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;
import cp2022.base.Workshop;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class WorkshopVisualizer implements Workshop {

  private final Workshop wrapped;
  int nextTimestamp = 1;

  TreeMap<Long, String> threadNames = new TreeMap<>();
  Map<Long, TreeMap<Integer, ThreadState>> events = new HashMap<>();

  public WorkshopVisualizer(Workshop wrapped) {
    this.wrapped = wrapped;
  }

  private synchronized void startState(ThreadState state) {
    if (!threadNames.containsKey(getThreadId())) {
      threadNames.put(getThreadId(), Thread.currentThread().getName());
      events.put(getThreadId(), new TreeMap<>());
      events.get(getThreadId()).put(0, new ThreadState(ThreadStateType.Unactive, null));
    }
    events.get(getThreadId()).put(nextTimestamp, state);
    ++nextTimestamp;

    if (nextTimestamp % 1 == 0) getVisualization();
  }

  private long getThreadId() {
    return Thread.currentThread().getId();
  }

  @Override
  public Workplace enter(WorkplaceId wid) {
    startState(new ThreadState(ThreadStateType.BeganEnter, wid.toString()));
    var workplace = wrapped.enter(wid);
    startState(new ThreadState(ThreadStateType.FinishedEnter, wid.toString()));
    return new WorkplaceWrapper(workplace);
  }

  @Override
  public Workplace switchTo(WorkplaceId wid) {
    startState(new ThreadState(ThreadStateType.BeganSwitchTo, wid.toString()));
    var workplace = wrapped.switchTo(wid);
    startState(new ThreadState(ThreadStateType.FinishedSwitchTo, wid.toString()));
    return new WorkplaceWrapper(workplace);
  }

  @Override
  public void leave() {
    startState(new ThreadState(ThreadStateType.BeganLeave, null));
    wrapped.leave();
    startState(new ThreadState(ThreadStateType.FinishedLeave, null));
  }

  public String getVisualization() {
    var html = new StringBuilder();

    html.append("<meta charset=\"UTF-8\"><link rel='stylesheet' href='style.css'>");
    html.append("<table><tr><th></th>");
    for (var i = 0; i < nextTimestamp; ++i)
      html.append("<th class='small squish'>").append(i).append("</th>");
    html.append("</tr>\n");

    for (var threadId : threadNames.keySet()) {
      var threadName = threadNames.get(threadId);

      html.append("<tr><th>").append(threadName).append("<span class='small'>id: ").append(threadId).append("</span></th>");

      for (var event : events.get(threadId).entrySet()) {
        var start = event.getKey();

        var higher = events.get(threadId).higherKey(start);
        var end = higher == null ? nextTimestamp : higher - 1;

        html.append("<td colspan=")
                .append(end - start + 1)
                .append(" class='squish ")
                .append(event.getValue().type.name())
                .append("' title='")
                .append(event.getValue().type.name())
                .append(event.getValue().info == null ? "" : " → " + event.getValue().info)
                .append("'>")
                .append(event.getValue().type.name())
                .append(event.getValue().info == null ? "" : " → " + event.getValue().info)
                .append("</td>");
      }

      html.append("</tr>\n");

    }

    try {
//      var templateFile = new File("/home/aleshkev/pw-sharing-economy/preview.html");
//      var template = Files.readString(templateFile.toPath());
//      var rendered = template.replace("{{content}}", html.toString());

      var outputFile = new File("/home/aleshkev/pw-sharing-economy/visualization.html");
      if (outputFile.exists())
        outputFile.delete();

      Files.write(outputFile.toPath(), List.of(html.toString()), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }


    return html.toString();
  }

  private enum ThreadStateType {
    Unactive,
    BeganEnter,
    FinishedEnter,
    BeganUse,
    FinishedUse,
    BeganSwitchTo,
    FinishedSwitchTo,
    BeganLeave,
    FinishedLeave
  }

  private class ThreadState {
    public ThreadStateType type;
    public String info;

    public ThreadState(ThreadStateType type, String info) {
      this.type = type;
      this.info = (info == null ? "" : info) + " #" + Thread.currentThread().getId();
    }
  }

  private class WorkplaceWrapper extends Workplace {
    Workplace wrapped;

    public WorkplaceWrapper(Workplace wrapped) {
      super(wrapped.getId());
      this.wrapped = wrapped;
    }

    @Override
    public void use() {
      startState(new ThreadState(ThreadStateType.BeganUse, null));
      wrapped.use();
      startState(new ThreadState(ThreadStateType.FinishedUse, null));
    }
  }
}
