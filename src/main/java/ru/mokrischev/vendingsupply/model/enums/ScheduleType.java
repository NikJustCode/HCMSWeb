package ru.mokrischev.vendingsupply.model.enums;

public enum ScheduleType {
    WEEKLY_DAYS("По дням недели"),
    SHIFT_PATTERN("Сменный график");

    private final String displayName;

    ScheduleType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
