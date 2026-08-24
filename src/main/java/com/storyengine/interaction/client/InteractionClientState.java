package com.storyengine.interaction.client;

import com.storyengine.StoryEngineMod;
import com.storyengine.interaction.data.InteractionTrigger;
import com.storyengine.interaction.server.TriggerManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Клиентское состояние Interaction System: кэш триггеров (для рейкаста/HUD),
 * текущая цель взгляда (ActiveTarget) и выбранный индекс пункта меню.
 *
 * Триггеры поступают двумя путями:
 *  - локально из config/story_engine/triggers/ (loadFromConfig) — работает в
 *    одиночном мире и как резерв, пока нет сетевой синхронизации;
 *  - по сети от сервера (setTriggers) — авторитетный источник в мультиплеере
 *    (см. InteractionNetworking, этап 5).
 */
public final class InteractionClientState {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<ResourceLocation, Map<BlockPos, InteractionTrigger>> triggers = new LinkedHashMap<>();

    /** Текущая цель взгляда (null = нет). */
    private static InteractionTrigger activeTarget = null;
    private static BlockPos activePos = null;

    /** Выбранный индекс пункта меню (0-based). */
    private static int selectedIndex = 0;

    private InteractionClientState() {
    }

    /** Загружает триггеры из локального config (клиентская сторона). */
    public static void loadFromConfig() {
        TriggerManager tmp = new TriggerManager();
        tmp.loadAll();
        setTriggers(tmp.getAll());
        LOGGER.info("[StoryEngine] Клиент загрузил {} триггеров из config.", countTriggers());
    }

    /** Заменяет кэш триггеров (используется сетевой синхронизацией). */
    public static void setTriggers(Collection<InteractionTrigger> all) {
        triggers.clear();
        for (InteractionTrigger t : all) {
            Map<BlockPos, InteractionTrigger> inDim = triggers.computeIfAbsent(t.getDimensionRL(), k -> new LinkedHashMap<>());
            for (BlockPos pose : t.getBlockPoses()) {
                inDim.put(pose, t);
            }
        }
    }

    public static int countTriggers() {
        int n = 0;
        for (Map<BlockPos, InteractionTrigger> m : triggers.values()) {
            n += m.size();
        }
        return n;
    }

    /** Поиск триггера по позиции в измерении. */
    public static InteractionTrigger findAt(ResourceLocation dimension, BlockPos pos) {
        Map<BlockPos, InteractionTrigger> inDim = triggers.get(dimension);
        if (inDim == null) {
            return null;
        }
        return inDim.get(pos);
    }

    public static InteractionTrigger getActiveTarget() {
        return activeTarget;
    }

    public static BlockPos getActivePos() {
        return activePos;
    }

    public static boolean hasActiveTarget() {
        return activeTarget != null;
    }

    /** Установить цель взгляда (со сбросом выбора при смене цели). */
    public static void setActiveTarget(InteractionTrigger trigger, BlockPos pos) {
        if (activeTarget != trigger) {
            selectedIndex = 0;
        }
        activeTarget = trigger;
        activePos = pos;
    }

    public static void clearActiveTarget() {
        activeTarget = null;
        activePos = null;
    }

    public static int getSelectedIndex() {
        return selectedIndex;
    }

    public static void setSelectedIndex(int index) {
        selectedIndex = index;
    }

    /** Сдвинуть выбор (скроллом). Останавливается на границах списка. */
    public static void scrollSelection(int direction, int actionCount) {
        if (actionCount <= 0) {
            return;
        }
        int next = selectedIndex + direction;
        if (next < 0) {
            next = 0;
        }
        if (next >= actionCount) {
            next = actionCount - 1;
        }
        selectedIndex = next;
    }
}
