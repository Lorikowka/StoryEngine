package com.storyengine.trigger;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Данные одного JSON-триггера (ТЗ §6.1). Неизменяемый после загрузки.
 * Поля по умолчанию: {@code cancel_vanilla=false}, {@code one_time=false},
 * {@code priority=0}, {@code enabled=true}.
 */
public final class TriggerDefinition {

    private final String id;
    private final TriggerEvent event;

    /** Тег цели (scoreboard tag сущности под крестовиной, ТЗ §5.2). */
    @Nullable
    private final String targetTag;

    /** Семантический тип цели; может отсутствовать для событий-состояний. */
    @Nullable
    private final TriggerTargetType targetType;

    private final boolean cancelVanilla;

    /** Текст подсказки для HUD (этап 6), {@code null}, если не задан. */
    @Nullable
    private final String hintLabel;

    /** Условия в едином диалекте Dialogue System (ТЗ §7). */
    private final List<String> conditions;

    private final List<TriggerActionSpec> actions;

    private final boolean oneTime;

    private final int priority;

    private final boolean enabled;

    private TriggerDefinition(Builder builder) {
        this.id = builder.id;
        this.event = builder.event;
        this.targetTag = builder.targetTag;
        this.targetType = builder.targetType;
        this.cancelVanilla = builder.cancelVanilla;
        this.hintLabel = builder.hintLabel;
        this.conditions = List.copyOf(builder.conditions);
        this.actions = List.copyOf(builder.actions);
        this.oneTime = builder.oneTime;
        this.priority = builder.priority;
        this.enabled = builder.enabled;
    }

    public static Builder builder(String id, TriggerEvent event) {
        return new Builder(id, event);
    }

    public String id() {
        return id;
    }

    public TriggerEvent event() {
        return event;
    }

    @Nullable
    public String targetTag() {
        return targetTag;
    }

    @Nullable
    public TriggerTargetType targetType() {
        return targetType;
    }

    public boolean cancelVanilla() {
        return cancelVanilla;
    }

    @Nullable
    public String hintLabel() {
        return hintLabel;
    }

    public List<String> conditions() {
        return conditions;
    }

    public List<TriggerActionSpec> actions() {
        return actions;
    }

    public boolean oneTime() {
        return oneTime;
    }

    public int priority() {
        return priority;
    }

    public boolean enabled() {
        return enabled;
    }

    public int comparePriority(TriggerDefinition other) {
        return Integer.compare(this.priority, other.priority);
    }

    public static final class Builder {
        private final String id;
        private final TriggerEvent event;

        @Nullable
        private String targetTag;

        @Nullable
        private TriggerTargetType targetType;

        private boolean cancelVanilla = false;

        @Nullable
        private String hintLabel;

        private List<String> conditions = List.of();

        private List<TriggerActionSpec> actions = List.of();

        private boolean oneTime = false;

        private int priority = 0;

        private boolean enabled = true;

        private Builder(String id, TriggerEvent event) {
            this.id = id;
            this.event = event;
        }

        public Builder targetTag(@Nullable String tag) {
            this.targetTag = tag;
            return this;
        }

        public Builder targetType(@Nullable TriggerTargetType type) {
            this.targetType = type;
            return this;
        }

        public Builder cancelVanilla(boolean cancel) {
            this.cancelVanilla = cancel;
            return this;
        }

        public Builder hintLabel(@Nullable String label) {
            this.hintLabel = label;
            return this;
        }

        public Builder conditions(List<String> conditions) {
            this.conditions = conditions == null ? List.of() : conditions;
            return this;
        }

        public Builder actions(List<TriggerActionSpec> actions) {
            this.actions = actions == null ? List.of() : actions;
            return this;
        }

        public Builder oneTime(boolean oneTime) {
            this.oneTime = oneTime;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public TriggerDefinition build() {
            return new TriggerDefinition(this);
        }
    }
}