package com.storyengine.trigger;

import java.util.Locale;
import java.util.Optional;

/**
 * Событие, на которое может срабатывать триггер (ТЗ §3.1).
 *
 * Категория «взаимодействие» ({@code isInteraction()}) важна для правил
 * конфликтов (ТЗ §9.3): для физического клика выполняется ОДИН наиболее
 * приоритетный триггер, а для событий-состояний (квест/диалог) — все
 * подходящие в порядке {@code priority}.
 */
public enum TriggerEvent {

    /** Игрок взаимодействовал с блоком (правый клик/cancel_vanilla). */
    INTERACT_BLOCK(true),

    /** Игрок взаимодействовал с сущностью. */
    INTERACT_ENTITY(true),

    /** Изменился статус или прогресс квеста. */
    QUEST_CHANGE(false),

    /** Диалог завершился на сервере. */
    DIALOGUE_FINISH(false);

    private final boolean interaction;

    TriggerEvent(boolean interaction) {
        this.interaction = interaction;
    }

    /** {@code true}, если событие является физическим взаимодействием игрока. */
    public boolean isInteraction() {
        return interaction;
    }

    /**
     * Находит событие по строке из JSON (без учёта регистра), например
     * {@code "interact_block"}. Пустая/неверная строка → {@code Optional.empty()}.
     */
    public static Optional<TriggerEvent> byId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(id.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}