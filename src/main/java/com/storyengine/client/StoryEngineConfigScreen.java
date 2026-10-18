package com.storyengine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Экран настроек Story Engine для кнопки «Настройки» в списке модов
 * (Моды → Story Engine → Настройки).
 *
 * Поверх config/story_engine-client.toml: тумблеры (boolean), слайдеры
 * (int/double с пределами из конфига), редактор цветов (HEX + образец),
 * разделы со скроллом, подсказки при наведении. Изменения применяются
 * сразу ({@link ForgeConfigSpec.ConfigValue#set}) и сохраняются на диск
 * ({@link ForgeConfigSpec#save()}).
 *
 * Регистрация фабрики — {@link #register()} (вызывается из конструктора
 * мода только на клиенте): без неё кнопка «Настройки» в Forge-списке
 * остаётся неактивной.
 */
public final class StoryEngineConfigScreen extends Screen {

    private enum Kind { TOGGLE, SLIDER, COLOR }

    /** Строка настройки или заголовок раздела (value == null). */
    private static final class View {
        final ForgeConfigSpec.ConfigValue<?> value;
        final String label;
        final Kind kind;
        final double min;
        final double max;
        final String hint;
        final int h;
        /** Обрезанная под ширину подпись (заполняется в rebuild). */
        String shown;
        /** Позиция в координатах контента и виджетная зона (заполняется в rebuild). */
        int y;
        int wx;
        int ww;

        View(ForgeConfigSpec.ConfigValue<?> value, String label, Kind kind,
             double min, double max, String hint, int h) {
            this.value = value;
            this.label = label;
            this.kind = kind;
            this.min = min;
            this.max = max;
            this.hint = hint;
            this.h = h;
        }

        boolean isHeader() {
            return value == null;
        }
    }

    private static final int HEADER_H = 16;
    private static final int ROW_H = 18;
    private static final String HINT_COLOR = "Формат ARGB: AARRGGBB (8 hex-цифр), например FF22C55E. "
            + "Первая пара — прозрачность (FF = непрозрачный).";

    /** Все строки меню (статические: значения читаются из конфига на лету). */
    private static final List<View> VIEWS = new ArrayList<>();

    static {
        // === Меню квестов — общее ===
        header("Меню квестов — общее");
        bool("Использовать кастомные текстуры и цвета", MenuCustomizationConfig.ENABLED,
                "false — встроенные текстуры и цвета мода, папка menu/ и правки цветов игнорируются.");
        sliderD("Фиксированный масштаб меню (0 = авто)", MenuCustomizationConfig.UI_SCALE_OVERRIDE, 0.0, 1.5,
                "0.0 = авто-подгонка под размер окна; 0.1..1.5 — жёсткий масштаб.");
        sliderD("Масштаб заголовка квеста", MenuCustomizationConfig.FONT_SCALE, 0.5, 2.0,
                "Множитель размера заголовка в панели деталей, 1.0 = базовый.");

        // === Меню квестов — цвета ===
        header("Меню квестов — цвета");
        color("Подложка окна", MenuCustomizationConfig.WINDOW_FILL, null);
        color("Основной текст", MenuCustomizationConfig.TEXT_PRIMARY, null);
        color("Метки-подписи («Автор:», «Цели:»)", MenuCustomizationConfig.TEXT_LABEL, null);
        color("Имя автора в списке", MenuCustomizationConfig.TEXT_AUTHOR, null);
        color("Пустое состояние", MenuCustomizationConfig.TEXT_EMPTY_STATE, null);
        color("Акцент (вкладка, кнопка)", MenuCustomizationConfig.ACCENT, null);
        color("Вкладка: наведение", MenuCustomizationConfig.TAB_HOVER, null);
        color("Вкладка: покой", MenuCustomizationConfig.TAB_IDLE, null);
        color("Кнопка: наведение", MenuCustomizationConfig.BUTTON_HOVER, null);
        color("Кнопка: покой", MenuCustomizationConfig.BUTTON_IDLE, null);
        color("Задача: обычный текст", MenuCustomizationConfig.TASK_NORMAL, null);
        color("Задача: выполнена", MenuCustomizationConfig.TASK_DONE, null);
        color("Задача: провалена", MenuCustomizationConfig.TASK_FAILED, null);
        color("Описание задачи", MenuCustomizationConfig.TASK_DESCRIPTION, null);
        color("Координаты локации", MenuCustomizationConfig.TASK_LOCATION, null);
        color("Фон-пилюля строки задачи", MenuCustomizationConfig.PILL_BACKGROUND, null);
        color("Скроллбар: трек", MenuCustomizationConfig.SCROLLBAR_TRACK, null);
        color("Скроллбар: бегунок", MenuCustomizationConfig.SCROLLBAR_THUMB, null);

        // === Окно диалогов ===
        header("Окно диалогов");
        bool("Использовать кастомные настройки диалогов", MenuCustomizationConfig.DIALOGUE_ENABLED,
                "false — рисовать окно диалогов встроенными значениями по умолчанию.");
        sliderI("Высота панели реплики (px)", MenuCustomizationConfig.DIALOGUE_BAR_HEIGHT, 32, 400, null);
        sliderI("Скорость печати (симв/сек)", MenuCustomizationConfig.DIALOGUE_TEXT_SPEED, 0, 200,
                "0 = мгновенно, 25 ~ эталонная скорость.");
        sliderI("Отступ текста слева (px)", MenuCustomizationConfig.DIALOGUE_TEXT_LEFT_INDENT, 0, 200,
                "Комфортное значение 12–16 пикселей.");
        sliderI("Отступ текста справа (px)", MenuCustomizationConfig.DIALOGUE_TEXT_RIGHT_INDENT, 0, 200, null);

        // === Диалог — цвета ===
        header("Окно диалогов — цвета");
        color("Фон панели реплики", MenuCustomizationConfig.DIALOGUE_BAR_FILL, null);
        color("Разделительная линия", MenuCustomizationConfig.DIALOGUE_DIVIDER_COLOR, null);
        color("Плашка спикера: фон", MenuCustomizationConfig.DIALOGUE_SPEAKER_PLATE_FILL, null);
        color("Плашка спикера: рамка", MenuCustomizationConfig.DIALOGUE_SPEAKER_PLATE_BORDER, null);
        color("Плашка спикера: акцент сверху", MenuCustomizationConfig.DIALOGUE_SPEAKER_ACCENT, null);
        color("Имя спикера", MenuCustomizationConfig.DIALOGUE_SPEAKER_NAME_COLOR, null);
        color("Текст реплики", MenuCustomizationConfig.DIALOGUE_TEXT_COLOR, null);

        // === Варианты ответов — геометрия ===
        header("Варианты ответов — геометрия");
        sliderI("Мин. ширина кнопки (px)", MenuCustomizationConfig.DIALOGUE_RESPONSE_BOX_WIDTH, 40, 2000,
                "Порог, чтобы короткие ответы («Да», «Нет») не были обрубками; при длинном тексте кнопка шире.");
        sliderI("Гориз. отступ текста (px)", MenuCustomizationConfig.DIALOGUE_RESPONSE_H_PADDING, 4, 80,
                "Ширина кнопки = ширина текста + отступ × 2.");
        sliderI("Высота кнопки (px)", MenuCustomizationConfig.DIALOGUE_RESPONSE_BOX_HEIGHT, 12, 200, null);
        sliderI("Зазор между кнопками (px)", MenuCustomizationConfig.DIALOGUE_RESPONSE_ROW_GAP, 4, 100, null);
        sliderI("Отступ блока от левого края (px)", MenuCustomizationConfig.DIALOGUE_RESPONSE_X, 0, 2000, null);
        sliderI("Отступ первой кнопки сверху (px)", MenuCustomizationConfig.DIALOGUE_RESPONSE_Y, 0, 2000, null);

        // === Варианты ответов — цвета ===
        header("Варианты ответов — цвета");
        color("Покой: фон", MenuCustomizationConfig.DIALOGUE_RESPONSE_IDLE_FILL, null);
        color("Покой: рамка", MenuCustomizationConfig.DIALOGUE_RESPONSE_IDLE_BORDER, null);
        color("Покой: текст", MenuCustomizationConfig.DIALOGUE_RESPONSE_IDLE_TEXT, null);
        color("Наведение: фон", MenuCustomizationConfig.DIALOGUE_RESPONSE_HOVER_FILL, null);
        color("Наведение: рамка", MenuCustomizationConfig.DIALOGUE_RESPONSE_HOVER_BORDER, null);
        color("Наведение: текст", MenuCustomizationConfig.DIALOGUE_RESPONSE_HOVER_TEXT, null);
        color("Заблокирован: фон", MenuCustomizationConfig.DIALOGUE_RESPONSE_DISABLED_FILL, null);
        color("Заблокирован: рамка", MenuCustomizationConfig.DIALOGUE_RESPONSE_DISABLED_BORDER, null);
        color("Заблокирован: текст", MenuCustomizationConfig.DIALOGUE_RESPONSE_DISABLED_TEXT, null);
        color("Нажатие: фон", MenuCustomizationConfig.DIALOGUE_RESPONSE_PRESSED_FILL, null);
        color("Нажатие: рамка", MenuCustomizationConfig.DIALOGUE_RESPONSE_PRESSED_BORDER, null);
        color("Нажатие: текст", MenuCustomizationConfig.DIALOGUE_RESPONSE_PRESSED_TEXT, null);

        // === История сюжетного чата ===
        header("История сюжетного чата (журнал)");
        color("Шапка: фон", MenuCustomizationConfig.LOG_HEADER_FILL, null);
        color("Подвал: фон", MenuCustomizationConfig.LOG_FOOTER_FILL, null);
        color("Акцентная линия", MenuCustomizationConfig.LOG_ACCENT_LINE, null);
        color("Заголовок", MenuCustomizationConfig.LOG_TITLE_COLOR, null);
        color("Подсказка скролла", MenuCustomizationConfig.LOG_HINT_COLOR, null);
        color("Фон ленты сообщений", MenuCustomizationConfig.LOG_FEED_FILL, null);
        color("Подложка строки сообщения", MenuCustomizationConfig.LOG_CHAT_LINE_BG, null);
        color("Текст сообщения", MenuCustomizationConfig.LOG_BODY_COLOR, null);
        color("Пустое состояние", MenuCustomizationConfig.LOG_EMPTY_COLOR, null);
        color("Скроллбар: трек", MenuCustomizationConfig.LOG_SCROLLBAR_TRACK, null);
        color("Скроллбар: бегунок", MenuCustomizationConfig.LOG_SCROLLBAR_THUMB, null);

        // === Подсказки взаимодействия ===
        header("Подсказки взаимодействия (HUD [F] и кольца)");
        bool("Мастер-включение (HUD, кольца, F-фолбэк)", MenuCustomizationConfig.INTERACTION_HINT_ENABLED,
                "false — исчезает плашка «[F] …», кнопка F и кольца, ПКМ работает как обычно.");
        bool("Текстура плашки (menu/interaction_hint.png)", MenuCustomizationConfig.INTERACTION_HINT_USE_TEXTURE,
                "false — плашка сплошной заливкой цветами ниже.");
        sliderI("Радиус скана близких целей (блоки)", MenuCustomizationConfig.INTERACTION_HINT_SCAN_RADIUS, 0, 8,
                "Куб скана вокруг игрока; оригинал мода использовал 5.");
        sliderI("Лимит колец-маркеров (0 = без лимита)", MenuCustomizationConfig.INTERACTION_HINT_MAX_MARKERS, 0, 256,
                "При превышении остаются ближайшие цели.");
        sliderI("HUD: отступ слева (px)", MenuCustomizationConfig.INTERACTION_HINT_LEFT_OFFSET, 0, 2000, null);
        sliderI("HUD: отступ снизу (px)", MenuCustomizationConfig.INTERACTION_HINT_BOTTOM_OFFSET, 0, 2000, null);
        color("Плашка HUD: фон", MenuCustomizationConfig.INTERACTION_HINT_FILL, null);
        color("Плашка HUD: рамка", MenuCustomizationConfig.INTERACTION_HINT_BORDER, null);
        color("Плашка HUD: текст", MenuCustomizationConfig.INTERACTION_HINT_TEXT, null);
        color("Кольцо у цели (покой)", MenuCustomizationConfig.INTERACTION_HINT_MARKER,
                "Кольцо-маркер близкого интерактибла в покое; альфа ниже FF — полупрозрачное.");
        color("Кольцо под прицелом", MenuCustomizationConfig.INTERACTION_HINT_MARKER_AIMED, null);

        // === Сюжетный чат (Narrative HUD) ===
        header("Сюжетный чат (Narrative HUD)");
        sliderI("Скорость печати (симв/сек)", MenuCustomizationConfig.NARRATIVE_TEXT_SPEED, 0, 200,
                "0 = мгновенно, 20 ~ эталонная скорость.");
        sliderI("Мин. пауза после допечатки (тики)", MenuCustomizationConfig.NARRATIVE_HOLD_MIN_TICKS, 0, 600,
                "20 тиков = 1 сек; реплика уходит не раньше этого времени.");
        sliderI("Пауза за каждый символ (тики)", MenuCustomizationConfig.NARRATIVE_HOLD_TICKS_PER_CHAR, 0, 60,
                "Итоговая пауза = max(мин. пауза, длина × это); 0 — всегда держать минимум.");
        sliderI("Очередь реплик (макс.)", MenuCustomizationConfig.NARRATIVE_MAX_QUEUE, 1, 512,
                "При переполнении самая старая реплика отбрасывается.");
    }

    private static void header(String title) {
        VIEWS.add(new View(null, title, null, 0, 0, null, HEADER_H));
    }

    private static void bool(String label, ForgeConfigSpec.BooleanValue v, String hint) {
        VIEWS.add(new View(v, label, Kind.TOGGLE, 0, 0, hint, ROW_H));
    }

    private static void sliderI(String label, ForgeConfigSpec.IntValue v, int min, int max, String hint) {
        VIEWS.add(new View(v, label, Kind.SLIDER, min, max, hint, ROW_H));
    }

    private static void sliderD(String label, ForgeConfigSpec.DoubleValue v, double min, double max, String hint) {
        VIEWS.add(new View(v, label, Kind.SLIDER, min, max, hint, ROW_H));
    }

    private static void color(String label, ForgeConfigSpec.IntValue v, String hint) {
        VIEWS.add(new View(v, label, Kind.COLOR, 0, 0,
                hint == null ? HINT_COLOR : hint, ROW_H));
    }

    // ----------------------------------------------------------------
    //  Регистрация фабрики (вызывается из StoryEngineMod только на клиенте)
    // ----------------------------------------------------------------

    /**
     * Регистрирует фабрику экрана для кнопки «Настройки» в списке модов Forge.
     * Без регистрации кнопка остаётся неактивной ({@code ModListScreen}
     * включает её только при наличии {@code ConfigScreenFactory}).
     */
    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(StoryEngineConfigScreen::new));
    }

    // ----------------------------------------------------------------
    //  Экран
    // ----------------------------------------------------------------

    private final Screen parent;

    /** Геометрия области контента (считается в init). */
    private int listX;
    private int listW;
    private int top;
    private int bottom;

    private double scroll;
    private int contentH;

    /** Перетаскиваемый слайдер (null — нет). */
    private View dragView;

    /** Открытый редактор цвета (null — закрыт) и его геометрия. */
    private EditBox colorEdit;
    private View colorView;
    private int editX;
    private int editY;
    private int editW;
    private int editH;

    public StoryEngineConfigScreen(Screen parent) {
        super(Component.literal("Настройки Story Engine"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        cancelColor();
        dragView = null;

        listX = 12;
        listW = Math.max(220, width - listX - 18 - 10); // - жёлоб скроллбара
        top = 34;
        bottom = height - 32;

        rebuild();

        addRenderableWidget(new Button(12, height - 24, 120, 20,
                Component.literal("Сбросить всё"), b -> resetAll()));
        addRenderableWidget(new Button(width - 112, height - 24, 100, 20,
                Component.literal("Готово"), b -> this.onClose()));

        // Редактор цвета — обычный child-виджет, скрыт вне правки:
        // ввод идёт через фокус Screen'а (setInitialFocus в openColor).
        colorEdit = new EditBox(font, 0, -100, 66, 14, Component.literal("HEX"));
        colorEdit.setMaxLength(8);
        colorEdit.setFilter(s -> s.isEmpty() || s.matches("[0-9a-fA-F]+"));
        colorEdit.visible = false;
        addRenderableWidget(colorEdit);
    }

    @Override
    public void onClose() {
        cancelColor();
        dragView = null;
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    /** Пересчитать позиции строк и зоны виджетов под текущую ширину. */
    private void rebuild() {
        int y = 0;
        int right = listX + listW - 4;
        for (View v : VIEWS) {
            v.y = y;
            y += v.h;
            if (v.isHeader()) {
                v.shown = v.label;
                continue;
            }
            switch (v.kind) {
                case TOGGLE -> {
                    v.ww = 54;
                    v.wx = right - v.ww;
                }
                case SLIDER -> {
                    int valueW = 46;
                    v.ww = 110;
                    v.wx = right - valueW - 4 - v.ww;
                }
                case COLOR -> {
                    v.ww = 66;
                    v.wx = right - 14 - 4 - v.ww;
                }
            }
            v.shown = ellipsize(v.label, v.wx - (listX + 6) - 6);
        }
        contentH = y;
        scroll = clamp(scroll, 0, scrollMax());
    }

    private double scrollMax() {
        return Math.max(0, contentH - (double) (bottom - top));
    }

    private String ellipsize(String s, int maxW) {
        if (maxW <= 8 || font.width(s) <= maxW) {
            return s;
        }
        String t = s;
        while (t.length() > 1 && font.width(t + "…") > maxW) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "…";
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    // ----------------------------------------------------------------
    //  Отрисовка
    // ----------------------------------------------------------------

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);

        String titleStr = this.title.getString();
        GuiComponent.drawString(pose, font, titleStr, (width - font.width(titleStr)) / 2, 8, 0xFFFFFFFF);
        String sub = "Всё сохраняется в config/story_engine-client.toml";
        GuiComponent.drawString(pose, font, sub, (width - font.width(sub)) / 2, 19, 0xFF9A9A9A);

        // Панель контента
        GuiComponent.fill(pose, listX - 6, top - 6, listX + listW + 10, bottom + 6, 0xF01A1E28);

        int scrollInt = (int) scroll;
        View hovered = null;

        enableScissor(listX - 6, top - 6, listX + listW + 10, bottom + 6);
        int idx = 0;
        for (View v : VIEWS) {
            int sy = top + v.y - scrollInt;
            if (sy + v.h >= top - 6 && sy <= bottom + 6) {
                boolean rowHover = mouseY >= top && mouseY <= bottom
                        && mouseX >= listX && mouseX <= listX + listW
                        && mouseY - top + scroll >= v.y && mouseY - top + scroll < v.y + v.h;
                if (v.isHeader()) {
                    GuiComponent.drawString(pose, font, v.label, listX + 2, sy + 4, 0xFF88B4FF);
                    GuiComponent.fill(pose, listX, sy + v.h - 1, listX + listW, sy + v.h, 0x40FFFFFF);
                } else {
                    int bg = rowHover ? 0x30FFFFFF : ((idx & 1) == 0 ? 0x10FFFFFF : 0x06FFFFFF);
                    GuiComponent.fill(pose, listX, sy, listX + listW, sy + v.h, bg);
                    GuiComponent.drawString(pose, font, v.shown, listX + 6, sy + 5,
                            rowHover ? 0xFFFFFFFF : 0xFFDDDDDD);
                    renderWidget(pose, v, sy, mouseX, mouseY);
                    if (rowHover) {
                        hovered = v;
                    }
                }
            }
            idx++;
        }
        disableScissor();

        // Скроллбар
        double max = scrollMax();
        if (max > 0) {
            int trackX = listX + listW + 2;
            GuiComponent.fill(pose, trackX, top, trackX + 4, bottom, 0x40FFFFFF);
            int viewH = bottom - top;
            int thumbH = Math.max(20, (int) (viewH * (double) viewH / contentH));
            int thumbY = top + (int) ((bottom - top - thumbH) * (scroll / max));
            GuiComponent.fill(pose, trackX, thumbY, trackX + 4, thumbY + thumbH, 0xC0FFFFFF);
        }

        super.render(pose, mouseX, mouseY, partialTick);

        if (hovered != null && hovered.hint != null) {
            renderTooltip(pose, Component.literal(hovered.hint), mouseX, mouseY);
        }
    }

    private void renderWidget(PoseStack pose, View v, int sy, int mouseX, int mouseY) {
        switch (v.kind) {
            case TOGGLE -> {
                boolean on = Boolean.TRUE.equals(v.value.get());
                boolean hov = mouseX >= v.wx && mouseX <= v.wx + v.ww
                        && mouseY >= sy && mouseY <= sy + v.h;
                GuiComponent.fill(pose, v.wx, sy + 2, v.wx + v.ww, sy + v.h - 2,
                        hov ? (on ? 0xFF35874A : 0xFF4A4A55) : (on ? 0xFF2A6B3C : 0xFF3A3A42));
                String txt = on ? "Вкл" : "Выкл";
                GuiComponent.drawString(pose, font, txt,
                        v.wx + (v.ww - font.width(txt)) / 2, sy + 5, on ? 0xFF9DFFB0 : 0xFFBBBBBB);
            }
            case SLIDER -> {
                int trackX = v.wx;
                int trackY = sy + 6;
                int trackH = 6;
                double cur = sliderT(v);
                String val = formatValue(v);
                GuiComponent.drawString(pose, font, val,
                        listX + listW - 4 - font.width(val), sy + 5, 0xFFC8C8C8);
                GuiComponent.fill(pose, trackX, trackY, trackX + v.ww, trackY + trackH, 0xFF2A2E36);
                int fillX = trackX + (int) (v.ww * cur);
                GuiComponent.fill(pose, trackX, trackY, fillX, trackY + trackH, 0xFF4F7BC4);
                GuiComponent.fill(pose, fillX - 3, trackY - 3, fillX + 3, trackY + trackH + 3, 0xFFE6E6E6);
            }
            case COLOR -> {
                int argb = (Integer) v.value.get();
                GuiComponent.fill(pose, v.wx, sy + 2, v.wx + v.ww, sy + v.h - 2, 0xE0101216);
                GuiComponent.fill(pose, v.wx, sy + 2, v.wx + v.ww, sy + 3, 0xFF3A3A42);
                GuiComponent.fill(pose, v.wx, sy + v.h - 3, v.wx + v.ww, sy + v.h - 2, 0xFF3A3A42);
                GuiComponent.fill(pose, v.wx, sy + 2, v.wx + 1, sy + v.h - 2, 0xFF3A3A42);
                GuiComponent.fill(pose, v.wx + v.ww - 1, sy + 2, v.wx + v.ww, sy + v.h - 2, 0xFF3A3A42);
                if (colorEdit == null || colorView != v) {
                    String hex = String.format(Locale.ROOT, "%08X", argb);
                    GuiComponent.drawString(pose, font, hex, v.wx + 5, sy + 5, 0xFFCCCCCC);
                }
                int sx = v.wx + v.ww + 4;
                GuiComponent.fill(pose, sx - 1, sy + 1, sx + 15, sy + v.h - 1, 0x50FFFFFF);
                GuiComponent.fill(pose, sx, sy + 2, sx + 14, sy + v.h - 2, argb);
            }
        }
    }

    private double sliderT(View v) {
        Object raw = v.value.get();
        double cur = raw instanceof Integer i ? i : raw instanceof Double d ? d : v.min;
        return clamp((cur - v.min) / (v.max - v.min), 0.0, 1.0);
    }

    private String formatValue(View v) {
        Object raw = v.value.get();
        if (raw instanceof Double d) {
            return String.format(Locale.ROOT, "%.2f", d);
        }
        return String.valueOf(raw);
    }

    // ----------------------------------------------------------------
    //  Ввод
    // ----------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (colorEdit != null && colorView != null) {
            boolean inField = mouseX >= editX && mouseX <= editX + editW
                    && mouseY >= editY && mouseY <= editY + editH;
            if (!inField) {
                commitColor();
            }
            // клик внутри поля super.mouseClicked отдаст самому EditBox'у
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0 || mouseY < top || mouseY > bottom
                || mouseX < listX || mouseX > listX + listW + 18) {
            return false;
        }
        View v = viewAt(mouseX, mouseY);
        if (v == null || v.isHeader()) {
            return false;
        }
        return switch (v.kind) {
            case TOGGLE -> {
                if (mouseX < v.wx || mouseX > v.wx + v.ww) {
                    yield false;
                }
                boolean on = Boolean.TRUE.equals(v.value.get());
                set(v, !on);
                MenuCustomizationConfig.SPEC.save();
                yield true;
            }
            case SLIDER -> {
                if (mouseX < v.wx - 4 || mouseX > v.wx + v.ww + 4) {
                    yield false;
                }
                dragView = v;
                updateSlider(v, mouseX);
                yield true;
            }
            case COLOR -> {
                if (mouseX < v.wx || mouseX > v.wx + v.ww + 18) {
                    yield false;
                }
                openColor(v);
                yield true;
            }
        };
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragView != null) {
            updateSlider(dragView, mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragView != null) {
            dragView = null;
            MenuCustomizationConfig.SPEC.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (colorEdit != null) {
            cancelColor();
        }
        scroll = clamp(scroll - delta * 14.0, 0, scrollMax());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (colorEdit != null && colorView != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                commitColor();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                cancelColor();
                return true;
            }
        }
        // прочие клавиши super.keyPressed отдаст сфокусированному EditBox'у
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Строка под курсором (координаты контента) или null. */
    private View viewAt(double mouseX, double mouseY) {
        int contentY = (int) (mouseY - top + scroll);
        for (View v : VIEWS) {
            if (contentY >= v.y && contentY < v.y + v.h) {
                return v;
            }
        }
        return null;
    }

    private void updateSlider(View v, double mouseX) {
        double t = clamp((mouseX - v.wx) / (double) v.ww, 0.0, 1.0);
        double raw = v.min + t * (v.max - v.min);
        if (v.value instanceof ForgeConfigSpec.DoubleValue) {
            double rounded = clamp(Math.round(raw * 100.0) / 100.0, v.min, v.max);
            if (rounded != (Double) v.value.get()) {
                set(v, rounded);
            }
        } else {
            int rounded = (int) clamp(Math.round(raw), v.min, v.max);
            if (rounded != (Integer) v.value.get()) {
                set(v, rounded);
            }
        }
    }

    // ----------------------------------------------------------------
    //  Редактор цвета
    // ----------------------------------------------------------------

    private void openColor(View v) {
        int sy = top + v.y - (int) scroll;
        editX = v.wx;
        editY = sy + 2;
        editW = v.ww;
        editH = v.h - 4;
        colorEdit.x = editX;
        colorEdit.y = editY;
        colorEdit.setValue(String.format(Locale.ROOT, "%08X", (Integer) v.value.get()));
        colorEdit.setTextColor(0xFFFFFFFF);
        colorEdit.setCursorPosition(8);
        colorEdit.setEditable(true);
        colorEdit.visible = true;
        setInitialFocus(colorEdit);
        colorView = v;
    }

    private void commitColor() {
        if (colorEdit == null || colorView == null) {
            return;
        }
        String s = colorEdit.getValue().trim();
        if (s.length() == 6) {
            s = "FF" + s;
        }
        if (s.length() == 8) {
            try {
                int argb = (int) Long.parseLong(s, 16);
                set(colorView, argb);
                MenuCustomizationConfig.SPEC.save();
                cancelColor();
                return;
            } catch (NumberFormatException ignored) {
                // невалидный ввод — подсветим красным и останемся в поле
            }
        }
        colorEdit.setTextColor(0xFFFF6666);
    }

    private void cancelColor() {
        if (colorEdit != null) {
            colorEdit.visible = false;
            colorEdit.x = 0;
            colorEdit.y = -100;
        }
        colorView = null;
        setFocused(null);
    }

    // ----------------------------------------------------------------
    //  Запись значений
    // ----------------------------------------------------------------

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void set(View v, Object value) {
        ((ForgeConfigSpec.ConfigValue) v.value).set(value);
    }

    private void resetAll() {
        cancelColor();
        dragView = null;
        for (View v : VIEWS) {
            if (!v.isHeader()) {
                set(v, v.value.getDefault());
            }
        }
        MenuCustomizationConfig.SPEC.save();
    }
}
