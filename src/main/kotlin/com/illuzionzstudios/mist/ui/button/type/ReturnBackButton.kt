package com.illuzionzstudios.mist.ui.button.type

import com.cryptomorin.xseries.XMaterial
import com.illuzionzstudios.mist.item.ItemCreator
import com.illuzionzstudios.mist.ui.UserInterface
import com.illuzionzstudios.mist.ui.button.Button
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

/**
 * A button that returns to a previous/parent [UserInterface]
 */
class ReturnBackButton(
    /**
     * Instance of [UserInterface] to open
     */
    private val parentInterface: UserInterface?,

    /**
     * The icon to display
     */
    override val item: ItemStack = ItemCreator.builder().material(XMaterial.OAK_DOOR).name("&4&lReturn Back").build()
        .makeUIItem(),

    /**
     * Create a new instanceof using [UserInterface.newInstance] when showing the interface?
     */
    private val newInstance: Boolean = false
) : Button() {

    /**
     * Open the parent interface
     */
    override val listener: ButtonListener
        get() = object : ButtonListener {
            override fun onClickInInterface(
                player: Player?,
                ui: UserInterface?,
                type: ClickType?,
                event: InventoryClickEvent?
            ) {
                // When clicking don't move items
                event?.isCancelled = true
                if (newInstance) parentInterface?.newInstance()?.show(player!!) else parentInterface?.show(player!!)
            }
        }
}