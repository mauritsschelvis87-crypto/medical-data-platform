package com.mauri.backend.dto.medication;

import java.util.UUID;

public class MedicationCatalogSelectionDto {

    private UUID id;
    private String name;
    private String defaultDosage;
    private String defaultFrequency;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefaultDosage() {
        return defaultDosage;
    }

    public void setDefaultDosage(String defaultDosage) {
        this.defaultDosage = defaultDosage;
    }

    public String getDefaultFrequency() {
        return defaultFrequency;
    }

    public void setDefaultFrequency(String defaultFrequency) {
        this.defaultFrequency = defaultFrequency;
    }
}
