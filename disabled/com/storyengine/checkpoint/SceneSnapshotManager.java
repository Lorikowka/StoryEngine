package com.storyengine.checkpoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.storyengine.StoryEngineMod;
import com.storyengine.player.PlayerDialogueData;
import com.storyengine.player.PlayerQuestDataHelper;
import com.storyengine.trigger.TriggerHintService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Менеджер чекпойнтов сцен (Scene Checkpoint System): снимает снапшот конкретной
 * сцены (все блоки региона + их BlockEntity NBT, сюжетные сущности, игрок) в файл
 * {@code config/story_engine/checkpoints/<id>.json} и применяет его обратно.
 *
 * Не является rollback всего мира: восстанавливаются только объекты, попавшие
 * в снапшот. Файлы — JSON (GSON, `setPrettyPrinting().disableHtmlEscaping()`),
 * NBT блоков/сущностей хранится как SNBT-строки, чтобы снэпшот был читаемым в git.
 *
 * Экземпляр хранится в {@code StoryEngineMod.SNAPSHOT_MANAGER}.
 */
public final class SceneSnapshotManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /** Допустимые символы id чекпойнта (используется как имя файла). */
    private static final String ID_PATTERN = "[A-Za-z0-9._-]+";

    private static final int BLOCK_UPDATE_FLAGS = 3;

    public SceneSnapshotManager() {
    }

    // ----------------------------------------------------------------
    // Директория и перечень
    // ----------------------------------------------------------------

    /** Директория чекпойнтов: config/story_engine/checkpoints/. */
    public Path getDirectory() {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("story_engine").resolve("checkpoints");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Не удалось создать директорию чекпойнтов: {}", dir, e);
        }
        return dir;
    }

    /**
     * Проходит по всем файлам чекпойнтов и проверяет их читаемость (call из
     * {@code onServerStarting}). Битые файлы не удаляются, а логируются.
     */
    public int loadAll() {
        Path dir = getDirectory();
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        int count = 0;
        try (Stream<Path> stream = Files.list(dir)) {
            for (Path file : stream.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList()) {
                Optional<SceneSnapshot> snapshot = readFile(file);
                if (snapshot.isPresent()) {
                    count++;
                } else {
                    LOGGER.warn("[StoryEngine] Чекпойнт {} повреждён или пропущен", file.getFileName());
                }
            }
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Ошибка чтения директории чекпойнтов {}: {}", dir, e.getMessage());
        }
        LOGGER.info("[StoryEngine] Загружено чекпойнтов: {}", count);
        return count;
    }

    /** Имена всех сохранённых чекпойнтов (без расширения .json). */
    public Collection<String> listIds() {
        Path dir = getDirectory();
        List<String> ids = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            for (Path file : stream.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList()) {
                String name = file.getFileName().toString();
                ids.add(name.substring(0, name.length() - ".json".length()));
            }
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Ошибка чтения директории чекпойнтов {}: {}", dir, e.getMessage());
        }
        return ids;
    }

    public Optional<SceneSnapshot> get(String id) {
        return isValidId(id) ? readFile(fileFor(id)) : Optional.empty();
    }

    public boolean delete(String id) {
        if (!isValidId(id)) {
            return false;
        }
        Path file = fileFor(id);
        if (!Files.isRegularFile(file)) {
            return false;
        }
        try {
            Files.deleteIfExists(file);
            return true;
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Не удалось удалить чекпойнт {}", file, e);
            return false;
        }
    }

    // ----------------------------------------------------------------
    // Capture
    // ----------------------------------------------------------------

    /**
     * Снимает снапшот сцены и сохраняет его в файл (перезаписывая при том же id).
     * Может вернуть пустой Optional, если заказан центр по тегу, но сущность с
     * тегом не найдена в радиусе — для действия это {@code FAILURE}.
     */
    public Optional<SceneSnapshot> capture(String id, ServerLevel level, ServerPlayer player,
                                           CheckpointRegionSpec region) {
        if (!isValidId(id)) {
            LOGGER.error("[StoryEngine] Захват чекпойнта: недопустимый id '{}'", id);
            return Optional.empty();
        }
        Vec3 center = resolveCenter(level, player, region);
        if (center == null) {
            LOGGER.warn("[StoryEngine] Чекпойнт '{}': центр по тегу '{}' не найден в радиусе {}",
                    id, region.centerTag(), region.radius());
            return Optional.empty();
        }

        double r = region.radius();
        AABB box = new AABB(center.x - r, center.y - r, center.z - r,
                center.x + r, center.y + r, center.z + r);

        Map<BlockPos, BlockSnapshot> blocks = new TreeMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ),
                Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ))) {
            if (!level.getBlockState(pos).isAir()) {
                blocks.put(pos, captureBlock(level, pos));
            }
        }
        for (BlockPos pos : region.explicitBlocks()) {
            if (!blocks.containsKey(pos)) {
                blocks.put(pos, captureBlock(level, pos));
            }
        }

        List<EntitySnapshot> entities = new ArrayList<>();
        if (!region.entityTags().isEmpty()) {
            for (Entity entity : level.getEntities((Entity) null, box, ignored -> true)) {
                if (entity instanceof ServerPlayer) {
                    continue;
                }
                if (region.entityTags().stream().anyMatch(entity.getTags()::contains)) {
                    entities.add(captureEntity(entity));
                }
            }
        }

        PlayerSnapshot playerSnapshot = capturePlayer(player);
        SceneSnapshot snapshot = new SceneSnapshot(
                id, level.dimension(),
                new double[]{center.x, center.y, center.z}, r,
                blocks, entities, Optional.of(playerSnapshot));

        saveToFile(snapshot);
        LOGGER.info("[StoryEngine] Чекпойнт '{}' сохранён ({} блоков, {} сущностей)",
                id, blocks.size(), entities.size());
        return Optional.of(snapshot);
    }

    /** Центр региона: сущность с {@code center_tag} в радиусе от игрока, иначе позиция игрока. */
    @Nullable
    private Vec3 resolveCenter(ServerLevel level, ServerPlayer player, CheckpointRegionSpec region) {
        String centerTag = region.centerTag();
        if (centerTag != null && !centerTag.isBlank()) {
            Entity target = StoryEngineMod.TRIGGER_MANAGER
                    .findTargetNear(level, player.position(), centerTag, region.radius());
            return target != null ? target.position() : null;
        }
        return player.position();
    }

    private BlockSnapshot captureBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return new BlockSnapshot(pos, NbtUtils.writeBlockState(state),
                blockEntity != null ? blockEntity.saveWithId() : null);
    }

    private EntitySnapshot captureEntity(Entity entity) {
        CompoundTag nbt = entity.saveWithoutId(new CompoundTag());
        String typeId = Registry.ENTITY_TYPE.getKey(entity.getType()).toString();
        return new EntitySnapshot(
                typeId,
                new ArrayList<>(entity.getTags()),
                new double[]{entity.getX(), entity.getY(), entity.getZ()},
                new float[]{entity.getYRot(), entity.getXRot()},
                nbt);
    }

    private PlayerSnapshot capturePlayer(ServerPlayer player) {
        return new PlayerSnapshot(
                player.level.dimension(),
                new double[]{player.getX(), player.getY(), player.getZ()},
                new float[]{player.getYRot(), player.getXRot()},
                PlayerDialogueData.get(player).serializeNBT(),
                PlayerQuestDataHelper.serializeNbt(player));
    }

    // ----------------------------------------------------------------
    // Restore
    // ----------------------------------------------------------------

    /**
     * Применяет сохранённый чекпойнт: восстанавливает блоки, затем сущности
     * (сначала удаляются старые сущности со сюжетными тегами в регионе снапшота,
     * потом спавнятся новые из NBT — чтобы не плодить дубликаты NPC), затем —
     * состояние игрока (телепорт + капабилити). Возвращает {@code false}, если
     * чекпойнта с таким id нет.
     */
    public boolean restore(String id, ServerPlayer player) {
        Optional<SceneSnapshot> opt = get(id);
        if (opt.isEmpty()) {
            LOGGER.warn("[StoryEngine] Restore: чекпойнт '{}' не найден", id);
            return false;
        }
        SceneSnapshot snapshot = opt.get();
        ServerLevel level = resolveLevel((ServerLevel) player.level, snapshot.dimension());

        for (BlockSnapshot block : snapshot.blocks().values()) {
            applyBlock(level, block);
        }
        applyEntities(level, snapshot);

        if (snapshot.player().isPresent()) {
            applyPlayer(player, snapshot.player().get());
        }
        LOGGER.info("[StoryEngine] Чекпойнт '{}' применён ({} блоков, {} сущностей, измерение {})",
                id, snapshot.blocks().size(), snapshot.entities().size(), level.dimension().location());
        return true;
    }

    private void applyBlock(ServerLevel level, BlockSnapshot block) {
        BlockPos pos = block.pos();
        BlockState state = NbtUtils.readBlockState(block.state());
        level.setBlock(pos, state, BLOCK_UPDATE_FLAGS);

        CompoundTag blockEntityNbt = block.blockEntity();
        if (blockEntityNbt != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                CompoundTag copy = blockEntityNbt.copy();
                copy.putInt("x", pos.getX());
                copy.putInt("y", pos.getY());
                copy.putInt("z", pos.getZ());
                blockEntity.load(copy);
                blockEntity.setChanged();
            } else {
                LOGGER.warn("[StoryEngine] Чекпойнт: нет BlockEntity на {} после установки {}", pos, state);
            }
        }
    }

    private void applyEntities(ServerLevel level, SceneSnapshot snapshot) {
        java.util.Set<String> snapshotTags = snapshot.entities().stream()
                .flatMap(e -> e.tags().stream())
                .collect(Collectors.toSet());

        double[] center = snapshot.regionCenter();
        if (center != null && center.length == 3) {
            double r = snapshot.regionRadius();
            AABB box = new AABB(center[0] - r, center[1] - r, center[2] - r,
                    center[0] + r, center[1] + r, center[2] + r);
            for (Entity entity : level.getEntities((Entity) null, box, ignored -> true)) {
                if (entity instanceof ServerPlayer) {
                    continue;
                }
                if (snapshotTags.stream().anyMatch(entity.getTags()::contains)) {
                    entity.discard();
                }
            }
        }

        for (EntitySnapshot entitySnapshot : snapshot.entities()) {
            CompoundTag nbt = entitySnapshot.nbt().copy();
            nbt.putString("id", entitySnapshot.typeId());
            Optional<Entity> restored = EntityType.create(nbt, level);
            if (restored.isEmpty()) {
                LOGGER.error("[StoryEngine] Чекпойнт '{}': не удалось создать сущность '{}'",
                        snapshot.id(), entitySnapshot.typeId());
                continue;
            }
            level.addFreshEntity(restored.get());
        }
    }

    private void applyPlayer(ServerPlayer player, PlayerSnapshot playerSnapshot) {
        ServerLevel target = resolveLevel((ServerLevel) player.level, playerSnapshot.dimension());
        player.teleportTo(target,
                playerSnapshot.pos()[0], playerSnapshot.pos()[1], playerSnapshot.pos()[2],
                playerSnapshot.rot()[0], playerSnapshot.rot()[1]);

        if (playerSnapshot.quests() != null) {
            PlayerQuestDataHelper.deserializeNbt(player, playerSnapshot.quests());
        }
        if (playerSnapshot.flags() != null) {
            PlayerDialogueData.get(player).deserializeNBT(playerSnapshot.flags());
        }
        TriggerHintService.get().resync(player, target);
    }

    private ServerLevel resolveLevel(ServerLevel fallback, ResourceKey<Level> key) {
        MinecraftServer server = fallback.getServer();
        ServerLevel level = server != null ? server.getLevel(key) : null;
        if (level == null) {
            LOGGER.warn("[StoryEngine] Измерение {} недоступно, используется текущее {}", key.location(),
                    fallback.dimension().location());
            return fallback;
        }
        return level;
    }

    // ----------------------------------------------------------------
    // Файлы: JSON (GSON), NBT — как SNBT-строки
    // ----------------------------------------------------------------

    private Path fileFor(String id) {
        return getDirectory().resolve(id + ".json");
    }

    private boolean isValidId(String id) {
        return id != null && id.matches(ID_PATTERN);
    }

    private Optional<SceneSnapshot> readFile(Path file) {
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            SceneSnapshot snapshot = root == null ? null : fromJson(root);
            return snapshot == null ? Optional.empty() : Optional.of(snapshot);
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.error("[StoryEngine] Ошибка чтения чекпойнта {}: {}", file, e.getMessage());
            return Optional.empty();
        }
    }

    private void saveToFile(SceneSnapshot snapshot) {
        Path file = fileFor(snapshot.id());
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(toJson(snapshot), writer);
        } catch (IOException e) {
            LOGGER.error("[StoryEngine] Не удалось записать чекпойнт {}", file, e);
        }
    }

    // ----------------------------------------------------------------
    // JsonObject <-> SceneSnapshot
    // ----------------------------------------------------------------

    @Nullable
    private SceneSnapshot fromJson(JsonObject root) {
        try {
            String id = root.get("id").getAsString();
            ResourceKey<Level> dimension = parseDimension(root.get("dimension").getAsString());
            if (dimension == null || !isValidId(id)) {
                return null;
            }
            double[] center = parseDoubleArray(root.get("region_center"));
            double radius = root.has("region_radius") ? root.get("region_radius").getAsDouble() : 0.0;

            Map<BlockPos, BlockSnapshot> blocks = new LinkedHashMap<>();
            if (root.has("blocks") && root.get("blocks").isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray("blocks")) {
                    if (!el.isJsonObject()) {
                        continue;
                    }
                    BlockSnapshot block = fromBlockJson(el.getAsJsonObject());
                    if (block != null) {
                        blocks.put(block.pos(), block);
                    }
                }
            }

            List<EntitySnapshot> entities = new ArrayList<>();
            if (root.has("entities") && root.get("entities").isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray("entities")) {
                    if (el.isJsonObject()) {
                        EntitySnapshot entity = fromEntityJson(el.getAsJsonObject());
                        if (entity != null) {
                            entities.add(entity);
                        }
                    }
                }
            }

            PlayerSnapshot player = root.has("player") && root.get("player").isJsonObject()
                    ? fromPlayerJson(root.getAsJsonObject("player")) : null;

            return new SceneSnapshot(id, dimension, center, radius, blocks, entities,
                    player != null ? Optional.of(player) : Optional.empty());
        } catch (Exception e) {
            LOGGER.error("[StoryEngine] Битый чекпойнт: {}", e.getMessage());
            return null;
        }
    }

    private JsonObject toJson(SceneSnapshot snapshot) {
        JsonObject root = new JsonObject();
        root.addProperty("id", snapshot.id());
        root.addProperty("dimension", snapshot.dimension().location().toString());
        if (snapshot.regionCenter() != null) {
            root.add("region_center", toDoubleArray(snapshot.regionCenter()));
            root.addProperty("region_radius", snapshot.regionRadius());
        }

        JsonArray blocks = new JsonArray();
        for (BlockSnapshot block : snapshot.blocks().values()) {
            JsonObject obj = new JsonObject();
            obj.add("pos", toIntArray(
                    block.pos().getX(), block.pos().getY(), block.pos().getZ()));
            obj.addProperty("state", toSnbt(block.state()));
            obj.addProperty("block_entity", toSnbt(block.blockEntity()));
            blocks.add(obj);
        }
        root.add("blocks", blocks);

        JsonArray entities = new JsonArray();
        for (EntitySnapshot entity : snapshot.entities()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", entity.typeId());
            obj.add("tags", toStringArray(entity.tags()));
            obj.add("pos", toDoubleArray(new double[]{entity.pos()[0], entity.pos()[1], entity.pos()[2]}));
            obj.add("rot", toDoubleArray(new double[]{entity.rot()[0], entity.rot()[1]}));
            obj.addProperty("nbt", toSnbt(entity.nbt()));
            entities.add(obj);
        }
        root.add("entities", entities);

        if (snapshot.player().isPresent()) {
            PlayerSnapshot player = snapshot.player().get();
            JsonObject obj = new JsonObject();
            obj.addProperty("dimension", player.dimension().location().toString());
            obj.add("pos", toDoubleArray(player.pos()));
            obj.add("rot", toDoubleArray(new double[]{player.rot()[0], player.rot()[1]}));
            obj.addProperty("flags", toSnbt(player.flags()));
            obj.addProperty("quests", toSnbt(player.quests()));
            root.add("player", obj);
        }
        return root;
    }

    @Nullable
    private BlockSnapshot fromBlockJson(JsonObject obj) {
        BlockPos pos = parseBlockPos(obj.get("pos"));
        if (pos == null) {
            return null;
        }
        CompoundTag state = toTag(obj.has("state") ? obj.get("state").getAsString() : null);
        if (state == null) {
            return null;
        }
        CompoundTag blockEntity = toTag(obj.has("block_entity") ? obj.get("block_entity").getAsString() : null);
        return new BlockSnapshot(pos, state, blockEntity);
    }

    @Nullable
    private EntitySnapshot fromEntityJson(JsonObject obj) {
        String typeId = obj.has("type") ? obj.get("type").getAsString() : null;
        CompoundTag nbt = toTag(obj.has("nbt") ? obj.get("nbt").getAsString() : null);
        if (typeId == null || nbt == null) {
            return null;
        }
        List<String> tags = new ArrayList<>();
        if (obj.has("tags") && obj.get("tags").isJsonArray()) {
            for (JsonElement el : obj.getAsJsonArray("tags")) {
                tags.add(el.getAsString());
            }
        }
        return new EntitySnapshot(typeId, tags, parseDoubleArray(obj.get("pos")), new float[]{0F, 0F}, nbt);
    }

    @Nullable
    private PlayerSnapshot fromPlayerJson(JsonObject obj) {
        ResourceKey<Level> dimension = parseDimension(obj.get("dimension").getAsString());
        if (dimension == null) {
            return null;
        }
        double[] pos = parseDoubleArray(obj.get("pos"));
        double[] rot = parseDoubleArray(obj.get("rot"));
        CompoundTag flags = toTag(obj.get("flags").getAsString());
        CompoundTag quests = toTag(obj.has("quests") ? obj.get("quests").getAsString() : null);
        if (pos == null || flags == null) {
            return null;
        }
        float[] rotF = rot != null && rot.length == 2
                ? new float[]{(float) rot[0], (float) rot[1]} : new float[]{0F, 0F};
        return new PlayerSnapshot(dimension, pos, rotF, flags, quests);
    }

    // ----------------------------------------------------------------
    // Хелперы JSON/NBT
    // ----------------------------------------------------------------

    @Nullable
    private static ResourceKey<Level> parseDimension(String raw) {
        ResourceLocation location = ResourceLocation.tryParse(raw);
        return location != null ? ResourceKey.create(Registry.DIMENSION_REGISTRY, location) : null;
    }

    @Nullable
    private static double[] parseDoubleArray(JsonElement el) {
        if (el == null || !el.isJsonArray()) {
            return null;
        }
        JsonArray arr = el.getAsJsonArray();
        double[] result = new double[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            result[i] = arr.get(i).getAsDouble();
        }
        return result;
    }

    private static JsonArray toDoubleArray(double[] values) {
        JsonArray arr = new JsonArray();
        for (double value : values) {
            arr.add(value);
        }
        return arr;
    }

    private static JsonArray toIntArray(int x, int y, int z) {
        JsonArray arr = new JsonArray();
        arr.add(x);
        arr.add(y);
        arr.add(z);
        return arr;
    }

    private static JsonArray toStringArray(Collection<String> values) {
        JsonArray arr = new JsonArray();
        for (String value : values) {
            arr.add(value);
        }
        return arr;
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
        return new BlockPos(arr.get(0).getAsInt(), arr.get(1).getAsInt(), arr.get(2).getAsInt());
    }

    /** NBT в SNBT для хранения в JSON (читабельность/diff в git). */
    @Nullable
    private static String toSnbt(@Nullable CompoundTag tag) {
        return tag == null ? null : tag.toString();
    }

    @Nullable
    private static CompoundTag toTag(@Nullable String snbt) {
        if (snbt == null || snbt.isBlank() || "null".equals(snbt)) {
            return null;
        }
        try {
            return TagParser.parseTag(snbt);
        } catch (CommandSyntaxException e) {
            LOGGER.error("[StoryEngine] Битый SNBT: {}", snbt);
            return null;
        }
    }
}