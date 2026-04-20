package com.mauri.backend.dto.medication;

public class MedicationCatalogDto {

    private Long id;
    private String code;
    private String dutchName;
    private String latinName;
    private String defaultDosage;
    private String advice;

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDutchName() {
        return dutchName;
    }

    public String getLatinName() {
        return latinName;
    }

    public String getDefaultDosage() {
        return defaultDosage;
    }

    public String getAdvice() {
        return advice;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setDutchName(String dutchName) {
        this.dutchName = dutchName;
    }

    public void setLatinName(String latinName) {
        this.latinName = latinName;
    }

    public void setDefaultDosage(String defaultDosage) {
        this.defaultDosage = defaultDosage;
    }

    public void setAdvice(String advice) {
        this.advice = advice;
    }
}