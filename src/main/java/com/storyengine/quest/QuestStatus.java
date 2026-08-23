package com.storyengine.quest;

/**
 * Статус квеста у конкретного игрока.
 */
public enum QuestStatus {
    NOT_STARTED,
    ACTIVE,
    COMPLETED,
    FAILED;

    /** Название статуса для GUI (журнал квестов). */
    public String displayName() {
        switch (this) {
            case COMPLETED:
                return "Выполнен";
            case FAILED:
                return "Провален";
            case ACTIVE:
                return "Активен";
            default:
                return "Не начат";
        }
    }

    /** Цвет текста статуса для GUI. */
    public int displayColor() {
        switch (this) {
            case COMPLETED:
                return 0x55FF55;
            case FAILED:
                return 0xFF5555;
            case ACTIVE:
                return 0xFFFFFF;
            default:
                return 0xAAAAAA;
        }
    }
}
