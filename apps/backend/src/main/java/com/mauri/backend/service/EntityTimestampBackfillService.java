package com.mauri.backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class EntityTimestampBackfillService {

    private final JdbcTemplate jdbcTemplate;

    public EntityTimestampBackfillService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void backfillConsultNoteTimestamps(UUID id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        backfill("consult_notes", id, createdAt, updatedAt);
    }

    public void backfillConsultNoteVersionTimestamps(UUID id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        backfill("consult_note_versions", id, createdAt, updatedAt);
    }

    public void backfillPatientMedicationTimestamps(UUID id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        backfill("patient_medications", id, createdAt, updatedAt);
    }

    private void backfill(String tableName, UUID id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        jdbcTemplate.update(
                "update " + tableName + " set created_at = ?, updated_at = ? where id = ?",
                Timestamp.valueOf(createdAt),
                Timestamp.valueOf(updatedAt),
                id
        );
    }
}
