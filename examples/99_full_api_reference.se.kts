// Пример 99 — полный справочник API HollowEngine 1.19.2-1.6.2a.
//
// Этот файл компилируется. Он используется как эталон при проверке документации:
// каждый вызов ниже — реально существующий API из hc-1.19.2-1.6.3.jar, проверенный
// компилятором Kotlin 2.0.21. Если что-то здесь перестало компилироваться —
// значит документация устарела, а не наоборот.
//
// Порядок разделов повторяет оглавление 08_IMPORTS_RU.md.
@file:EntryPoint

package storyengine.examples

import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.phys.Vec3
import net.minecraftforge.event.level.BlockEvent
import ru.hollowhorizon.hc.client.models.gltf.animations.AnimationType
import ru.hollowhorizon.hc.client.models.gltf.animations.PlayMode
import ru.hollowhorizon.hc.client.models.gltf.manager.LayerMode
import ru.hollowhorizon.hollowengine.common.entities.NPCEntity
import ru.hollowhorizon.hollowengine.common.npcs.HitboxMode
import ru.hollowhorizon.hollowengine.common.npcs.NpcIcon
import ru.hollowhorizon.hollowengine.common.scripting.RUNTIME_PROPERTIES
import ru.hollowhorizon.hollowengine.common.scripting.addStage
import ru.hollowhorizon.hollowengine.common.scripting.bold
import ru.hollowhorizon.hollowengine.common.scripting.color
import ru.hollowhorizon.hollowengine.common.scripting.east
import ru.hollowhorizon.hollowengine.common.scripting.hasStage
import ru.hollowhorizon.hollowengine.common.scripting.italic
import ru.hollowhorizon.hollowengine.common.scripting.item
import ru.hollowhorizon.hollowengine.common.scripting.north
import ru.hollowhorizon.hollowengine.common.scripting.obfuscated
import ru.hollowhorizon.hollowengine.common.scripting.players
import ru.hollowhorizon.hollowengine.common.scripting.removeStage
import ru.hollowhorizon.hollowengine.common.scripting.runtime
import ru.hollowhorizon.hollowengine.common.scripting.skin
import ru.hollowhorizon.hollowengine.common.scripting.south
import ru.hollowhorizon.hollowengine.common.scripting.stages
import ru.hollowhorizon.hollowengine.common.scripting.story.global
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.Break
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.Continue
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.DoWhile
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.Else
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.If
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.While
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.await
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.events.gui
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.events.keybind
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.events.waitClick
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.execute
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.fadeIn
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.fadeInOut
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.fadeOut
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.next
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.not
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.playSound
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.restartScript
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.send
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.startScript
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.stopScript
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.stopSound
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.wait
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.base.waitForgeEvent
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.camera.camera
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.camera.shake
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.dialogues.ChoiceAction
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.dialogues.choices
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.dialogues.dialogue
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.dialogues.sonhar.sonharDialogue
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.npcs.asSubModel
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.npcs.collectItems
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.npcs.distanceToXZ
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.npcs.subModel
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.npcs.subModels
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.particles.effekseer
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.world.pauseTime
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.world.resumeTime
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.world.waitStorage
import ru.hollowhorizon.hollowengine.common.scripting.story.progressManager
import ru.hollowhorizon.hollowengine.common.scripting.story.saveable
import ru.hollowhorizon.hollowengine.common.scripting.strikethrough
import ru.hollowhorizon.hollowengine.common.scripting.stringName
import ru.hollowhorizon.hollowengine.common.scripting.tag
import ru.hollowhorizon.hollowengine.common.scripting.test
import ru.hollowhorizon.hollowengine.common.scripting.underline
import ru.hollowhorizon.hollowengine.common.scripting.west
import ru.hollowhorizon.hollowengine.common.util.Keybind
import ru.hollowhorizon.hollowengine.common.scripting.story.nodes.util.PostEffect


    // ---------- players / server ----------
    val players by server.players
    val pm = players.progressManager
    test()
    println(RUNTIME_PROPERTIES)

    // ---------- variables ----------
    var saved by saveable { 0 }
    var glob by global { "reset" }
    var temp by runtime { false }

    // ---------- wait / time ----------
    wait { 20 }
    wait { 1.sec }
    wait { 1.min }
    wait { 1.hours }
    await { true }

    // ---------- control flow ----------
    val cond = If({ saved > 0 }) {
        wait { 10 }
    }
    cond.Else {
        wait { 5 }
    }
    cond.Elif({ false }) { }
    cond.Else { }

    While({ saved < 5 }) {
        saved++
        +Break { "tag-a" }
        +Continue { "tag-a" }
    }
    While({ saved < 5 }, { "tag-a" }) {
        +Break { "tag-a" }
    }
    DoWhile({ false }) { }
    DoWhile({ false }, { "tag-b" }) { }
    val negated = { false }.not()

    // ---------- script control ----------
    next { }
    +startScript { "other.se.kts" }
    +stopScript { "other.se.kts" }
    +restartScript()
    +execute { "/say hello" }
    +send("Literal".bold)

    // ---------- messages ----------
    +send("raw message".color(0xFFFFFF))
    players.send { "hi {0}" }
    players.sendComponent { "hi".color(0xFFAA00) }
    players.say { "hmm" }
    players.sayComponent { "hmm".italic }
    players.sayComponent { "x".obfuscated }
    players.sayComponent { "x".strikethrough }
    println("x".underline)
    players.sayComponent { "x".color("#FFAA00") }
    players.sayComponent { "x".color("#FFAA00") }

    // ---------- progress ----------
    +pm.addMessage { "step 1" }
    +pm.removeMessage { "step 1" }
    +pm.clear()

    // ---------- npc creation ----------
    val npc by NPCEntity.creating {
        name = "Bob"
        model = "npc/bob.json"
        world = "minecraft:overworld"
        pos = pos(0.0, 64.0, 0.0)
        rotation = vec(0.0, 0.0)
        showName = true
        switchHeadRot = true
        size = 1.0f to 2.0f
        animations[AnimationType.IDLE] = "Idle"
        textures["default"] = "textures/entity/bob.png"
        skin("default")
    }
    val found by NPCEntity.finding {
        name = "Bob"
        world = "minecraft:overworld"
    }
    val sub by NPCEntity.fromSubModel {
        subModel { model = "npc/sub.json" }
    }

    // ---------- npc properties ----------
    npc.name = "Bobby"
    npc.flying = true
    npc.invulnerable = true
    npc.isRunning = true
    npc.hitboxMode = HitboxMode.PULLING
    npc.icon = NpcIcon.EMPTY
    val npcName: String = npc.name
    val sub1 = npc.asSubModel
    val subMap = npc.subModels
    val dist: Double = npc.invoke().distanceToXZ(pos(0.0, 0.0, 0.0))
    val dist2: Double = npc.invoke().distanceToXZ(players.invoke().first())
    npc.despawn()
    npc.configure { skin("default") }

    // ---------- npc actions ----------
    npc.say { "hello" }
    npc.sayComponent { "hello".color(0xFFFFFF) }
    npc.moveTo<Vec3> { pos(10.0, 64.0, 10.0) }
    npc.moveTo<LivingEntity> { players.invoke().first() }
    npc.moveTo<Collection<Vec3>> { listOf(pos(1.0, 1.0, 1.0)) }
    npc.moveToBiome { "minecraft:plains" }
    npc.moveToStructure({ "minecraft:village" })
    npc.moveAlwaysTo<Vec3> { pos(20.0, 64.0, 20.0) }
    npc.stopMoveAlways()
    npc.setTarget<LivingEntity> { players.invoke().first() }
    npc.clearTarget()
    npc.lookAt<Vec3> { pos(0.0, 0.0, 0.0) }
    npc.lookAlwaysAt<LivingEntity> { players.invoke().first() }
    npc.stopLookAlways()
    npc.lookAtEntityType { "minecraft:zombie" }
    npc.setPose { "minecraft:sitting" }
    npc.clearPose()
    npc.tpTo {
        pos = pos(0.0, 64.0, 0.0)
        vec = vec(0.0, 0.0)
        world = "minecraft:overworld"
    }
    npc.useBlock { pos(1.0, 1.0, 1.0) }
    npc.destroyBlock { pos(1.0, 1.0, 1.0) }
    npc.dropItem { item("minecraft:diamond", 1) }
    npc.giveLeftHand { item("minecraft:paper", 3) }
    npc.giveRightHand { item("minecraft:diamond", 1) }
    npc.waitInteract()
    npc.replay { "Idle" }
    npc.replayAndWait { "Idle" }
    npc.play { animation = "Walk"; playType = PlayMode.LOOPED; layerMode = LayerMode.OVERWRITE; speed = 1.0f }
    npc.playOnce { "Attack" }
    npc.playLooped { "Walk" }
    npc.playFreeze { "Idle" }
    npc.stop { "Idle" }
    npc.addTrade { MerchantOffer(item("minecraft:paper", 1), item("minecraft:apple", 1), 1, 1, 1.0f) }
    npc.clearTrades()
    npc.clearTradeUses()
    npc.requestItems {
        text = "Give me 3 apples"
        +item("minecraft:apple", 3)
    }
    collectItems {
        text = "Give me 3 apples"
        +item("minecraft:apple", 3)
    }
    waitStorage {
        world = "minecraft:overworld"
        pos = pos(10.0, 64.0, 10.0)
        consumeItems = true
        +item("minecraft:diamond", 1)
    }

    // ---------- player actions ----------
    players.tpTo {
        pos = pos(0.0, 64.0, 0.0)
        vec = vec(0.0, 0.0)
        world = "minecraft:overworld"
    }
    players.seat {
        pos = pos(0.0, 64.0, 0.0)
        offset = 0.0
        rot = north
    }
    players.seat { rot = south }
    players.play { animation = "Walk" }
    players.playOnce { "Attack" }
    players.playLooped { "Walk" }
    players.playFreeze { "Idle" }
    players.stop { "Idle" }
    players.configure { skin("default") }
    +players.saveInventory()
    +players.loadInventory()
    +players.clearInventory()
    +players.postEffect { ResourceLocation("hollowengine", "effects/confusion") }
    +players.clearPostEffects()
    +players.removeMark { pos(0.0, 0.0, 0.0) }
    val fx1 = PostEffect.BLUR
    val fx2 = PostEffect.create("some/path")
    players.waitPos {
        pos = pos(0.0, 64.0, 0.0)
        radius = 3.0
        world = "minecraft:overworld"
        inverse = false
        ignoreY = true
    }
    val inputNode = players.input("name", "yes")
    println(inputNode.values)
    println(inputNode.message)

    // ---------- player stages ----------
    val p = players.invoke().first()
    println(p.stringName)
    p.addStage("intro")
    val has = p.hasStage("intro")
    p.removeStage("intro")
    val all: Collection<String> = p.stages

    // ---------- input events ----------
    +waitClick()
    +keybind { Keybind.A }
    +waitForgeEvent<BlockEvent.BreakEvent> { it.state.isAir }

    // ---------- gui ----------
    gui {
        runSingleCommandInGui("time set day")
        close()
    }

    // ---------- sound / fade / time / shake ----------
    +playSound {
        sound = "hollowengine:effects/bell"
        volume = 1.0f
        pitch = 1.0f
        pos = pos(0.0, 0.0, 0.0)
    }
    +stopSound { "hollowengine:effects/bell" }
    +fadeIn { text = "Chapter 1"; subtitle = "Prologue"; time = 40; color = 0xFFFFFF }
    +fadeOut { text = "The End" }
    fadeInOut { text = "Loading" }
    pauseTime()
    resumeTime()
    shake { /* ScreenShakePacket */ }

    // ---------- camera / particles ----------
    +camera {
        static(40, pos(0.0, 0.0, 0.0), pos(10.0, 10.0, 10.0))
        entity(40, pos(0.0, 0.0, 0.0)) { npc.invoke() }
        update()
        val end = isEnd
    }
    +effekseer {
        world = "minecraft:overworld"
        path = "hollowengine:particles/smoke"
        pos = pos(0.0, 64.0, 0.0)
        scale(1.0f, 1.0f, 1.0f)
        rotation(0.0f, 90.0f, 0.0f)
        dynamic0 = 1.0f
    }

    // ---------- dialogue ----------
    dialogue {
        dialogueNpc = npc
        val act: ChoiceAction = action
        npc.sayComponent { "Hello!".color(0xFFFFFF) }
        players.sayComponent { "Hi".color(0xFFFFFF) }
        options {
            name = "Bob".color(0xFFFFFF)
            text = "Choose".color(0xFFFFFF)
        }
        choice(ChoiceAction.SCREEN) {
            timeout = 100
            addChoice("Yes".color(0x00FF00)) { dialogueNpc?.sayComponent { "Great!".color(0x00FF00) } }
            "No".invoke { dialogueNpc?.sayComponent { "Fine".color(0xFFFFFF) } }
            onTimeout { dialogueNpc?.sayComponent { "No answer".color(0xFFFFFF) } }
        }
    }
    choices {
        text = "Pick"
        background = "hollowengine:textures/gui/choices.png"
        textX = 10
        textY = 20
        buttonX = 10
        buttonY = 40
        entityX = 100
        entityY = 100
        entitySize = 2.0f
        open = true
        "Left".invoke { }
        "Right".invoke { }
    }
    sonharDialogue {
        open = true
        text.add("Hello")
        sounds.add("hollowengine:effects/bell")
        entities.add(npc)
        npc.entry("Next", "hollowengine:effects/click")
        "Goodbye".invoke { }
    }

    // ---------- async ----------
    val bg = async { }
    +bg.stop()
    +bg.resume()
    +bg.join()

    // ---------- items / tags ----------
    val i1 = item("minecraft:stone", 1)
    val i2 = item("minecraft:stone", 1, "{display:{Name:'{\"text\":\"Rock\"}'}}")
    val i3 = item("minecraft:stone")
    val t = tag("minecraft:planks")
    val d = Direction.NORTH

    // ---------- unaryPlus on a raw node ----------
    +next { }
