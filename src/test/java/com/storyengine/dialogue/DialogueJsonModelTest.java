package com.storyengine.dialogue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DialogueJsonModelTest {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Test
    void deserializesMeta() {
        String json = """
                {
                  "title": "Встреча со старостой",
                  "entry": "start",
                  "speaker": "Староста",
                  "icon": "old_man",
                  "portrait": "old_man_large"
                }
                """;

        DialogueMeta meta = gson.fromJson(json, DialogueMeta.class);
        assertNotNull(meta);
        assertEquals("Встреча со старостой", meta.getTitle());
        assertEquals("start", meta.getEntry());
        assertEquals("Староста", meta.getSpeaker());
        assertEquals("old_man", meta.getIcon());
        assertEquals("old_man_large", meta.getPortrait());
    }

    @Test
    void deserializesNodeWithResponses() {
        String json = """
                {
                  "text": "Здравствуй, путник.",
                  "responses": [
                    { "text": "Я ищу ключ.", "next": "knows" },
                    { "text": "§cУходи.", "command": "/say bye", "next": "angry" },
                    { "text": "[Убеждение] Помоги.", "if": "quest:village_in_danger:active",
                      "completeTask": "village_in_danger talk_to_elder", "next": "helpful" }
                  ]
                }
                """;

        DialogueNode node = gson.fromJson(json, DialogueNode.class);
        assertNotNull(node);
        assertEquals("Здравствуй, путник.", node.getText());
        assertEquals(3, node.getResponses().size());

        DialogueResponse r2 = node.getResponses().get(1);
        assertEquals("/say bye", r2.getCommand());
        assertEquals("angry", r2.getNext());

        DialogueResponse r3 = node.getResponses().get(2);
        assertEquals("quest:village_in_danger:active", r3.getCondition());
        assertEquals("village_in_danger talk_to_elder", r3.getCompleteTask());
    }

    @Test
    void deserializesGiveStringAndObject() {
        String json = """
                {
                  "text": "Возьми.",
                  "responses": [
                    { "text": "Спасибо", "give": "minecraft:diamond 5", "next": "x" },
                    { "text": "Ещё", "give": {"id":"minecraft:potion","count":1,"nbt":"{Potion:\\"minecraft:healing\\"}"}, "close": true }
                  ]
                }
                """;

        DialogueNode node = gson.fromJson(json, DialogueNode.class);
        DialogueResponse r1 = node.getResponses().get(0);
        assertTrue(r1.getGive() instanceof String);
        assertEquals("minecraft:diamond 5", r1.getGive());

        DialogueResponse r2 = node.getResponses().get(1);
        assertTrue(r2.getGive() instanceof java.util.Map);
        assertTrue(r2.shouldClose());
    }

    @Test
    void deserializesStorytell() {
        String json = """
                {
                  "text": "Прощай.",
                  "responses": [
                    { "text": "Пока",
                      "storytell": { "speaker": "Трактирщик", "icon": "bartender",
                                     "message": {"text":"Береги себя, путник."} },
                      "close": true }
                  ]
                }
                """;

        DialogueNode node = gson.fromJson(json, DialogueNode.class);
        DialogueResponse.Storytell st = node.getResponses().get(0).getStorytell();
        assertNotNull(st);
        assertEquals("Трактирщик", st.getSpeaker());
        assertEquals("bartender", st.getIcon());
        assertNotNull(st.getMessage());
    }

    @Test
    void deserializesV4NodeWithoutIcons() {
        // v4 (без иконок): узел описывается только text/speaker/responses,
        // поля icon/portrait отсутствуют и опционально игнорируются.
        String json = """
                {
                  "text": "Дорога на север перекрыта.",
                  "speaker": "Староста",
                  "responses": [
                    { "text": "Понял.", "next": "helpful" },
                    { "text": "Уйду.", "close": true }
                  ]
                }
                """;

        DialogueNode node = gson.fromJson(json, DialogueNode.class);
        assertNotNull(node);
        assertEquals("Дорога на север перекрыта.", node.getText());
        assertEquals("Староста", node.getSpeakerOverride());
        assertEquals(2, node.getResponses().size());
        // icon/portrait в v4 не задаются — поля пустые (обратная совместимость)
        assertTrue(node.getIconOverride() == null || node.getIconOverride().isEmpty());
        assertTrue(node.getPortraitOverride() == null || node.getPortraitOverride().isEmpty());
    }

    @Test
    void conditionParserHandlesAllPrefixes() {
        Optional<DialogueCondition> quest = DialogueConditionParser.parse("quest:find_key:active");
        assertTrue(quest.isPresent());
        assertTrue(quest.get() instanceof QuestStatusCondition);

        Optional<DialogueCondition> item = DialogueConditionParser.parse("item:minecraft:emerald 3");
        assertTrue(item.isPresent());
        assertTrue(item.get() instanceof ItemCondition);

        Optional<DialogueCondition> task = DialogueConditionParser.parse("task:find_key reach_mill");
        assertTrue(task.isPresent());
        assertTrue(task.get() instanceof TaskCondition);

        Optional<DialogueCondition> flag = DialogueConditionParser.parse("flag:met_elder");
        assertTrue(flag.isPresent());
        assertTrue(flag.get() instanceof FlagCondition);

        Optional<DialogueCondition> not = DialogueConditionParser.parse("not:quest:bandit_path:completed");
        assertTrue(not.isPresent());
        assertTrue(not.get() instanceof NotCondition);

        // пустая/некорректная строка -> empty (ответ тогда недоступен)
        assertTrue(DialogueConditionParser.parse("").isEmpty());
        assertTrue(DialogueConditionParser.parse("bogus:foo").isEmpty());
    }

    @Test
    void statusMapping() {
        assertEquals(com.storyengine.quest.QuestStatus.ACTIVE, DialogueConditionParser.statusOf("active"));
        assertEquals(com.storyengine.quest.QuestStatus.NOT_STARTED, DialogueConditionParser.statusOf("not_started"));
        assertNull(DialogueConditionParser.statusOf("nonsense"));
    }
}
