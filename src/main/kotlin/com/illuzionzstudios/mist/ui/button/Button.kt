package com.illuzionzstudios.mist.ui.button

import com.cryptomorin.xseries.XMaterial
import com.illuzionzstudios.mist.item.ItemCreator
import com.illuzionzstudios.mist.ui.UserInterface
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

/**
 * This represents a button in a [com.illuzionzstudios.mist.ui.UserInterface]
 * that can be interacted with an perform actions
 */
abstract class Button {

    /**
     * @return The implemented [ButtonListener] to give functionality
     */
    abstract val listener: ButtonListener?

    /**
     * @return The [ItemStack] that represents this button as an icon
     */
    abstract val item: ItemStack?

    /**
     * Represents a listener for a [Button] to
     * provide functionality
     */
    interface ButtonListener {
        /**
         * Invoked when the button is clicked on
         *
         * @param player The [Player] who clicked
         * @param ui     The instance of [UserInterface] that was clicked on
         * @param type   How the button was clicked on
         * @param event  The click event should we need it
         */
        fun onClickInInterface(player: Player?, ui: UserInterface?, type: ClickType?, event: InventoryClickEvent?)

        companion object {
            /**
             * @return Simply returns a listener without functionality
             */
            fun ofNull(): ButtonListener {
                return object : ButtonListener {
                    override fun onClickInInterface(
                        player: Player?,
                        ui: UserInterface?,
                        type: ClickType?,
                        event: InventoryClickEvent?
                    ) {
                        // Nothing
                    }
                }
            }
        }
    }

    /**
     * Represents a "blank" button. This means it doesn't do anything
     * when clicked but just renders an item stack
     */
    class IconButton(
        /**
         * The [ItemStack] to render
         */
        override val item: ItemStack,
        override val listener: ButtonListener? = ButtonListener.ofNull()
    ) : Button()

    /**
     * A simple button that renders an item and does
     * something when clicked
     */
    class SimpleButton(
        /**
         * The [ItemStack] to render
         */
        override val item: ItemStack? = null,

        /**
         * Listener to execute when clicked on
         */
        override val listener: ButtonListener? = null
    ) : Button()

    companion object {
        /**
         * The material representing info button, see [.makeInfo]
         */
        private val infoButtonMaterial = XMaterial.NETHER_STAR

        /**
         * Construct a button that uses name and lore to display lines of text
         *
         * @param description Lines to display
         * @return The dummy icon
         */
        fun makeInfo(vararg description: String?): IconButton {
            // Get all except first
            val lore = listOf(*description).subList(1, listOf(*description).size).toMutableList()

            // Format lines
            for (i in lore.indices) {
                lore[i] = "&7" + lore[i]
            }

            return makeIcon(
                ItemCreator(
                    material = infoButtonMaterial,
                    name = description[0],
                    hideTags = true,
                    lores = lore.toList()
                )
            )
        }

        /**
         * Construct an icon from a [ItemCreator]
         *
         * @param creator Creator of an item
         * @return The now appropriate item
         */
        fun makeIcon(creator: ItemCreator): IconButton {
            return IconButton(creator.makeUIItem())
        }

        fun makeIcon(stack: ItemStack): IconButton {
            return IconButton(stack)
        }

        /**
         * @return Simply a button that is just air, represents `null`
         */
        fun makeEmpty(): IconButton {
            return IconButton(ItemStack(Material.AIR))
        }

        /**
         * Easily create a [SimpleButton]
         *
         * @param creator  Creator for item to use
         * @param listener Listener (usually as lambda)
         * @return Created button to use
         */
        fun of(creator: ItemCreator, listener: ButtonListener?): SimpleButton {
            return SimpleButton(creator.makeUIItem(), listener)
        }
    }
}