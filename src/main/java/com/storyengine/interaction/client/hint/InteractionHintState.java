package com.storyengine.interaction.client.hint;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Клиентское состояние general-подсказок взаимодействия:
 * цель взгляда и список близких интерактиблов для мировых маркеров.
 *
 * Заполняется в {@link com.storyengine.interaction.client.hint.InteractionHintScanner},
 * читается HUD/маркерами и F-fallback ввода.
 */
public final class InteractionHintState {

    /** Ключ перевода действия (story_engine.action.*). */
    private static String aimedAction = null;

    /** Центр цели взгляда (мир), для маркера/подсветки. */
    private static Vec3 aimedPos = null;

    /** Центры близких интерактиблов (мир) - кольца-маркеры. */
    private static List<Vec3> nearbyTargets = new ArrayList<>();

    private InteractionHintState() {
    }

    public static String getAimedAction() {
        return aimedAction;
    }

    public static Vec3 getAimedPos() {
        return aimedPos;
    }

    public static boolean hasAimed() {
        return aimedAction != null;
    }

    public static List<Vec3> getNearbyTargets() {
        return nearbyTargets;
    }

    public static void setAimed(String action, Vec3 pos) {
        aimedAction = action;
        aimedPos = pos;
    }

    public static void clearAimed() {
        aimedAction = null;
        aimedPos = null;
    }

    public static void setNearbyTargets(List<Vec3> targets) {
        nearbyTargets = targets != null ? targets : new ArrayList<>();
    }

    public static void clear() {
        clearAimed();
        nearbyTargets = new ArrayList<>();
    }
}