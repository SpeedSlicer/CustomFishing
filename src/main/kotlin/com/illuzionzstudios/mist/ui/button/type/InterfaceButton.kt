package com.illuzionzstudios.mist.ui.button.type

import com.cryptomorin.xseries.XMaterial
import com.illuzionzstudios.mist.Logger
import com.illuzionzstudios.mist.item.ItemCreator
import com.illuzionzstudios.mist.ui.UserInterface
import com.illuzionzstudios.mist.ui.button.Button
import com.illuzionzstudios.mist.util.ReflectionUtil
import com.illuzionzstudios.mist.util.Valid
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.concurrent.Callable

/**
 * A simple button to open another [com.illuzionzstudios.mist.ui.UserInterface]
 */
class InterfaceButton(
    /**
     * Instance of [UserInterface] to open
     */
    private val toOpen: UserInterface?,

    /**
     * Sometimes you need to allocate data when you create the button,
     * but these data are not yet available when you make new instance of this button
     *
     * Use this helper to set them right before showing the button
     */
    private val interfaceLateBind: Callable<UserInterface>?,

    /**
     * The icon to display
     */
    override val item: ItemStack,
    /**
     * Create a new instanceof using [UserInterface.newInstance] when showing the interface?
     */
    private val newInstance: Boolean
) : Button() {
    /**
     * Create a new button that triggers another menu
     *
     * @param menuClass Class of the menu to create
     * @param material  Material for the icon
     * @param name      Name for the icon
     * @param lore      Lore for the icon
     */
    constructor(
        menuClass: Class<out UserInterface?>?,
        material: XMaterial,
        name: String?,
        vararg lore: String?
    ) : this(
        null,
        { ReflectionUtil.instantiate(menuClass!!) },
        ItemCreator(material = material, name = name, lores = lore.toList()).makeUIItem(),
        false
    )

    /**
     * Create a new button that triggers another menu
     *
     * @param menuLateBind The callable to create the menu
     * @param item         The item creator for the icon
     */
    constructor(menuLateBind: Callable<UserInterface>?, item: ItemCreator) : this(
        null,
        menuLateBind,
        item.makeUIItem(),
        false
    )

    /**
     * Create a new button that triggers another menu
     *
     * @param menuLateBind The callable to create the menu
     * @param item         The icon for the button
     */
    constructor(menuLateBind: Callable<UserInterface>?, item: ItemStack) : this(null, menuLateBind, item, false)

    /**
     * Create a new button that triggers another menu
     *
     * @param menu     Instance of menu to create
     * @param material Material for the icon
     * @param name     Name for the icon
     * @param lore     Lore for the icon
     */
    constructor(menu: UserInterface?, material: XMaterial, name: String?, vararg lore: String?) : this(
        menu,
        ItemCreator(material = material, name = name, lores = lore.toList())
    )

    /**
     * Create a new button that triggers another menu
     *
     * @param menu Instance of menu to create
     * @param item The item creator for the icon
     */
    constructor(menu: UserInterface?, item: ItemCreator) : this(
        menu,
        null,
        item.makeUIItem(),
        false
    )

    /**
     * Create a new button that triggers another menu
     *
     * @param menu Instance of menu to create
     * @param item The icon for the button
     */
    constructor(menu: UserInterface?, item: ItemStack) : this(menu, null, item, false)
    constructor(menu: UserInterface?, item: ItemStack, newInstance: Boolean) : this(menu, null, item, newInstance)

    // Try set the menu afterwards
    override val listener: ButtonListener
        get() = object : ButtonListener {
            override fun onClickInInterface(
                player: Player?,
                ui: UserInterface?,
                type: ClickType?,
                event: InventoryClickEvent?
            ) {
                if (interfaceLateBind != null) {
                    var menuToOpen: UserInterface?

                    // Try set the menu afterwards
                    menuToOpen = try {
                        interfaceLateBind.call()
                    } catch (ex: Exception) {
                        Logger.displayError(ex, "Could not open interface via button in " + toOpen?.title)
                        return
                    }
                    if (newInstance) menuToOpen = menuToOpen?.newInstance()
                    menuToOpen!!.show(player!!)
                } else {
                    Valid.checkNotNull(
                        toOpen,
                        "Report / ButtonTrigger requires either 'late bind menu' or normal menu to be set!"
                    )
                    if (newInstance) toOpen!!.newInstance().show(player!!) else toOpen!!.show(player!!)
                }
            }
        }
}