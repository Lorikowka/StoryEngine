// Пример 4 — катсцена: камера, затемнение, звук, частицы, управление временем.
// Показывает: camera/shake, fadeIn/fadeOut, playSound, effekseer, pauseTime, async, script control.
@file:EntryPoint

package storyengine.examples

import ru.hollowhorizon.hc.client.utils.math.Interpolation
import ru.hollowhorizon.hollowengine.common.entities.NPCEntity
import ru.hollowhorizon.hollowengine.common.scripting.players
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.fadeIn
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.fadeOut
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.next
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.playSound
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.restartScript
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.stopSound
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.wait
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.camera.camera
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.camera.shake
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.particles.effekseer
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.world.pauseTime
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.world.resumeTime

val players by server.players

val npc by NPCEntity.creating {
    name = "Наставник"
    model = "hollowengine:models/entity/player_model.gltf"
    world = "minecraft:overworld"
    pos = pos(0.5, 64.0, 0.5)
    showName = true
}

// Плавное появление, затемнение мира на время сцены.
+fadeIn { text = "Глава 1"; subtitle = "Пробуждение"; time = 40; color = 0x000000 }

// Ставим время мира на паузу, чтобы сцена не зависела от суток.
pauseTime()

// Камера на статичной точке, смотрит в заданную позицию.
+camera {
    static(40, pos(0.0, 66.0, 6.0), pos(0.5, 65.0, 0.5))
}

// Плавный проход по сплайну. `path` — это ИМЯ кривой (String), а не список точек,
// иначе получится ошибка «actual type is List<Vec3>, but String was expected».
+camera {
    spline(
        60,
        "my_scene_path",
        Interpolation.SINE_IN_OUT,
        true,
        Interpolation.SINE_IN_OUT,
        Interpolation.SINE_IN_OUT,
    )
}

// Камера, следящая за сущностью.
+camera {
    entity(40, pos(0.0, 65.0, 4.0)) { npc.invoke() }
    update()
}

npc.say { "Ты очень долго спал." }

// Тряска камеры + звук. У ScreenShakePacket нет поля `intensity` — есть intensity1/2/3.
shake {
    duration = 20
    intensity1 = 0.4f
    intensity2 = 0.2f
    intensity3 = 0.1f
}
+playSound { sound = "hollowengine:effects/portal"; volume = 1.0f; pitch = 1.0f; pos = pos(0.5, 65.0, 0.5) }

// Частицы Effekseer: дым вокруг NPC.
+effekseer {
    world = "minecraft:overworld"
    path = "hollowengine:particles/smoke"
    pos = pos(0.5, 64.5, 0.5)
    scale(1.5f, 1.5f, 1.5f)
    rotation(0.0f, 90.0f, 0.0f)
}

// Параллельный поток: пока идёт сцена, гасим звук через 2 секунды.
val bg = async {
    wait { 2.sec }
    +stopSound { "hollowengine:effects/portal" }
}

wait { 3.sec }

// `+next { }` перескакивает к следующему узлу сюжета, не дожидаясь конца параллельного потока.
+next { }

wait { 1.sec }

// Возвращаем время и гасим экран.
resumeTime()
+fadeOut { text = "Конец главы" }

// `restartScript` перезапускает сюжет с начала (нужен другой файл-скрипт).
// +restartScript()
