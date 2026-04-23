package com.mauri.backend.dto.timeline;

import java.time.LocalDateTime;
import java.util.UUID;

public class TimelineEventDto {

    private UUID id;
    private String eventType;

    private String title;
    private String description;

    private LocalDateTime eventTimestamp;

    public UUID getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEventTimestamp(LocalDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }
}
