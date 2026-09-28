// Пример 1 — первый вход игрока.
// Файл: hollowengine/scripts/storyengine/examples/01_first_join.se.kts
// @file:EntryPoint обязателен: без него скрипт не запустится автоматически.
@file:EntryPoint

package storyengine.examples

import net.minecraft.world.phys.Vec3
import ru.hollowhorizon.hc.client.models.gltf.Transform
import ru.hollowhorizon.hc.client.models.gltf.animations.AnimationType
import ru.hollowhorizon.hollowengine.common.entities.NPCEntity
import ru.hollowhorizon.hollowengine.common.npcs.Attributes
import ru.hollowhorizon.hollowengine.common.npcs.NpcIcon
import ru.hollowhorizon.hollowengine.common.scripting.color
import ru.hollowhorizon.hollowengine.common.scripting.item
import ru.hollowhorizon.hollowengine.common.scripting.players
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.wait

// `team` в 1.6.2a нет. Список игроков, для которых запущен скрипт, — `players`.
val players by server.players

// NPC — это узел-делегат. Сам спавн происходит при загрузке скрипта в машину состояний.
val npc by NPCEntity.creating {
    name = "Боб"
    model = "hollowengine:models/entity/player_model.gltf"
    world = "minecraft:overworld"
    pos = pos(0.5, 64.0, 0.5)
    rotation = vec(0.0, 180.0)
    showName = true
    switchHeadRot = true
    size = 0.6f to 2.0f
    transform = Transform(tX = 1.5f)
    attributes = Attributes(
        "minecraft:generic.max_health" to 20f,
        "minecraft:generic.movement_speed" to 0.3f,
    )
    animations[AnimationType.IDLE] = "Idle"
}

// Иконка над NPC — companion-свойства, не enum.
npc.icon = NpcIcon.DIALOGUE

// Обычные узлы: реплика NPC, выдача предмета, пауза.
npc.say { "Привет, путешественник!" }
npc.giveRightHand { item("minecraft:compass", 1) }

wait { 1.sec }

// `moveTo`/`lookAlwaysAt` — reified-функции: тип цели задаётся явно.
npc.lookAlwaysAt<Vec3> { pos(0.5, 65.0, 0.5) }

// Узлы игроков пишутся БЕЗ префикса `player`.
// Имена вроде `playerSend` — это JVM-имена (`@JvmName`), в скриптах они не работают.
players.send { "Боб вас встретил." }
players.say { "Боб: добро пожаловать!" }

wait { 2.sec }

// moveTo выполняется «здесь и сейчас» и блокирует сюжет до прибытия.
npc.moveTo<Vec3> { pos(8.0, 64.0, 8.0) }

// Ссылка на конкретного игрока: team[nick] в документации == players[nick] здесь.
val firstPlayerPos: Vec3 = players.invoke().first().position()

players.sendComponent {
    "Боб подошёл к тебе на ${firstPlayerPos.x} блоков".color(0x55FF55)
}
