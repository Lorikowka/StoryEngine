package com.storyengine.dialogue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционная проверка демо-диалога v4_demo, лежащего в
 * src/test/resources/dialogues/v4_demo. Гарантирует, что реальные
 * файлы контента парсятся моделью и содержат заявленные возможности
 * (условия, команды, выдача, флаги, опыт, storytell, close).
 *
 * <p>Фикстуры держатся в ресурсах теста, а не в run/config: папка run
 * в .gitignore, поэтому чтение оттуда ломало сборку на любом свежем
 * клоне и в CI.
 */
class DialogueDemoIntegrationTest {

    private static final String BASE = "/dialogues/v4_demo/";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private String json(String name) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(BASE + name + ".json")) {
            assertNotNull(is, "Нет фикстуры в тестовых ресурсах: " + BASE + name + ".json");
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                StringBuilder sb = new StringBuilder();
                char[] buf = new char[4096];
                int n;
                while ((n = reader.read(buf)) > 0) {
                    sb.append(buf, 0, n);
                }
                return sb.toString();
            }
        }
    }

    private DialogueNode node(String id) throws Exception {
        return gson.fromJson(json(id), DialogueNode.class);
    }

    @Test
    void demoMetaParses() throws Exception {
        DialogueMeta meta = gson.fromJson(json("_meta"), DialogueMeta.class);
        assertEquals("start", meta.getEntry());
        assertEquals("Староста", meta.getSpeaker());
    }

    @Test
    void startNodeCoversConditionAndCommand() throws Exception {
        DialogueNode start = node("start");
        assertEquals("Здравствуй, путник. Что привело тебя в наши края?", start.getText());

        DialogueResponse cond = start.getResponses().get(2);
        assertEquals("quest:village_in_danger:active", cond.getCondition());
        assertEquals("village_in_danger talk_to_elder", cond.getCompleteTask());
        assertEquals("helpful", cond.getNext());

        DialogueResponse rude = start.getResponses().get(1);
        assertTrue(rude.getCommand().contains("/say"));
        assertEquals("angry", rude.getNext());
    }

    @Test
    void knowsNodeGrantsFlagAndXp() throws Exception {
        DialogueNode knows = node("knows");
        DialogueResponse reward = knows.getResponses().get(0);
        assertEquals("met_elder true", reward.getSetFlag());
        assertNotNull(reward.getXp());
        assertEquals(10, reward.getXp());
        assertEquals("helpful", reward.getNext());
    }

    @Test
    void helpfulNodeGivesItemAndStorytellThenCloses() throws Exception {
        DialogueNode helpful = node("helpful");
        DialogueResponse reward = helpful.getResponses().get(0);
        assertEquals("minecraft:iron_sword 1", reward.getGive());
        assertEquals("elder_trust true", reward.getSetFlag());
        assertNotNull(reward.getStorytell());
        assertEquals("Староста", reward.getStorytell().getSpeaker());
        assertNotNull(reward.getStorytell().getMessage());
        assertTrue(reward.shouldClose());
    }

    @Test
    void angryNodeCloses() throws Exception {
        DialogueNode angry = node("angry");
        assertTrue(angry.getResponses().get(0).shouldClose());
    }
}
