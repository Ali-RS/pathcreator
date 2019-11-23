package com.jayfella.pathcreator.event;

import java.util.List;

public interface CreatorEvent {

    void eventTriggered();

    static void triggerEvents(List<CreatorEvent> events) {
        events.forEach(CreatorEvent::eventTriggered);
    }

}
