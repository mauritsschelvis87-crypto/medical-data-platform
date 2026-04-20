package com.mauri.backend.entity;

import com.mauri.backend.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "medication_catalog",
        indexes = {
                @Index(name = "idx_medication_catalog_code", columnList = "code")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_medication_catalog_code", columnNames = "code")
        }
)
public class MedicationCatalog extends BaseEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "dutch_name", nullable = false, length = 255)
    private String dutchName;

    @Column(name = "latin_name", length = 255)
    private String latinName;

    @Column(name = "default_dosage", length = 100)
    private String defaultDosage;

    @Column(name = "advice", columnDefinition = "TEXT")
    private String advice;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDutchName() {
        return dutchName;
    }

    public void setDutchName(String dutchName) {
        this.dutchName = dutchName;
    }

    public String getLatinName() {
        return latinName;
    }

    public void setLatinName(String latinName) {
        this.latinName = latinName;
    }

    public String getDefaultDosage() {
        return defaultDosage;
    }

    public void setDefaultDosage(String defaultDosage) {
        this.defaultDosage = defaultDosage;
    }

    public String getAdvice() {
        return advice;
    }

    public void setAdvice(String advice) {
        this.advice = advice;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}