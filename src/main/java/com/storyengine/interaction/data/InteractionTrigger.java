package com.storyengine.interaction.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Точка интерактивного взаимодействия (блок/объект в мире).
 *
 * Один JSON-файл в config/story_engine/triggers/ = одна точка (см. спецификацию
 * Interaction System §3). Детекция чисто позиционная: по BlockPos + измерению,
 * без привязки к типу блока (чтобы автор мог повесить триггер на любой блок).
 */
public class InteractionTrigger {

    /** Уникальный id точки (используется в сетевых пакетах и командах). */
    private String id = "";

    /** Тип сущности: "block" (пока только он, задел под "entity"). */
    private String type = "block";

    /** Абсолютные координаты блока [x, y, z]. */
    @SerializedName("pos")
    private int[] position = new int[3];

    /** Измерение (ResourceLocation, напр. minecraft:overworld). */
    private String dimension = "minecraft:overworld";

    /** Отображаемое имя объекта (шапка меню). */
    private String name = "";

    /** Максимальная дистанция взгляда от глаз игрока до блока. */
    private float maxDistance = 3.5f;

    /** Цвет 3D-контура блока (hex-строка вида 0xFF22C55E). */
    private String outlineColor = "0xFF22C55E";

    /** Включён ли триггер (false = не детектится и не исполняется). */
    private boolean enabled = true;

    /** Дополнительные блоки многоблочной структуры (абсолютные [x,y,z]). */
    private List<int[]> blocks = new ArrayList<>();

    /** Список действий (пункты меню). */
    private List<TriggerAction> actions = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int[] getPosition() {
        return position;
    }

    public void setPosition(int[] position) {
        this.position = position;
    }

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(float maxDistance) {
        this.maxDistance = maxDistance;
    }

    public String getOutlineColor() {
        return outlineColor;
    }

    public void setOutlineColor(String outlineColor) {
        this.outlineColor = outlineColor;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<int[]> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<int[]> blocks) {
        this.blocks = blocks;
    }

    /** Все позиции структуры: anchor (position) + дополнительные blocks[]. */
    public List<BlockPos> getBlockPoses() {
        List<BlockPos> poses = new ArrayList<>();
        poses.add(getBlockPos());
        if (blocks != null) {
            for (int[] b : blocks) {
                if (b == null) {
                    continue;
                }
                int x = b.length > 0 ? b[0] : 0;
                int y = b.length > 1 ? b[1] : 0;
                int z = b.length > 2 ? b[2] : 0;
                poses.add(new BlockPos(x, y, z));
            }
        }
        return poses;
    }

    public List<TriggerAction> getActions() {
        return actions;
    }

    public void setActions(List<TriggerAction> actions) {
        this.actions = actions;
    }

    /**
     * Убирает из списка точные дубли действий (одинаковые label и все эффекты),
     * чтобы в меню не появлялось дублирующихся строк, а индексы выбора на клиенте
     * и сервере совпадали. Вызывается при индексации триггера (см. TriggerManager)
     * и при сетевой синхронизации (см. InteractionClientState).
     */
    public void removeDuplicateActions() {
        if (actions == null) {
            actions = new ArrayList<>();
            return;
        }
        Set<String> seen = new HashSet<>();
        List<TriggerAction> unique = new ArrayList<>(actions.size());
        for (TriggerAction a : actions) {
            if (a == null) {
                continue;
            }
            if (seen.add(actionKey(a))) {
                unique.add(a);
            }
        }
        if (unique.size() != actions.size()) {
            actions = unique;
        }
    }

    private static String actionKey(TriggerAction a) {
        StringBuilder sb = new StringBuilder();
        sb.append(a.getLabel()).append('\u0001');
        sb.append(a.getCondition()).append('\u0001');
        sb.append(a.getDialogue()).append('\u0001');
        sb.append(a.getNpc()).append('\u0001');
        sb.append(a.getCommand()).append('\u0001');
        sb.append(a.getCompleteTask()).append('\u0001');
        sb.append(a.getSound()).append('\u0001');
        sb.append(a.getSetFlag()).append('\u0001');
        sb.append(a.getBlockAction()).append('\u0001');
        sb.append(a.isOpenStorage()).append('\u0001');
        TriggerAction.Storytell st = a.getStorytell();
        if (st != null) {
            sb.append(st.getSpeaker()).append('\u0002');
            if (st.getMessage() != null) {
                sb.append(st.getMessage());
            }
        }
        return sb.toString();
    }

    /** BlockPos из массива pos (защита от неполного массива). */
    public BlockPos getBlockPos() {
        int[] p = position != null ? position : new int[3];
        int x = p.length > 0 ? p[0] : 0;
        int y = p.length > 1 ? p[1] : 0;
        int z = p.length > 2 ? p[2] : 0;
        return new BlockPos(x, y, z);
    }

    /** ResourceLocation измерения (защита от кривого значения в конфиге). */
    public ResourceLocation getDimensionRL() {
        String d = dimension != null && !dimension.isBlank() ? dimension : "minecraft:overworld";
        ResourceLocation loc = ResourceLocation.tryParse(d);
        return loc != null ? loc : new ResourceLocation("minecraft:overworld");
    }

    /** Цвет контура как int (0xAARRGGBB). При ошибке парсинга — дефолт неоново-зелёный. */
    public int parseOutlineColor() {
        try {
            return (int) Long.parseLong(outlineColor.replace("0x", "").replace("0X", ""), 16);
        } catch (NumberFormatException e) {
            return 0xFF22C55E;
        }
    }
}
