package com.eduspark.agent.task;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class TaskStreamService {

  private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

  public SseEmitter subscribe(String taskId) {
    SseEmitter emitter = new SseEmitter(0L);
    emitters.computeIfAbsent(taskId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
    emitter.onCompletion(() -> remove(taskId, emitter));
    emitter.onTimeout(() -> remove(taskId, emitter));
    emitter.onError(ignored -> remove(taskId, emitter));
    return emitter;
  }

  public void sendLog(String taskId, EduTaskLog log) {
    send(taskId, "task.log.appended", log);
  }

  public void sendStatus(String taskId, EduTask task) {
    String eventName =
        task.getStatus() == TaskStatus.COMPLETED
            ? "task.completed"
            : task.getStatus() == TaskStatus.FAILED ? "task.failed" : "task.status.changed";
    send(taskId, eventName, task);
  }

  public void complete(String taskId) {
    for (SseEmitter emitter : emitters.getOrDefault(taskId, List.of())) {
      emitter.complete();
    }
    emitters.remove(taskId);
  }

  private void send(String taskId, String eventName, Object payload) {
    for (SseEmitter emitter : emitters.getOrDefault(taskId, List.of())) {
      try {
        emitter.send(SseEmitter.event().name(eventName).data(payload, MediaType.APPLICATION_JSON));
      } catch (IOException ex) {
        remove(taskId, emitter);
      }
    }
  }

  private void remove(String taskId, SseEmitter emitter) {
    List<SseEmitter> taskEmitters = emitters.get(taskId);
    if (taskEmitters == null) {
      return;
    }
    taskEmitters.remove(emitter);
    if (taskEmitters.isEmpty()) {
      emitters.remove(taskId);
    }
  }
}
