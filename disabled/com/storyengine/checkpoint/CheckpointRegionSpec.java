package com.storyengine.checkpoint;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Описание региона сцены для чекпойнта — откуда брать центр, радиус и какие
 * теги считать «сюжетными». Разбирается из поля {@code checkpoint_region} в JSON
 * действия {@code save_checkpoint} (см. {@link SceneSnapshotManager}).
 *
 * <pre>
 * "checkpoint_region": {
 *   "center_tag": "story_npc_melan",
 *   "radius": 8,
 *   "entity_tags": ["story_npc_melan", "scene_prop_candles"],
 *   "blocks": [[x, y, z], ...]   // optional, точечные позиции в дополнение к радиусу
 * }
 * </pre>
 *
 * {@code center_tag} + {@code radius} описывают AABB вокруг сущности с тегом
 * (все не-air блоки внутри него попадают в снапшот);
 * {@code entity_tags} — какие сущности попадают в снапшот; {@code blocks} —
 * явный список дополнительных позиций вне радиуса (захватываются всегда,
 * даже если сейчас там air).
 */
public final class CheckpointRegionSpec {

    private static final int DEFAULT_RADIUS = 8;
    private static final int MAX_RADIUS = 64;

    @Nullable
    private final String centerTag;
    private final int radius;
    private final List<String> entityTags;
    private final List<BlockPos> explicitBlocks;

    private CheckpointRegionSpec(@Nullable String centerTag, int radius,
                                 List<String> entityTags, List<BlockPos> explicitBlocks) {
        this.centerTag = centerTag;
        this.radius = Math.max(0, Math.min(radius, MAX_RADIUS));
        this.entityTags = List.copyOf(entityTags);
        this.explicitBlocks = List.copyOf(explicitBlocks);
    }

    /**
     * Разбирает {@code checkpoint_region} из параметров действия триггера.
     * Возвращает пустой Optional, если поля нет или оно не является объектом.
     */
    public static Optional<CheckpointRegionSpec> parse(JsonObject actionParams) {
        if (actionParams == null || !actionParams.has("checkpoint_region")) {
            return Optional.empty();
        }
        JsonElement el = actionParams.get("checkpoint_region");
        if (el == null || !el.isJsonObject()) {
            return Optional.empty();
        }
        JsonObject region = el.getAsJsonObject();

        String centerTag = null;
        JsonElement ct = region.get("center_tag");
        if (ct != null && ct.isJsonPrimitive()) {
            String s = ct.getAsString();
            centerTag = s.isBlank() ? null : s;
        }

        int radius = DEFAULT_RADIUS;
        JsonElement r = region.get("radius");
        if (r != null && r.isJsonPrimitive() && r.getAsJsonPrimitive().isNumber()) {
            radius = r.getAsInt();
        }

        List<String> entityTags = new ArrayList<>();
        if (region.has("entity_tags") && region.get("entity_tags").isJsonArray()) {
            for (JsonElement e : region.getAsJsonArray("entity_tags")) {
                if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
                    String tag = e.getAsString();
                    if (!tag.isBlank()) {
                        entityTags.add(tag);
                    }
                }
            }
        }

        List<BlockPos> explicitBlocks = new ArrayList<>();
        if (region.has("blocks") && region.get("blocks").isJsonArray()) {
            for (JsonElement e : region.getAsJsonArray("blocks")) {
                BlockPos pos = parseBlockPos(e);
                if (pos != null) {
                    explicitBlocks.add(pos);
                }
            }
        }

        return Optional.of(new CheckpointRegionSpec(centerTag, radius, entityTags, explicitBlocks));
    }

    /** Регион «вокруг игрока» для ручной команды {@code /story checkpoint save}. */
    public static CheckpointRegionSpec forManual(int radius) {
        return new CheckpointRegionSpec(null, radius, List.of(), List.of());
    }

    @Nullable
    private static BlockPos parseBlockPos(JsonElement el) {
        if (el == null || !el.isJsonArray()) {
            return null;
        }
        JsonArray arr = el.getAsJsonArray();
        if (arr.size() != 3) {
            return null;
        }
        try {
            return new BlockPos(arr.get(0).getAsInt(), arr.get(1).getAsInt(), arr.get(2).getAsInt());
        } catch (UnsupportedOperationException | IllegalStateException e) {
            return null;
        }
    }

    @Nullable
    public String centerTag() {
        return centerTag;
    }

    public int radius() {
        return radius;
    }

    public List<String> entityTags() {
        return entityTags;
    }

    public List<BlockPos> explicitBlocks() {
        return explicitBlocks;
    }
}