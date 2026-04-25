package com.mauri.backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClinicalDatasetReplacementService {

    private final JdbcTemplate jdbcTemplate;

    public ClinicalDatasetReplacementService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void replacePatientScopedData() {
        jdbcTemplate.update("delete from predictions");
        jdbcTemplate.update("delete from timeline_events");
        jdbcTemplate.update("delete from patient_medications");
        jdbcTemplate.update("update consult_notes set current_version_id = null");
        jdbcTemplate.update("delete from consult_note_versions");
        jdbcTemplate.update("delete from consult_notes");
        jdbcTemplate.update("delete from vital_signs");
        jdbcTemplate.update("delete from patient_address");
        jdbcTemplate.update("delete from patient");
    }
}
