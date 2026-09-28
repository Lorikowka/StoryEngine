// Пример 2 — NPC и диалог с вариантами ответа.
// Показывает: say/sayComponent, lookAt, setPose, диалоговое окно, ручной `choices`.
@file:EntryPoint

package storyengine.examples

import ru.hollowhorizon.hc.client.models.gltf.animations.AnimationType
import ru.hollowhorizon.hollowengine.common.entities.NPCEntity
import ru.hollowhorizon.hollowengine.common.npcs.NpcIcon
import ru.hollowhorizon.hollowengine.common.scripting.color
import ru.hollowhorizon.hollowengine.common.scripting.players
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.wait
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.dialogues.ChoiceAction
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.dialogues.choices
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.dialogues.dialogue

val players by server.players

val npc by NPCEntity.creating {
    name = "Торговец Виллем"
    model = "hollowengine:models/entity/villager.gltf"
    world = "minecraft:overworld"
    pos = pos(0.5, 64.0, 0.5)
    rotation = vec(0.0, 180.0)
    showName = true
    animations[AnimationType.IDLE] = "Idle"
}

npc.icon = NpcIcon.QUESTION

npc.sayComponent { "Добро пожаловать в мою лавку!".color(0xFFD700) }

// Один реплика-узел, потом пауза, чтобы игрок успел прочитать.
wait { 2.sec }

// Смотрит на первого игрока и играет анимацию приветствия.
npc.lookAlwaysAt<net.minecraft.server.level.ServerPlayer> { players.invoke().first() }
npc.playOnce { "Wave" }

// Диалоговое окно с кнопками. `choice(...)` ждёт выбора игрока.
dialogue {
    dialogueNpc = npc
    options {
        name = "Виллем".color(0xFFD700)
        text = "Что тебе нужно?".color(0xFFFFFF)
    }
    choice(ChoiceAction.SCREEN) {
        timeout = 200
        addChoice("Купить зелье".color(0x00FF00)) { npc.say { "Зелье здоровья, три алмаза." } }
        addChoice("Уйти".color(0xFF5555)) { npc.say { "Заходи ещё." } }
        onTimeout { npc.say { "Молчишь? Ладно." } }
    }
}

wait { 1.sec }

// Ручное окно выбора без NPC — просто кнопки на экране.
// В `choices` поле `text` — обычный String, а не Component (в отличие от `options` в dialogue).
choices {
    text = "Выбери награду"
    background = "hollowengine:textures/gui/choices.png"
    buttonX = 20
    buttonY = 60
    "Зелье".invoke { players.send { "Получено зелье." } }
    "Меч".invoke { players.send { "Получен меч." } }
}
