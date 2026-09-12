package com.storyengine.trigger;

import java.util.Locale;
import java.util.Optional;

/**
 * Тип цели триггера (ТЗ §6.1, поле {@code target_type}): блок, сущность,
 * квест или диалог. Определяет, как интерпретируется {@code target_tag}
 * и как фильтруется событие.
 *
 * Для MVP (ТЗ §5.2) блок адресуется через связанный маркер-сущность с тегом
 * (отдельной адресации позиций нет), поэтому фактически цель всегда сущность
 * с тегом; тип нужен для семантики и будущего расширения ({@code target_id}).
 */
public enum TriggerTargetType {

    BLOCK,
    ENTITY,
    QUEST,
    DIALOGUE;

    /** Находит тип по строке из JSON (без учёта регистра). */
    public static Optional<TriggerTargetType> byId(String id) {
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