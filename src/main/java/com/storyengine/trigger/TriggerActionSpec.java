package com.storyengine.trigger;

import com.google.gson.JsonObject;

/**
 * Одно действие триггера в том виде, в котором оно записано в JSON
 * (ТЗ §6.1, {@code actions}): ключ-тип + параметры. Фактический исполнитель
 * будет резолвиться по {@code type} из реестра действий при выполнении
 * (запланировано на этап 3-5).
 *
 * @param type   тип действия, например {@code "story_tell"} или {@code "give_item"}
 * @param params параметры действия (все поля кроме {@code type})
 */
public record TriggerActionSpec(String type, JsonObject params) {
}