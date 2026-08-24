package com.storyengine.interaction.data;

import com.google.gson.annotations.SerializedName;
import com.storyengine.dialogue.DialogueConditionParser;
import net.minecraft.world.entity.player.Player;

/**
 * Действие интерактивного триггера (пункт меню взаимодействия).
 *
 * Поля максимально совместимы с DialogueResponse (см. Dialogue System v4 §5),
 * чтобы переиспользовать DialogueConditionParser (if) и DialogueActionExecutor
 * (command/completeTask/setFlag/storytell/give). Дополнительно добавлены:
 *  - dialogue: открыть диалог по id (вместо next-перехода);
 *  - sound: проиграть звук (ResourceLocation) игроку.
 */
public class TriggerAction {

    /** Текст пункта меню (подсвечивается как [>] 1. ...). */
    private String label = "";

    /** Условие доступности в строковом формате (см. Dialogue System §6). null/пусто = всегда доступно. */
    @SerializedName("if")
    private String condition = "";

    /** Открыть диалог по id (аналог /dialogue start). */
    private String dialogue = "";

    /** Выполнить команду сервера (с заменой @p на игрока). */
    private String command = "";

    /** Завершить подзадачу: "questId taskId". */
    private String completeTask = "";

    /** Проиграть звук игроку (ResourceLocation, напр. minecraft:block.end_portal_frame.fill). */
    private String sound = "";

    /** Установить флаг: "flag_name true/false". */
    private String setFlag = "";

    /** Показать реплику в Narrative HUD (как в DialogueResponse). */
    private Storytell storytell = null;

    /** Вложенный объект реплики Narrative HUD: {speaker, message:{text:...}}. */
    public static class Storytell {
        private String speaker = "";
        private com.google.gson.JsonObject message = null;

        public String getSpeaker() {
            return speaker;
        }

        public com.google.gson.JsonObject getMessage() {
            return message;
        }
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getDialogue() {
        return dialogue;
    }

    public void setDialogue(String dialogue) {
        this.dialogue = dialogue;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getCompleteTask() {
        return completeTask;
    }

    public void setCompleteTask(String completeTask) {
        this.completeTask = completeTask;
    }

    public String getSound() {
        return sound;
    }

    public void setSound(String sound) {
        this.sound = sound;
    }

    public String getSetFlag() {
        return setFlag;
    }

    public void setSetFlag(String setFlag) {
        this.setFlag = setFlag;
    }

    public Storytell getStorytell() {
        return storytell;
    }

    public void setStorytell(Storytell storytell) {
        this.storytell = storytell;
    }

    /** Есть ли хоть одно действие для исполнения (кроме чисто декоративного label). */
    public boolean hasEffect() {
        return (dialogue != null && !dialogue.isBlank())
                || (command != null && !command.isBlank())
                || (completeTask != null && !completeTask.isBlank())
                || (sound != null && !sound.isBlank())
                || (setFlag != null && !setFlag.isBlank())
                || storytell != null;
    }

    /** Доступен ли пункт прямо сейчас (проверка условия if). Косметика на клиенте; сервер валидирует повторно. */
    public boolean isAvailable(Player player) {
        if (condition == null || condition.isBlank()) {
            return true;
        }
        return DialogueConditionParser.parse(condition).map(c -> c.evaluate(player)).orElse(false);
    }
}
