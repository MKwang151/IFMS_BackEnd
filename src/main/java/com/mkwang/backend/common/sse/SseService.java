package com.mkwang.backend.common.sse;

import com.mkwang.backend.common.dto.SseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseService {

    private static final long SSE_TIMEOUT_MS = 0L;
    private static final String DEFAULT_EVENT_NAME = "notification";

    private final Map<Long, Map<String, SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    public SseEmitter connect(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        String emitterId = UUID.randomUUID().toString();

        userEmitters
                .computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>())
                .put(emitterId, emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitterId));
        emitter.onTimeout(() -> removeEmitter(userId, emitterId));
        emitter.onError(ignored -> removeEmitter(userId, emitterId));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE connected"));
        } catch (IOException e) {
            removeEmitter(userId, emitterId);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public void sendToUser(Long userId, SseEvent sseEvent) {
        Map<String, SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        String eventName = resolveEventName(sseEvent);
        Object data = sseEvent == null ? null : sseEvent.getData();

        for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
            String emitterId = entry.getKey();
            SseEmitter emitter = entry.getValue();
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException | IllegalStateException ex) {
                log.debug("[SseService] Remove broken emitter userId={} emitterId={}", userId, emitterId);
                removeEmitter(userId, emitterId);
            }
        }
    }

    private String resolveEventName(SseEvent sseEvent) {
        if (sseEvent == null || !StringUtils.hasText(sseEvent.getEvent())) {
            return DEFAULT_EVENT_NAME;
        }
        return sseEvent.getEvent();
    }

    private void removeEmitter(Long userId, String emitterId) {
        Map<String, SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitterId);
        if (emitters.isEmpty()) {
            userEmitters.remove(userId);
        }
    }
}


