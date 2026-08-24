package com.storyengine.dialogue;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import net.minecraft.world.entity.player.Player;

/**
 * Вариант ответа игрока. Вместо массива actions[] с type - плоские
 * опциональные поля, любые комбинации разрешены (см. спецификацию §5).
 */
public class DialogueResponse {

    private String text = "";

    /** Условие доступности в строковом формате (см. §6). null/пусто = всегда доступно. */
    @SerializedName("if")
    private String condition = "";

    /** Перейти к узлу (имя файла без .json). */
    private String next = "";

    /** true - закрыть диалог (игнорирует next). */
    private Boolean close = null;

    /** Выполнить команду (с заменой @p на UUID игрока). */
    private String command = "";

    /** Начать квест. */
    private String startQuest = "";

    /** Завершить подзадачу: "questId taskId". */
    private String completeTask = "";

    /** Показать Narrative HUD (см. ниже). */
    private Storytell storytell = null;

    /** Выдать предмет: "id count" или {id, count, nbt}. */
    private Object give = null;

    /** Установить флаг: "flag_name true/false". */
    private String setFlag = "";

    /** Выдать опыт. */
    private Integer xp = null;

    public static class Storytell {
        private String speaker = "";
        private String icon = "";
        private JsonObject message = null;

        public String getSpeaker() {
            return speaker;
        }

        public String getIcon() {
            return icon;
        }

        public JsonObject getMessage() {
            return message;
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }

    public Boolean getClose() {
        return close;
    }

    public void setClose(Boolean close) {
        this.close = close;
    }

    public boolean shouldClose() {
        return close != null && close;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getStartQuest() {
        return startQuest;
    }

    public void setStartQuest(String startQuest) {
        this.startQuest = startQuest;
    }

    public String getCompleteTask() {
        return completeTask;
    }

    public void setCompleteTask(String completeTask) {
        this.completeTask = completeTask;
    }

    public Storytell getStorytell() {
        return storytell;
    }

    public void setStorytell(Storytell storytell) {
        this.storytell = storytell;
    }

    public Object getGive() {
        return give;
    }

    public void setGive(Object give) {
        this.give = give;
    }

    public String getSetFlag() {
        return setFlag;
    }

    public void setSetFlag(String setFlag) {
        this.setFlag = setFlag;
    }

    public Integer getXp() {
        return xp;
    }

    public void setXp(Integer xp) {
        this.xp = xp;
    }

    /** Доступен ли ответ игроку прямо сейчас (проверка условия if). */
    public boolean isAvailable(Player player) {
        if (condition == null || condition.isBlank()) {
            return true;
        }
        return DialogueConditionParser.parse(condition).map(c -> c.evaluate(player)).orElse(false);
    }
}
