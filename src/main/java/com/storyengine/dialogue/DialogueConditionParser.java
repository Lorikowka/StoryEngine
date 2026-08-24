package com.storyengine.dialogue;

import com.storyengine.quest.QuestStatus;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Optional;

/**
 * Преобразует строковое условие из JSON ("quest:id:active") в объект
 * DialogueCondition. Формат см. спецификацию §6.
 *
 * Разделитель ":" используется между префиксом и телом. Для составных ID
 * (itemId+count, questId+taskId) внутри тела используется пробел.
 */
public final class DialogueConditionParser {

    private static final Logger LOGGER = LogUtils.getLogger();

    private DialogueConditionParser() {
    }

    public static Optional<DialogueCondition> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String s = raw.trim();

        if (s.startsWith("not:")) {
            return parse(s.substring(4)).map(NotCondition::new);
        }

        int idx = s.indexOf(':');
        if (idx < 0) {
            LOGGER.warn("[StoryEngine] Некорректное условие диалога (нет префикса): '{}'", raw);
            return Optional.empty();
        }

        String prefix = s.substring(0, idx);
        String rest = s.substring(idx + 1);

        switch (prefix) {
            case "quest": {
                String[] parts = rest.split(":", 2);
                if (parts.length != 2) {
                    return Optional.empty();
                }
                return Optional.of(new QuestStatusCondition(parts[0], parts[1]));
            }
            case "item": {
                int space = rest.lastIndexOf(' ');
                if (space < 0) {
                    return Optional.of(new ItemCondition(rest, 1));
                }
                String itemId = rest.substring(0, space);
                int count;
                try {
                    count = Integer.parseInt(rest.substring(space + 1).trim());
                } catch (NumberFormatException e) {
                    LOGGER.warn("[StoryEngine] Некорректное кол-во предметов в условии: '{}'", raw);
                    return Optional.empty();
                }
                return Optional.of(new ItemCondition(itemId, count));
            }
            case "task": {
                String[] parts = rest.split(" ", 2);
                if (parts.length != 2) {
                    return Optional.empty();
                }
                return Optional.of(new TaskCondition(parts[0], parts[1]));
            }
            case "flag": {
                if (rest.isBlank()) {
                    return Optional.empty();
                }
                return Optional.of(new FlagCondition(rest));
            }
            default:
                LOGGER.warn("[StoryEngine] Неизвестный префикс условия диалога: '{}'", prefix);
                return Optional.empty();
        }
    }

    /** Вспомогательный метод для маппинга статуса (используется тестами). */
    public static QuestStatus statusOf(String name) {
        try {
            return QuestStatus.valueOf(name.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
