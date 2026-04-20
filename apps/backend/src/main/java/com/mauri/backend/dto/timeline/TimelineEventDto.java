package com.mauri.backend.dto.timeline;

import java.time.LocalDateTime;

public class TimelineEventDto {

    private Long id;
    private String eventType;

    private String title;
    private String description;

    private LocalDateTime eventTimestamp;

    public Long getId() {
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

    public void setId(Long id) {
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