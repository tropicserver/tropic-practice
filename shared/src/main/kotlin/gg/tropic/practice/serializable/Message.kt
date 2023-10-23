package gg.tropic.practice.serializable

import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.entity.Player

/**
 * A small utility to serialize chat messages with bound actions.
 *
 * @author GrowlyX
 * @since 10/22/2023
 */
class Message
{
    private val components = mutableListOf<SerializableComponent>()

    fun withMessage(vararg messages: String): Message
    {
        components.add(
            SerializableComponent(
                messages.joinToString(separator = "\n") { it.replace("&", "§") }
            )
        )

        return this
    }

    fun andHoverOf(vararg hover: String): Message
    {
        try
        {
            val latestComponent = components[components.size - 1]
            latestComponent.hoverMessage = hover.joinToString(separator = "\n")

            return this
        } catch (exception: Exception)
        {
            throw InvalidComponentException("No component found to apply hover to")
        }
    }

    fun andCommandOf(action: ClickEvent.Action, command: String): Message
    {
        try
        {
            val latestComponent = components[components.size - 1]
            latestComponent.clickEvent = ClickEvent(action, command)

            return this
        } catch (exception: Exception)
        {
            throw InvalidComponentException("No component found to apply command to")
        }
    }

    fun sendToPlayer(player: Player)
    {
        val components = mutableListOf<TextComponent>()

        this.components.forEach { serializable ->
            val textComponent = TextComponent(serializable.value)

            serializable.hoverMessage?.let {
                textComponent.hoverEvent = HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    ComponentBuilder(it).create()
                )
            }

            serializable.clickEvent?.let {
                textComponent.clickEvent = it
            }

            components.add(textComponent)
        }

        player.spigot().sendMessage(
            *components.toTypedArray()
        )
    }

    class InvalidComponentException(message: String) : RuntimeException(message)

    data class SerializableComponent(var value: String)
    {
        var clickEvent: ClickEvent? = null
        var hoverMessage: String? = null
    }
}
