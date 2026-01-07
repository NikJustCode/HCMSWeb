package ru.mokrischev.vendingsupply.model.enums;

public enum OperationType {
    INCOME("Приход"),
    OUTCOME("Списание"),
    INCOMING_ORDER("Поступление (Заказ)"),
    MANUAL_INCOME("Ручной приход"),
    MANUAL_OUTCOME("Ручное списание"),
    MACHINE_SERVICE("Обслуживание автомата");

    private final String displayName;

    OperationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
