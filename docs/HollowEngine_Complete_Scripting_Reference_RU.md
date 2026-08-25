# HollowEngine — Complete Scripting Reference
## Полный русскоязычный обзор документации по скриптингу

> Версия документа: 1.0  
> Источник: HollowEngine Wiki  
> Основной сайт: https://hollowengine-docs.readthedocs.io/en/latest/  
> Репозиторий: https://github.com/HollowHorizon/HollowEngine
>
> Этот документ объединяет структуру и доступные материалы разделов `Scripting` HollowEngine в единый справочник. Он не является дословной копией сайта: материал переформулирован и организован как техническая документация.

---

# 0. Что такое HollowEngine

HollowEngine — движок для создания контента Minecraft, ориентированный в том числе на сюжетные карты.

Согласно документации, через него можно:

- редактировать ванильные рецепты;
- создавать NPC;
- создавать катсцены;
- создавать диалоги;
- взаимодействовать с игровым миром;
- работать с группами игроков;
- писать небольшие игровые расширения;
- добавлять предметы, блоки, мобов и измерения через более продвинутый API.

Основной скриптовый язык — Kotlin Script.

Документация отдельно рекомендует изучить основы Kotlin до работы с движком.

---

# 1. Структура документации

Раздел `Scripting` документации организован следующим образом:

```text
Scripting
├── Введение
│
├── Сюжетные события
│   ├── Сюжетные события
│   │
│   ├── Действия
│   │   ├── Таймеры и ожидания
│   │   ├── Условия и ветвления
│   │   ├── Циклы
│   │   └── Асинхронные события
│   │
│   ├── Персонажи
│   │   ├── Создание персонажа
│   │   ├── Настройка персонажа
│   │   ├── Настройка модели персонажа
│   │   ├── Перемещение и направление взгляда
│   │   ├── Возможности персонажей
│   │   └── Анимирование персонажей
│   │
│   ├── Игроки
│   │   ├── Изменение моделей
│   │   └── Анимации игроков
│   │
│   ├── Мир
│   │   └── Время
│   │
│   ├── Окно диалога
│   │   └── Базовые возможности
│   │
│   ├── Катсцены
│   │   ├── Камера
│   │   └── Переходы
│   │
│   ├── Список событий
│   │   └── Список событий
│   │
│   └── Эффекты
│       └── Частицы
│
└── Общее
    ├── Интерполяции
    └── Импорт скриптов
```

Также корневая Wiki содержит отдельные разделы установки, загрузчика ресурсов и измерения рассказчика.

---

# 2. Введение в скриптовый движок

HollowEngine использует Kotlin, модифицированный для запуска непосредственно внутри игры.

Главное отличие от обычной программы Kotlin:

```kotlin
println("Hello")
```

можно писать непосредственно в `.kts`, без обязательного:

```kotlin
fun main() {
    ...
}
```

При этом полноценный Kotlin не запрещён.

Можно создавать:

- функции;
- классы;
- вложенные классы;
- объекты;
- обычные Kotlin-конструкции.

---

# 3. Типы скриптов

HollowEngine выделяет три типа:

| Тип | Расширение | Назначение |
|---|---|---|
| Mod | `.mod.kts` | Выполняется во время загрузки игры |
| Story Event | `.se.kts` | Сюжетное событие |
| Content | `.content.kts` | Контент, в частности рецепты |

Примеры:

```text
initialization.mod.kts
intro.se.kts
chapter_01.se.kts
recipe.content.kts
```

---

# 4. Расположение скриптов

Корневая папка:

```text
.minecraft/hollowengine/scripts/
```

Можно создавать подпапки:

```text
scripts/
├── chapters/
│   ├── chapter_01/
│   ├── chapter_02/
│   └── chapter_03/
│
├── characters/
│
├── recipes/
│
└── initialization.mod.kts
```

Формат имени:

```text
<название>.<тип>.kts
```

Рекомендации:

```text
правильно:
chapter_01.se.kts
npc_intro.se.kts
custom_recipe.content.kts

нежелательно:
My Story.se.kts
Chapter 1.se.kts
TEST.kts
```

---

# 5. Package

Чтобы избежать конфликтов имён, можно использовать Kotlin `package`.

Если файл находится:

```text
scripts/ru/hollowhorizon/my_super_modpack/intro.se.kts
```

в начале файла:

```kotlin
package ru.hollowhorizon.my_super_modpack
```

Правило:

```text
/
```

заменяется на:

```text
.
```

`package` указывается относительно папки:

```text
scripts/
```

---

# 6. Компиляция

При запуске `.kts` происходит компиляция.

Первичная компиляция может занимать от нескольких секунд до нескольких минут.

На неё влияют:

- размер скрипта;
- количество импортов;
- библиотеки;
- производительность компьютера.

После успешной компиляции скрипт кэшируется в JAR.

Последующие запуски поэтому выполняются значительно быстрее.

Если исходный `.kts` изменён, он компилируется заново.

Если скрипт не запускается несколько минут и ошибок нет, документация рекомендует проверить установленную версию HollowCore. Для полноценной разработки требуется полная версия, а не Lite.

---

# 7. IDE

Для написания скриптов можно использовать:

- IntelliJ IDEA;
- VS Code;
- другие редакторы с поддержкой Kotlin.

Однако обычный редактор не знает весь API HollowEngine.

Для полноценного автодополнения документация предлагает работать с исходниками HollowEngine.

Общая схема:

```text
клонировать HollowEngine
        ↓
открыть проект в IntelliJ IDEA
        ↓
дождаться индексации
        ↓
Gradle
        ↓
genIntellijRuns
        ↓
build → jar
        ↓
запустить dev-инстанс
```

Скрипты в таком окружении находятся:

```text
run/hollowengine/scripts/
```

---

# 8. Story Event

Story Event — главный механизм сюжетного скриптинга.

Через него создаются:

- сцены;
- NPC;
- диалоги;
- катсцены;
- выборы;
- ветвления;
- ожидания;
- циклы;
- последовательности действий.

Файл:

```text
something.se.kts
```

---

# 9. Главная концепция Story Event — граф узлов

Story Event нельзя воспринимать как обычную программу, выполняемую строка за строкой.

Сценарий превращается в граф узлов.

Например:

```text
Создать NPC
     ↓
Переместить NPC
     ↓
Ждать
     ↓
Реплика
     ↓
Выбор
   ┌─┴─┐
  Да  Нет
   ↓   ↓
  ... ...
```

Каждое действие представляет собой узел.

Сложные узлы могут содержать другие узлы:

```text
If
├── True
│   ├── действие
│   └── действие
│
└── False
    └── действие
```

---

# 10. Две стадии Story Event

Story Event имеет две стадии.

## 10.1. Загрузка

На стадии загрузки:

- вызываются методы создания узлов;
- строится граф;
- создаётся машина состояний;
- подготавливаются данные;
- строится структура события.

Поэтому:

```kotlin
npc ...
```

не обязательно означает «NPC прямо сейчас сделал действие».

Часто это означает:

```text
создать Node
```

---

## 10.2. Исполнение

На второй стадии машина состояний обслуживает узлы.

Здесь реально происходят:

- движение;
- ожидание;
- диалог;
- изменение переменных;
- анимации;
- условия;
- циклы.

---

# 11. Почему обычный Kotlin if отличается

В обычном Kotlin:

```kotlin
if (condition) {
    ...
}
```

условие вычисляется немедленно.

В Story Event это может происходить на стадии построения графа.

Поэтому для сюжетной логики используется:

```kotlin
If({ condition }) {
    ...
} Else {
    ...
}
```

Условие проверяется при исполнении события.

---

# 12. Делегированные переменные

Story Event активно использует Kotlin delegates.

Пример:

```kotlin
var reputation by saveable { 0 }
```

Условно можно представить это как три операции:

```text
создать состояние
       ↓
прочитать состояние
       ↓
записать состояние
```

Получение:

```kotlin
npc say {
    "Твоя репутация: $reputation"
}
```

Изменение:

```kotlin
next {
    reputation++
}
```

---

# 13. next

`next` используется, когда обычный Kotlin-код необходимо выполнить именно во время исполнения сценария.

Например:

```kotlin
next {
    reputation++
}
```

Это важно, потому что без такого механизма код может выполниться при построении графа.

---

# 14. Запуск Story Event

Основная команда:

```text
/hollowengine start-script <цель> <файл>
```

Важный момент:

```text
цель = команда игроков
```

а не просто один игрок.

Таким образом Story Event концептуально рассчитан на команду.

---

# 15. EntryPoint

Если сценарий должен запускаться при первом входе игрока, используется:

```kotlin
@file:EntryPoint
```

Это сообщает движку, что скрипт является точкой входа для нового игрока.

---

# 16. Таймеры и ожидания

В Story Event ожидание должно быть представлено узлом сценария.

Временные значения HollowEngine используют удобный синтаксис:

```kotlin
10.sec
5.min
1.hour
20
```

Где:

```text
20 = 20 игровых тиков
20 тиков = 1 секунда
```

Время можно складывать:

```kotlin
1.hour + 15.min + 30.sec + 45
```

Это особенно полезно для:

- задержек;
- сцен;
- анимаций;
- катсцен;
- последовательных действий.

---

# 17. Условия

Вместо обычного `if`:

```kotlin
If({ reputation >= 10 }) {
    npc say { "Я тебе доверяю." }
} Else {
    npc say { "Я тебе не доверяю." }
}
```

Условие:

```text
любой Kotlin-код → Boolean
```

Например:

```kotlin
If({ player.health > 10 }) {
    ...
}
```

или:

```kotlin
If({ reputation >= 10 && hasQuest }) {
    ...
}
```

---

# 18. Циклы

В Story Event не рекомендуется использовать обычные:

```kotlin
while
for
```

для построения сюжетной последовательности.

Вместо `while` используется:

```kotlin
While({ condition }) {
    // действия
}
```

Например:

```kotlin
While({ reputation < 10 }) {
    // действия
}
```

Условие проверяется во время исполнения.

---

# 19. Бесконечный цикл

```kotlin
While({ true }) {
    // действия
}
```

Такой цикл никогда не завершится самостоятельно.

Если он находится в основном Story Event, то событие также не завершится.

Поэтому бесконечные циклы особенно удобно использовать вместе с `async`.

---

# 20. Асинхронные действия

Для параллельного выполнения используется:

```kotlin
async {
    // действие
}
```

Например:

```kotlin
async {
    npc playLooped {
        "idle"
    }
}
```

Основной сценарий продолжит выполнение независимо от этого блока.

---

# 21. Управление async

Асинхронную задачу можно сохранить:

```kotlin
val task = async {
    While({ true }) {
        // ...
    }
}
```

Остановить:

```kotlin
task.stop()
```

Продолжить:

```kotlin
task.resume()
```

Это особенно полезно для:

- фоновых циклов;
- постоянных анимаций;
- наблюдения за состоянием;
- параллельных эффектов.

---

# 22. NPC

NPC — одна из центральных возможностей HollowEngine.

NPC поддерживают:

- собственные модели;
- текстуры;
- анимации;
- атрибуты;
- размеры;
- имена;
- перемещение;
- направление взгляда;
- предметы;
- взаимодействие с блоками;
- цели;
- атаки;
- диалоги.

---

# 23. Создание NPC

Базовый шаблон:

```kotlin
val npc by NPCEntity.creating {
    name = "Виталик"

    model = "hollowengine:models/entity/player_model.gltf"

    attributes = Attributes(
        "minecraft:generic.max_health" to 100f
    )

    size = 0.6f to 0.8f

    showName = true

    transform = Transform(
        tX = 1.5f
    )

    animations[AnimationType.IDLE] = "<newAnimationName>"

    world = "minecraft:overworld"

    pos = pos(x, y, z)
}
```

---

# 24. Поля NPC

## name

Имя NPC:

```kotlin
name = "Виталик"
```

## model

ResourceLocation модели:

```kotlin
model = "hollowengine:models/entity/player_model.gltf"
```

## attributes

Атрибуты Minecraft:

```kotlin
attributes = Attributes(
    "minecraft:generic.max_health" to 100f
)
```

## size

Размер хитбокса:

```kotlin
size = 0.6f to 0.8f
```

## showName

Отображение имени:

```kotlin
showName = true
```

## transform

Начальная трансформация:

```kotlin
transform = Transform(
    tX = 1.5f
)
```

## world

Измерение:

```kotlin
world = "minecraft:overworld"
```

## pos

Позиция:

```kotlin
pos = pos(x, y, z)
```

---

# 25. Повторное использование NPC

При выполнении создания движок может найти уже существующего NPC.

Если соответствующий NPC уже существует, он может быть использован вместо создания нового.

---

# 26. Случайный спавн

Получить случайную позицию рядом с игроком команды:

```kotlin
pos = team.randomPos()
```

Радиус:

```kotlin
pos = team.randomPos(25f)
```

---

# 27. Атрибуты NPC

Можно использовать стандартные Minecraft-атрибуты:

```kotlin
Attributes(
    "minecraft:generic.max_health" to 100f
)
```

Проверить доступные атрибуты можно через Minecraft:

```text
/attribute
```

---

# 28. Удаление NPC

```kotlin
npc.despawn()
```

---

# 29. Настройка NPC после создания

NPC можно перенастраивать во время сценария:

```kotlin
npc configure {
    model = "hollowengine:models/entity/player_model.gltf"

    animations[AnimationType.IDLE] = "animationName"

    textures["original_name"] =
        "hollowengine:textures/skins/my_skin.png"

    transform = Transform(
        tX = 1.5f
    )
}
```

Можно менять:

- модель;
- текстуры;
- стандартные анимации;
- трансформацию.

---

# 30. ResourceLocation

Пути ресурсов имеют формат:

```text
namespace:path
```

Пример:

```text
hollowengine:models/entity/player_model.gltf
```

Текстура:

```text
hollowengine:textures/skins/my_skin.png
```

---

# 31. Модель NPC

Модели могут использовать GLTF и соответствующие системы HollowEngine.

В Blockbench/Blender модель должна иметь корректные кости/узлы.

Для предметов:

```text
RightHandItem
LeftHandItem
```

`RightHandItem` — правая рука.

`LeftHandItem` — левая рука.

Эти кости также могут использоваться в анимациях.

---

# 32. Анимации NPC

Простое однократное воспроизведение:

```kotlin
npc playOnce {
    "animationName"
}
```

Зацикливание:

```kotlin
npc playLooped {
    "animationName"
}
```

Остановка:

```kotlin
npc stop {
    "animationName"
}
```

---

# 33. Детальная настройка анимации

```kotlin
npc play {
    animation = "animationName"

    layerMode = LayerMode.ADD

    playType = PlayMode.ONCE

    speed = 1f
}
```

---

# 34. LayerMode

```text
ADD
OVERWRITE
```

`ADD`:

```text
добавить анимацию к текущим
```

`OVERWRITE`:

```text
перезаписать другие анимации
```

---

# 35. PlayMode

Основные варианты:

```text
ONCE
LOOPED
LAST_FRAME
REVERSED
```

### ONCE

Проиграть один раз.

### LOOPED

Повторять.

### LAST_FRAME

Остановиться на последнем кадре.

### REVERSED

Циклически проигрывать в прямом и обратном направлении.

---

# 36. Скорость анимации

```kotlin
speed = 1f
```

Например:

```kotlin
speed = 2f
```

ускоряет воспроизведение.

---

# 37. Получение списка анимаций

Используется команда:

```text
/hollowengine model <путь к модели>
```

Команда помогает посмотреть доступные анимации модели.

---

# 38. Действия NPC с предметами

Сообщение:

```kotlin
npc say {
    "Привет!"
}
```

---

## Бросить предмет

```kotlin
npc dropItem {
    item("minecraft:apple")
}
```

Для получения данных предмета можно:

1. взять предмет в основную руку;
2. выполнить:

```text
/hollowengine hand
```

---

# 39. Предмет в правой руке

```kotlin
npc giveRightHand {
    item("minecraft:apple")
}
```

---

# 40. Предмет в левой руке

```kotlin
npc giveLeftHand {
    item("minecraft:apple")
}
```

---

# 41. Запрос предметов

NPC может требовать предметы у игрока:

```kotlin
npc requestItems {
    text = "Принеси мне эти предметы:"
    +item("minecraft:apple")
}
```

---

# 42. Использование блока

NPC может взаимодействовать с блоком:

```kotlin
npc useBlock {
    pos(x, y, z)
}
```

Позицию блока, на который смотрит игрок, можно получить:

```text
/hollowengine pos
```

---

# 43. Цели NPC

Установить сущность:

```kotlin
npc setTarget {
    entity
}
```

Другого NPC:

```kotlin
npc1 setTarget npc2
```

Целую команду:

```kotlin
npc setTargetTeam {
    team
}
```

Сброс:

```kotlin
npc setTarget {
    null
}
```

---

# 44. Перемещение NPC

Перемещение NPC является отдельной частью API персонажей.

Концептуально оно используется для сценариев вида:

```text
NPC появился
      ↓
NPC идёт к точке
      ↓
ожидание окончания движения
      ↓
NPC говорит
```

Для сложных сцен движение следует комбинировать с:

- ожиданием;
- направлением взгляда;
- анимацией;
- интерполяцией.

---

# 45. Направление взгляда NPC

NPC может быть ориентирован на:

- координату;
- другого NPC;
- сущность;
- игрока.

Это позволяет строить сцены:

```text
NPC A смотрит на NPC B
NPC B смотрит на NPC A
```

и:

```text
NPC смотрит на игрока
```

Особенно важно для диалогов и катсцен.

---

# 46. Игроки

Story Event работает с командами игроков.

Переменная:

```kotlin
team
```

представляет группу, для которой выполняется событие.

Концептуально:

```text
Story Event
     ↓
Team
 ┌───┼───┐
P1  P2   P3
```

---

# 47. Получение конкретного игрока

```kotlin
val player by team[{ "Ник Игрока" }]
```

После этого используется переменная:

```kotlin
player
```

---

# 48. Изменение модели игрока

```kotlin
player configure {
    model = "hollowengine:models/entity/player_model.gltf"

    animations[AnimationType.IDLE] =
        "animationName"

    textures["original_name"] =
        "hollowengine:textures/skins/my_skin.png"

    transform = Transform(
        tX = 1.5f
    )
}
```

---

# 49. Возврат стандартной модели

```kotlin
model = "%NO_MODEL%"
```

Это возвращает стандартную модель игрока.

---

# 50. Анимации игроков

Однократная:

```kotlin
player playOnce {
    "animationName"
}
```

Зацикленная:

```kotlin
player playLooped {
    "animationName"
}
```

Остановка:

```kotlin
player stop {
    "animationName"
}
```

Детальная:

```kotlin
player play {
    animation = "animationName"
    layerMode = LayerMode.ADD
    playType = PlayMode.ONCE
    speed = 1f
}
```

Используются те же:

```text
LayerMode
PlayMode
```

что и для NPC.

---

# 51. Время мира

HollowEngine предоставляет действия для управления временем.

Остановить:

```kotlin
pauseTime()
```

Продолжить:

```kotlin
resumeTime()
```

Установить время:

```kotlin
execute {
    "time set <время>"
}
```

Например:

```kotlin
execute {
    "time set day"
}
```

---

# 52. Диалоги

Диалог создаётся через:

```kotlin
dialogue {
    // действия
}
```

NPC, участвующего в диалоге, нужно создать до входа в `dialogue`.

---

# 53. Реплика NPC

```kotlin
dialogue {
    npc say {
        "Привет!"
    }
}
```

Внутри `dialogue` действие `say` становится репликой диалогового интерфейса, а не обычным сообщением в чат.

---

# 54. Реплика команды игроков

```kotlin
dialogue {
    team send {
        "Привет!"
    }
}
```

Она также отображается в окне диалога.

---

# 55. Авторский текст

Можно вручную задать имя и текст:

```kotlin
dialogue {
    send {
        name = "Неизвестный"
        text = "Привет, мы раньше не встречались?"
    }
}
```

Это позволяет создавать:

- narration;
- авторские комментарии;
- системные реплики;
- сообщения неизвестного персонажа.

---

# 56. Выборы

Выборы существуют внутри:

```kotlin
dialogue {
    ...
}
```

Пример:

```kotlin
choice {
    "Ваш вариант 1" {
        // действия
    }

    "Ваш вариант 2" {
        // действия
    }
}
```

Можно создавать несколько вариантов:

```kotlin
choice {
    "Да" {
        ...
    }

    "Нет" {
        ...
    }

    "Не знаю" {
        ...
    }
}
```

Диалог завершается, когда больше нет фраз и выборов.

---

# 57. Типичная структура диалога

```kotlin
dialogue {
    npc say {
        "Что ты выберешь?"
    }

    choice {
        "Помочь" {
            npc say {
                "Спасибо."
            }

            next {
                reputation++
            }
        }

        "Отказаться" {
            npc say {
                "Понятно."
            }
        }
    }
}
```

---

# 58. Катсцены

Катсцены строятся из обычных Story Event плюс:

- камеры;
- переходов;
- анимаций;
- NPC;
- эффектов;
- диалогов.

Типичная последовательность:

```text
Fade In
   ↓
позиционирование NPC
   ↓
камера
   ↓
анимация
   ↓
реплика
   ↓
движение камеры
   ↓
эффект
   ↓
Fade Out
```

---

# 59. Камера

В HollowEngine имеется специальный предмет:

```text
Камера
```

Он доступен во вкладке Creative.

---

# 60. Создание пути камеры

Управление камерой:

```text
ПКМ
```

добавляет точку.

```text
ЛКМ
```

удаляет ближайшую точку.

```text
+
```

и:

```text
-
```

поворачивают камеру.

```text
C
```

сбрасывает настройку.

---

# 61. Сохранение камеры

Используется:

```text
Shift + ПКМ
```

После сохранения файл появляется в:

```text
.minecraft/hollowengine/camera/
```

---

# 62. Запуск камеры

```kotlin
camera {
    time = 10.sec
    path = "123456.nbt"
    interpolation = Interpolation.LINEAR
}
```

`time` — продолжительность пролёта.

`path` — файл камеры.

`interpolation` — кривая движения.

---

# 63. Переходы

Для затемнения/появления экрана:

```kotlin
fadeIn {
    text = "Большой текст"
    subtitle = "Текст поменьше"
    texture = "modid:textures/your_texture.png"
    color = 0xFFFFFF
    time = 10.sec
}
```

После этого можно выполнить действия «за экраном»:

```text
телепортация
перестановка NPC
изменение мира
смена сцены
```

Затем:

```kotlin
fadeOut {
    text = "Большой текст"
    subtitle = "Текст поменьше"
    texture = "modid:textures/your_texture.png"
    color = 0xFFFFFF
    time = 10.sec
}
```

---

# 64. Время переходов

Можно использовать:

```text
20
```

как 20 тиков.

```text
3.sec
```

как 3 секунды.

```text
5.min
```

как 5 минут.

```text
10.hour
```

как 10 часов.

Комбинация:

```kotlin
1.hour + 15.min + 30.sec + 45
```

---

# 65. Цвет перехода

Цвет задаётся HEX:

```kotlin
color = 0xFFFFFF
```

Важно:

```text
0x
```

и шесть шестнадцатеричных цифр.

---

# 66. Частицы

Создание пространства частиц:

```kotlin
particles {
    // настройки
}
```

Основные параметры:

```text
world
particle
settings
```

Пример:

```kotlin
particles {
    particle = "hc:star"

    settings {
        randomOffset(0.05, 0.05)
        randomMotion(0.05)

        lifetime = 100
        gravity = 0.1f

        spawn(x, y, z)
    }
}
```

---

# 67. Доступные частицы

Документация приводит:

```text
hc:star
hc:circle
```

и отмечает, что список может расширяться.

---

# 68. randomMotion

Случайное движение по трём осям:

```kotlin
randomMotion(
    xSpeed,
    ySpeed,
    zSpeed
)
```

Горизонталь + вертикаль:

```kotlin
randomMotion(
    horizontalSpeed,
    verticalSpeed
)
```

Общий вариант:

```kotlin
randomMotion(speed)
```

---

# 69. randomOffset

Случайное смещение:

```kotlin
randomOffset(
    x,
    y,
    z
)
```

Также существуют сокращённые формы для горизонтали/общего значения.

---

# 70. Вращение частиц

Два значения:

```kotlin
spin(
    startValue,
    endValue
)
```

Три:

```kotlin
spin(
    startValue,
    middleValue,
    endValue
)
```

Можно задать интерполяцию:

```kotlin
startToMiddleEasing = Interpolation.LINEAR
middleToEndEasing = Interpolation.LINEAR
```

---

# 71. Цвет частиц

```kotlin
color(
    r1,
    g1,
    b1,
    r2,
    g2,
    b2
)
```

Значения RGB задаются от:

```text
0f
```

до:

```text
1f
```

Можно задать:

```kotlin
colorCurveEasing = Interpolation.LINEAR
```

---

# 72. Прозрачность

```kotlin
transparency(
    startValue,
    endValue
)
```

или:

```kotlin
transparency(
    startValue,
    middleValue,
    endValue
)
```

Интерполяции задаются аналогично вращению.

Документация отмечает ограничение: изменение прозрачности к концу жизни частицы на момент написания страницы временно не работало.

---

# 73. Гравитация

```kotlin
gravity = 0.1f
```

---

# 74. No Clip

```kotlin
noClip = true
```

или:

```kotlin
noClip = false
```

Определяет, может ли частица проходить сквозь блоки.

---

# 75. SpritePicker

Тип выбора спрайта:

```kotlin
spritePicker = SpritePicker.FIRST_INDEX
```

Доступные варианты:

```text
FIRST_INDEX
LAST_INDEX
WITH_AGE
RANDOM_SPRITE
```

`WITH_AGE` меняет картинку в зависимости от возраста частицы.

---

# 76. Lifetime

```kotlin
lifetime = 100
```

Время жизни задаётся в тиках.

---

# 77. Spawn и Repeat

Одна частица:

```kotlin
spawn(x, y, z)
```

Несколько:

```kotlin
repeat(x, y, z, n)
```

где `n` — количество.

---

# 78. Интерполяции

Интерполяция управляет изменением скорости действия.

Вместо:

```text
A → B
```

получается:

```text
A ─── кривая движения ───→ B
```

---

# 79. Полный список интерполяций

```text
Interpolation.LINEAR

Interpolation.SINE_IN
Interpolation.SINE_OUT
Interpolation.SINE_IN_OUT

Interpolation.QUAD_IN
Interpolation.QUAD_OUT
Interpolation.QUAD_IN_OUT

Interpolation.CUBIC_IN
Interpolation.CUBIC_OUT
Interpolation.CUBIC_IN_OUT

Interpolation.QUART_IN
Interpolation.QUART_OUT
Interpolation.QUART_IN_OUT

Interpolation.QUINT_IN
Interpolation.QUINT_OUT
Interpolation.QUINT_IN_OUT

Interpolation.EXPO_IN
Interpolation.EXPO_OUT
Interpolation.EXPO_IN_OUT

Interpolation.CIRC_IN
Interpolation.CIRC_OUT
Interpolation.CIRC_IN_OUT

Interpolation.BACK_IN
Interpolation.BACK_OUT
Interpolation.BACK_IN_OUT

Interpolation.ELASTIC_IN
Interpolation.ELASTIC_OUT
Interpolation.ELASTIC_IN_OUT

Interpolation.BOUNCE_IN
Interpolation.BOUNCE_OUT
Interpolation.BOUNCE_IN_OUT
```

---

# 80. Суффиксы интерполяций

```text
_IN
```

характеризует начало движения.

```text
_OUT
```

характеризует конец движения.

```text
_IN_OUT
```

использует кривую на обеих сторонах.

---

# 81. Импорт скриптов

Для импорта другого скрипта:

```kotlin
@file:Import("../path/to/file.kts")
```

Путь относительный.

То есть он считается относительно текущего файла.

---

# 82. Важное ограничение Import

Переменные импортируемого скрипта обычно создаются заново.

Например:

```text
mod.kts
    ↓
создал объект
    ↓
se.kts импортировал mod.kts
```

не означает, что `se.kts` получает тот же экземпляр объекта.

Документация отдельно указывает исключение для `runtime`-переменных.

---

# 83. Сохранение состояния

Одна из ключевых особенностей Story Event — состояние узлов может сохраняться.

Это позволяет использовать сюжетные переменные:

```kotlin
var reputation by saveable { 0 }
```

для:

- репутации;
- выбора;
- флагов;
- состояния квестов;
- этапов истории.

---

# 84. Архитектурная модель Story Event

В упрощённом виде:

```text
Kotlin Script
      │
      ▼
Построение графа
      │
      ▼
State Machine
      │
      ├── Node
      ├── Node
      ├── If
      │    ├── True
      │    └── False
      ├── While
      ├── Dialogue
      └── Async
      │
      ▼
Исполнение
      │
      ▼
Minecraft World
```

---

# 85. Практический шаблон сюжетной сцены

```kotlin
package story.chapter01

var reputation by saveable { 0 }

val npc by NPCEntity.creating {
    name = "Незнакомец"

    model =
        "hollowengine:models/entity/player_model.gltf"

    showName = true

    world =
        "minecraft:overworld"

    pos =
        team.randomPos(10f)
}

npc say {
    "Эй!"
}

dialogue {

    npc say {
        "Мне нужна твоя помощь."
    }

    choice {

        "Помочь" {

            npc say {
                "Спасибо."
            }

            next {
                reputation++
            }
        }

        "Отказаться" {

            npc say {
                "Тогда уходи."
            }
        }
    }
}

If({ reputation > 0 }) {

    npc say {
        "Я запомню твою доброту."
    }
}
```

---

# 86. Полноценная архитектура сюжетной карты

Для большой карты разумно разделять код:

```text
scripts/
│
├── common/
│   ├── variables/
│   ├── characters/
│   └── utilities/
│
├── chapters/
│   ├── chapter_01/
│   │   ├── intro.se.kts
│   │   ├── village.se.kts
│   │   └── battle.se.kts
│   │
│   ├── chapter_02/
│   │   ├── investigation.se.kts
│   │   └── confrontation.se.kts
│   │
│   └── chapter_03/
│
├── effects/
│
├── recipes/
│
└── initialization.mod.kts
```

Это значительно удобнее одного огромного:

```text
story.se.kts
```

---

# 87. Как проектировать сцену

Практический порядок:

```text
1. Определить участников
        ↓
2. Создать NPC
        ↓
3. Расставить NPC
        ↓
4. Настроить модели
        ↓
5. Настроить анимации
        ↓
6. Настроить камеру
        ↓
7. Добавить диалог
        ↓
8. Добавить выборы
        ↓
9. Добавить условия
        ↓
10. Добавить эффекты
        ↓
11. Добавить сохранение состояния
```

---

# 88. Что особенно важно для разработчика StoryEngine

Из архитектуры HollowEngine следует несколько принципов.

## 88.1. Story Event — не обычный Kotlin

Это DSL над машиной состояний.

Поэтому:

```kotlin
npc say { ... }
```

следует концептуально читать как:

```text
создай узел "NPC Say"
```

а не:

```text
немедленно вызвать NPC.say()
```

---

## 88.2. Runtime и Build Stage разделены

Есть:

```text
Build Stage
```

и:

```text
Runtime Stage
```

Это фундаментальная особенность.

---

## 88.3. saveable — часть состояния

Переменная:

```kotlin
var reputation by saveable { 0 }
```

не просто значение в памяти.

Она является частью сохраняемого сценарного состояния.

---

## 88.4. async — параллельная ветка

```kotlin
async {
    ...
}
```

создаёт независимую выполняемую ветку.

---

## 88.5. If/While — узлы управления

```kotlin
If
While
```

являются не просто Kotlin-конструкциями.

Они создают элементы графа Story Event.

---

# 89. Взаимосвязь основных систем

```text
                   Story Event
                       │
       ┌───────────────┼────────────────┐
       │               │                │
      NPC            Player           World
       │               │                │
   ┌───┼───┐       ┌───┼───┐        ┌───┼───┐
   │   │   │       │   │   │        │   │   │
 Model Anim Items  Model Anim Team   Time Effects
   │   │   │       │   │            │
   └───┴───┘       └───┴────────────┘
       │
       ▼
    Dialogue
       │
       ▼
     Choice
       │
       ▼
    If / While
       │
       ▼
      Async
       │
       ▼
  State Machine
```

---

# 90. Команды HollowEngine, упомянутые в документации

Полезные команды:

```text
/hollowengine start-script <цель> <файл>
```

Запуск Story Event.

```text
/hollowengine model <модель>
```

Просмотр информации модели и доступных анимаций.

```text
/hollowengine hand
```

Получение данных предмета из основной руки.

```text
/hollowengine pos
```

Получение позиции блока, на который смотрит игрок.

---

# 91. Установка

Для классической версии документации указываются:

```text
HollowEngine
HollowCore
Kotlin For Forge
```

Для сюжетной работы дополнительно требовались:

```text
FTB Teams
FTB Library
Architectury
```

FTB Teams используется для концепции команд игроков.

---

# 92. Важное замечание о версиях

Официальная старая Wiki и актуальная разработка HollowEngine — не одно и то же.

В старой документации описывается окружение Minecraft 1.19.2 и HollowEngine 1.x.

Современная разработка HollowEngine уже значительно расширена.

Поэтому API необходимо проверять относительно версии движка, которую использует конкретный проект.

---

# 93. Современная архитектура HollowEngine

Современный репозиторий HollowEngine содержит значительно больше систем, чем старая Wiki.

Среди них:

- текстовый Kotlin scripting;
- визуальный block scripting;
- runtime-компиляция;
- animation system;
- GLTF/GLB/BBModel;
- ECS/Geary;
- Bedrock particle system;
- игровые события;
- внутриигровой IDE;
- различные команды `/hollowengine`.

Поэтому старую Wiki следует рассматривать как документацию конкретной версии API, а исходный репозиторий — как источник для проверки актуальной реализации.

---

# 94. Связь с Forge

Документация описывает HollowEngine как средство, позволяющее писать небольшие игровые расширения.

Продвинутые пользователи могут использовать возможности, сопоставимые с Forge-моддингом:

```text
Items
Blocks
Mobs
Dimensions
Events
Recipes
```

Но для сюжетной карты большая часть задач может быть реализована непосредственно через Story Events.

---

# 95. Как выбирать механизм

| Задача | Использовать |
|---|---|
| Реплика персонажа | `npc say` |
| Диалог | `dialogue` |
| Выбор игрока | `choice` |
| Сюжетная проверка | `If` |
| Повторение | `While` |
| Параллельная задача | `async` |
| NPC | `NPCEntity.creating` |
| Анимация | `playOnce` / `playLooped` / `play` |
| Модель | `configure` |
| Частицы | `particles` |
| Камера | `camera` |
| Затемнение | `fadeIn` / `fadeOut` |
| Состояние | `saveable` |
| Импорт | `@file:Import` |
| Время мира | `pauseTime` / `resumeTime` |
| Событие при входе | `@file:EntryPoint` |

---

# 96. Минимальный жизненный цикл сюжетного события

```text
Создать .se.kts
      ↓
Загрузить скрипт
      ↓
Построить узлы
      ↓
Создать State Machine
      ↓
Запустить событие
      ↓
Исполнять узлы
      ↓
Сохранять состояние
      ↓
Завершить событие
```

---

# 97. Чек-лист перед созданием большого Story Event

```text
- [ ] Определена команда игроков
- [ ] Создана структура папок
- [ ] Определён package
- [ ] NPC имеют понятные имена
- [ ] Модели находятся по корректным ResourceLocation
- [ ] Анимации проверены через /hollowengine model
- [ ] Диалог отделён от обычной логики
- [ ] Ветвления используют If/Else
- [ ] Циклы используют While
- [ ] Параллельные процессы используют async
- [ ] Сюжетные флаги используют saveable
- [ ] Долгие сцены разбиты на отдельные файлы
- [ ] Катсцены используют отдельные camera paths
- [ ] Визуальные эффекты вынесены в отдельные блоки/скрипты
```

---

# 98. Ключевая идея HollowEngine

Если свести всю документацию к одной архитектурной формуле:

```text
Kotlin
  +
DSL
  +
State Machine
  +
Persistent State
  +
Minecraft API
  =
Story Engine
```

А Story Event можно представить так:

```text
Сценарий
│
├── последовательность
│
├── условия
│
├── циклы
│
├── параллельные задачи
│
├── состояние
│
├── NPC
│
├── игроки
│
├── диалоги
│
├── камера
│
├── анимации
│
├── частицы
│
└── мир
```

---

# 99. Оригинальные страницы документации

Основная Wiki:

https://hollowengine-docs.readthedocs.io/en/latest/

Введение:

https://hollowengine-docs.readthedocs.io/en/latest/scripting/introduction/

Story Events:

https://hollowengine-docs.readthedocs.io/en/latest/scripting/story_events/

Диалоги:

https://hollowengine-docs.readthedocs.io/en/latest/scripting/story_events/dialogues/features/

NPC:

https://hollowengine-docs.readthedocs.io/en/latest/scripting/story_events/npcs/creation/

https://hollowengine-docs.readthedocs.io/en/latest/scripting/story_events/npcs/settings/

https://hollowengine-docs.readthedocs.io/en/latest/scripting/story_events/npcs/capabilities/

https://hollowengine-docs.readthedocs.io/en/latest/scripting/story_events/npcs/animations/

Модели:

https://hollowengine-docs.readthedocs.io/en/latest/scripting/story_events/npcs/model_features/

Игроки:

https://hollowengine-docs.readthedocs.io/en/latest/scripting/story_events/players/configure/

https://hollowengine-docs.readthedocs.io/en/latest/scripting/story_events/players/animations/

Условия:

https://hollowengine-docs.readthedocs.io/en/latest/scripting/story_events/actions/conditions/

Циклы:

https://hollowengine-docs.readthedocs.io/en/latest/scripting/story_events/actions/loops/

Асинхронные действия:

https://hollowengine-docs.readthedocs.io/en/latest/scripting/story_events/actions/async/

Время:

https://hollowengine-docs.readthedocs.io/en/latest/scripting/story_events/world/time/

Камера:

https://hollowengine-docs.readthedocs.io/en/latest/scripting/story_events/cutscenes/camera/

Переходы:

https://hollowengine-docs.readthedocs.io/en/latest/scripting/story_events/cutscenes/transitions/

Частицы:

https://hollowengine-docs.readthedocs.io/en/latest/scripting/story_events/effects/particles/

Интерполяции:

https://hollowengine-docs.readthedocs.io/en/latest/scripting/common/util/interpolations/

Импорт:

https://hollowengine-docs.readthedocs.io/en/latest/scripting/common/imports/

Установка:

https://hollowengine-docs.readthedocs.io/en/latest/install/

Репозиторий:

https://github.com/HollowHorizon/HollowEngine

---

# 100. Примечание об актуальности

Данный файл объединяет структуру и материалы старой HollowEngine Wiki, доступные в разделе `Scripting`.

На официальной странице Wiki в структуре действительно присутствуют разделы Story Events, Actions, NPC, Players, World, Dialogues, Cutscenes, Events, Effects, Interpolations и Imports.

При работе с конкретной версией HollowEngine следует сверять синтаксис с исходным кодом соответствующего релиза, поскольку API движка развивается.

Для StoryEngine наиболее важными разделами являются:

```text
Story Events
NPC
Players
Dialogues
Actions
Conditions
Loops
Async
Camera
Transitions
Particles
Interpolation
Import
```

---

# Конец документа
