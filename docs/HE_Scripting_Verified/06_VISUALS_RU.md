# 06 — Камера, звук, затемнение, частицы, время

Версия: **HollowEngine 1.19.2-1.6.2a**. Все фрагменты скомпилированы.

## Затемнение экрана

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.fadeIn
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.fadeInOut
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.fadeOut

+fadeIn { text = "Глава 1"; subtitle = "Пролог"; time = 40; color = 0xFFFFFF }
+fadeOut { text = "Конец" }
fadeInOut { text = "Загрузка" }
```

`FadeContainer`: `text` (String), `subtitle` (String), `time` (Int, тики),
`color` (Int, ARGB). `fadeIn`/`fadeOut` возвращают узлы → `+`.
`fadeInOut` регистрирует себя сам → `+` не нужен.

## Звук

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.playSound
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.stopSound

+playSound {
    sound = "hollowengine:effects/bell"
    volume = 1.0f
    pitch = 1.0f
    pos = pos(0.0, 0.0, 0.0)
}
+stopSound { "hollowengine:effects/bell" }
```

`SoundContainer`: `sound` (ResourceLocation-строка), `volume` (`Float`),
`pitch` (`Float`), `pos` (`Vec3`).

## Камера

```kotlin
import ru.hollowhorizon.hc.client.utils.math.Interpolation
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.camera.camera
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.camera.shake

// Статичная камера: позиция + точка взгляда
+camera {
    static(40, pos(0.0, 66.0, 6.0), pos(0.5, 65.0, 0.5))
}

// Следование за сущностью
+camera {
    entity(40, pos(0.0, 65.0, 4.0)) { npc.invoke() }
    update()
    val end = isEnd
}

// Движение по сплайну
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
```

### Методы `CameraContainer`

| Метод                                  | Сигнатура                                                        |
|----------------------------------------|------------------------------------------------------------------|
| `static`                               | `(time: Int, pos: Vec3, rotation: Vec3)`                          |
| `entity`                               | `(time: Int, pos: Vec3, entity: Entity)` или `(time, pos, () -> Entity)` |
| `spline`                               | `(time: Int, path: String, interpolation, enableBorders, borderInterpolation, rotationInterpolation)` |
| `update()`                             | Применить накопленное движение                                    |
| `isEnd`                                | `Boolean` — дошли ли до конца                                    |

> `spline` принимает **`path` как `String`** (имя кривой), а не список точек.
> `spline(60, listOf(pos(...)), …)` → `argument type mismatch: actual type is
> List<Vec3>, but String was expected`.
>
> `static(...)` тоже ждёт `Vec3`, а не `Vec2`, вопреки имени параметра
> `rotation` в некоторых версиях.

`Interpolation` — enum из `hc` (30 значений): `LINEAR`, `SINE_IN/OUT/IN_OUT`,
`QUAD_*`, `CUBIC_*`, `QUART_*`, `QUINT_*`, `EXPO_*`, `CIRC_*`, `BACK_*`,
`ELASTIC_*`, `BOUNCE_*`.

`camera` возвращает узел и сам себя не регистрирует → `+`.

### Тряска камеры

```kotlin
shake {
    duration = 20
    intensity1 = 0.4f
    intensity2 = 0.2f
    intensity3 = 0.1f
}
```

Блок `shake` — это `ScreenShakePacket`. Полей: `duration` (`Int`),
`intensity1/2/3` (`Float`), `intensityStart`/`intensityEnd` (`Interpolation`),
`targets` (список `ShakeTarget`).

> Поля `intensity` **не существует**. `shake { intensity = 4.0f }` →
> `unresolved reference 'intensity'`.

## Частицы

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.particles.effekseer

+effekseer {
    world = "minecraft:overworld"
    path = "hollowengine:particles/smoke"
    pos = pos(0.0, 64.0, 0.0)
    scale(1.0f, 1.0f, 1.0f)
    rotation(0.0f, 90.0f, 0.0f)
    dynamic0 = 1.0f
}
```

В 1.6.2a частицы — это **Effekseer**, функция называется `effekseer`, а не
`particles`. Путь указывается без расширения `.efkefc` (оно отрезается при
загрузке). Также есть поля `entity` и `target` — привязка к сущности вместо
фиксированной позиции.

`effekseer` возвращает узел и сам себя не регистрирует → `+`.

Если мир не загружен, бросается
`IllegalStateException: Dimension <id> not found. Or not loaded!`.

## Управление временем мира

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.world.pauseTime
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.world.resumeTime

pauseTime()
resumeTime()
```

Обе функции регистрируют себя сами → `+` не нужен. `pauseTime` ставит
`is_time_paused` в NBT машины состояний, `resumeTime` снимает.

## GUI

```kotlin
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.events.gui

gui {
    runSingleCommandInGui("time set day")
    close()
}
```

Внутри receiver в `Widget`; `close()` и `runSingleCommandInGui(String)` —
extension-функции на `Widget`. `gui` возвращает `void`, поэтому `+` не нужен
(и недопустим).

## Ссылки

- `StoryEngine/examples/04_cutscene.se.kts` — всё вышеперечисленное вместе
- `StoryEngine/examples/06_choice_screen.se.kts` — GUI и ввод
- [07 — Грабли](07_GOTCHAS_RU.md)
