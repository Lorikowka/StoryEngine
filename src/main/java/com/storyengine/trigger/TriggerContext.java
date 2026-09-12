package com.storyengine.trigger;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;

/**
 * Данные конкретного события (ТЗ §12): игрок, уровень, измерение, цель
 * (блок или сущность), позиция и исходное Forge-событие.
 *
 * Контекст создаётся на сервере при наступлении события и пробрасывается
 * через фильтр цели, условия, доступность и действия. Для MVP доступны:
 * {@link #player()}, {@link #level()}, цель (сущность либо блок), источник
 * в виде Forge-события для отмены ванильного действия.
 */
public final class TriggerContext {

    private final ServerPlayer player;
    private final ServerLevel level;
    private final TriggerEvent event;

    @Nullable
    private final Entity targetEntity;

    @Nullable
    private final BlockPos targetPos;

    /** Forge-событие, вызвавшее триггер; используется для обслуживания {@code cancel_vanilla}. */
    @Nullable
    private final Object sourceEvent;

    private TriggerContext(Builder builder) {
        this.player = builder.player;
        this.level = builder.level;
        this.event = builder.event;
        this.targetEntity = builder.targetEntity;
        this.targetPos = builder.targetPos;
        this.sourceEvent = builder.sourceEvent;
    }

    public static Builder builder(ServerPlayer player, ServerLevel level, TriggerEvent event) {
        return new Builder(player, level, event);
    }

    public ServerPlayer player() {
        return player;
    }

    public ServerLevel level() {
        return level;
    }

    /** Измерение события (совпадает с {@code level.dimension()}). */
    public Level getWorld() {
        return level;
    }

    public TriggerEvent event() {
        return event;
    }

    @Nullable
    public Entity targetEntity() {
        return targetEntity;
    }

    @Nullable
    public BlockPos targetPos() {
        return targetPos;
    }

    @Nullable
    public BlockState targetState() {
        return targetPos == null ? null : level.getBlockState(targetPos);
    }

    @Nullable
    public Object sourceEvent() {
        return sourceEvent;
    }

    /**
     * Отменяет ванильное поведение исходного события, если событие
     * (а) есть в контексте и (б) отменяемо. Используется для
     * {@code cancel_vanilla: true} (ТЗ §6.1, §10.2).
     */
    public void cancelVanillaIfRequested(TriggerDefinition trigger) {
        if (!trigger.cancelVanilla() || sourceEvent == null) {
            return;
        }
        if (sourceEvent instanceof Event event) {
            if (event.isCancelable() && !event.isCanceled()) {
                event.setCanceled(true);
            }
        }
    }

    /** Данные для построения контекста; обязательные поля задаются в builder. */
    public static final class Builder {
        private final ServerPlayer player;
        private final ServerLevel level;
        private final TriggerEvent event;

        @Nullable
        private Entity targetEntity;

        @Nullable
        private BlockPos targetPos;

        @Nullable
        private Object sourceEvent;

        private Builder(ServerPlayer player, ServerLevel level, TriggerEvent event) {
            this.player = player;
            this.level = level;
            this.event = event;
        }

        public Builder targetEntity(Entity entity) {
            this.targetEntity = entity;
            return this;
        }

        public Builder targetPos(BlockPos pos) {
            this.targetPos = pos;
            return this;
        }

        public Builder sourceEvent(Object event) {
            this.sourceEvent = event;
            return this;
        }

        public TriggerContext build() {
            return new TriggerContext(this);
        }
    }
}