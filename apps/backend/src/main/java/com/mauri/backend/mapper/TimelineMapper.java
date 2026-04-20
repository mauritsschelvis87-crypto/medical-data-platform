package com.mauri.backend.mapper;

import com.mauri.backend.dto.timeline.TimelineEventDto;
import com.mauri.backend.entity.TimelineEvent;
import org.springframework.stereotype.Component;

@Component
public class TimelineMapper {

    public TimelineEventDto toDto(TimelineEvent timelineEvent) {
        if (timelineEvent == null) {
            return null;
        }

        TimelineEventDto dto = new TimelineEventDto();
        dto.setId(timelineEvent.getId());
        dto.setEventType(timelineEvent.getEventType() != null ? timelineEvent.getEventType().name() : null);
        dto.setTitle(timelineEvent.getTitle());
        dto.setDescription(timelineEvent.getDescription());
        dto.setEventTimestamp(timelineEvent.getEventTimestamp());

        return dto;
    }
}