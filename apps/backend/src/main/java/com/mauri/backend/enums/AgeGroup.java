package com.mauri.backend.enums;

public enum AgeGroup {
    UNKNOWN,
    BABY,
    TODDLER,
    CHILD,
    ADOLESCENT,
    ADULT,
    OLDER_ADULT;

    public boolean isPediatric() {
        return this == BABY || this == TODDLER || this == CHILD || this == ADOLESCENT;
    }

    public boolean isAdultOrOlder() {
        return this == ADULT || this == OLDER_ADULT;
    }
}
