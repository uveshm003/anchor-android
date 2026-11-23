package io.github.uveshm003.anchor.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A focus mode: what to block, what to change while it runs, what starts it and what ends it. */
@Serializable
data class Mode(
    val id: ModeId,
    val name: String,
    val icon: ModeIcon = ModeIcon.Focus,
    val color: ModeColor = ModeColor.Teal,
    /** Lower runs first when several automatic triggers fire (0 = highest). */
    val priority: Int = 0,
    val appPolicy: AppPolicy = AppPolicy.BlockList(emptySet()),
    val quietActions: QuietActions = QuietActions(),
    val startTriggers: Set<StartTrigger> = setOf(StartTrigger.Hold, StartTrigger.AnchorTap),
    val exitRule: ExitRule = ExitRule.RequiresAnchor(),
    val strict: Boolean = false,
) {
    init {
        require(name.isNotBlank()) { "Mode name must not be blank" }
        require(priority >= 0) { "Priority must be >= 0, was $priority" }
    }
}

@Serializable
enum class ModeIcon { Focus, Study, Sleep, Driving, Family, Timer }

@Serializable
enum class ModeColor { Teal, Indigo, Purple, Rose, Amber, Green }

/** Which apps a mode blocks. Essentials are never blocked, whatever the policy says (see the engine). */
@Serializable
sealed interface AppPolicy {
    val packages: Set<String>

    /** Would [packageName] be blocked by this policy alone (before essentials are applied)? */
    fun blocks(packageName: String): Boolean

    /** These apps are blocked; everything else works. */
    @Serializable
    @SerialName("block")
    data class BlockList(override val packages: Set<String>) : AppPolicy {
        override fun blocks(packageName: String): Boolean = packageName in packages
    }

    /** Only these apps work; everything else is blocked. */
    @Serializable
    @SerialName("allow")
    data class AllowList(override val packages: Set<String>) : AppPolicy {
        override fun blocks(packageName: String): Boolean = packageName !in packages
    }
}

/** What a mode changes on the phone while it runs (M4). */
@Serializable
data class QuietActions(
    val doNotDisturb: Boolean = false,
    val ringer: RingerMode? = null,
    val holdNotifications: Boolean = false,
    val drivingAutoReply: Boolean = false,
)

@Serializable
enum class RingerMode { Normal, Vibrate, Silent }
