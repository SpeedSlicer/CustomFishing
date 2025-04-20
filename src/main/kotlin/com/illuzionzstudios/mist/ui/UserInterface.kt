package com.illuzionzstudios.mist.ui

import com.illuzionzstudios.mist.exception.PluginException
import com.illuzionzstudios.mist.plugin.SpigotPlugin
import com.illuzionzstudios.mist.scheduler.MinecraftScheduler
import com.illuzionzstudios.mist.scheduler.Tickable
import com.illuzionzstudios.mist.ui.button.Button
import com.illuzionzstudios.mist.ui.button.type.ReturnBackButton
import com.illuzionzstudios.mist.ui.render.InterfaceDrawer
import com.illuzionzstudios.mist.util.*
import lombok.*
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import java.lang.reflect.*
import java.util.*

/**
 * Our main menu (or interface) for all inventory interaction. We provide
 * a lot of functionality for adding buttons, storing items, and being
 * able to navigate through the menu easily. We can also have a parent menu
 * that can be returned to.
 */
abstract class UserInterface protected constructor(
    /**
     * The parent interface we entered from. This allows us to return to a previous menu
     * Can be `null` if no parent
     */
    val parent: UserInterface? = null,

    /**
     * If when returning to parent will create a new instance
     * of the inventory
     */
    val makeNewInstance: Boolean = false
) : Tickable {

    /**
     * Registered buttons to this menu (via reflection) to add in rendering
     */
    private val registeredButtons: MutableList<Button?> = ArrayList()

    /**
     * @return A list of buttons to manually add instead of scanning
     */
    protected val buttonsToRegister: List<Button?>?
        get() = null

    //  -------------------------------------------------------------------------
    //  Properties of an interface
    //  -------------------------------------------------------------------------

    /**
     * The return button to display if applicable
     */
    private val returnButton: Button

    /**
     * Dictates if we called the method to register buttons
     * See [.registerButtonViaReflection]
     */
    private var buttonsRegisteredViaReflection = false

    /**
     * Amount of slots in the inventory
     */
    var size = 9 * 3
        protected set

    /**
     * This is the title to display at the top of the interface
     */
    var title = "&8Menu"
        protected set

    /**
     * This is the description of the menu that can be displayed
     * at a certain slot
     */
    private var info: Array<String>? = null

    /**
     * This is the player currently viewing the menu.
     * Isn't set till displayed to a player
     */
    protected var viewer: Player? = null

    //  -------------------------------------------------------------------------
    //  Button utils
    //  -------------------------------------------------------------------------

    /**
     * Scans the class for every [Button] instance and registers it
     */
    protected fun registerButtons() {
        // Don't double register stuff
        registeredButtons.clear()

        // Register buttons explicitly given
        run {
            val buttons = buttonsToRegister
            if (buttons != null) registeredButtons.addAll(buttons)
        }

        // Register buttons declared as fields
        run {
            var lookup: Class<*> = javaClass

            // Scan every class and super until interface class
            do for (f in lookup.declaredFields) registerButtonViaReflection(f) while (UserInterface::class.java.isAssignableFrom(
                    lookup.superclass.also { lookup = it })
            )
        }
    }

    /**
     * Registers a [Button] into this [UserInterface] if the
     * field is a [Button]
     *
     * @param field The field to register
     */
    private fun registerButtonViaReflection(field: Field) {
        field.isAccessible = true
        val clazz = field.type

        // Is just a button instance
        if (Button::class.java.isAssignableFrom(clazz)) {
            // Get button
            val button = ReflectionUtil.getFieldContent(field, this) as Button
            Valid.checkNotNull(button, "Invalid button for names field " + field.name)
            registeredButtons.add(button)
        } else if (Array<Button>::class.java.isAssignableFrom(clazz)) {
            // Array of buttons
            Valid.checkBoolean(
                Modifier.isFinal(field.modifiers),
                "Button[] field must be final: $field"
            )
            val buttons = ReflectionUtil.getFieldContent(field, this) as Array<Button?>
            Valid.checkBoolean(buttons.isNotEmpty(), "Null " + field.name + "[] in " + this)
            registeredButtons.addAll(listOf(*buttons))
        }
        buttonsRegisteredViaReflection = true
    }

    /**
     * Try to get a button with a specific icon from [ItemStack]
     *
     * @param icon [ItemStack] to find by
     * @return Found button otherwise null
     */
    fun getButton(icon: ItemStack?): Button? {
        if (!buttonsRegisteredViaReflection) registerButtons()
        if (icon != null) {
            for (button in registeredButtons) {
                // Make sure valid button
                Valid.checkNotNull(button, "Menu button is null at " + javaClass.simpleName)
                if (button?.item == null) return null
                if (equals(icon, button.item)) {
                    return button
                }
            }
        }
        return null
    }

    /**
     * Internal check if [ItemStack] are equal
     */
    private fun equals(stack1: ItemStack, stack2: ItemStack?): Boolean {
        val meta1 = stack1.itemMeta
        val meta2 = stack2!!.itemMeta
        if (stack1.type != stack2.type) return false
        return if (meta1 != null) meta1 == meta2 else stack1.amount == stack2.amount
    }

    /**
     * Return a new instance of this interface
     *
     * You must override this in certain cases
     *
     * @return the new instance, of null
     * @throws PluginException if new instance could not be made, for example when the menu is
     * taking constructor params
     */
    fun newInstance(): UserInterface {
        try {
            return ReflectionUtil.instantiate(javaClass)
        } catch (t: Throwable) {
            try {
                val parent = javaClass.getMethod("getParent").invoke(javaClass)
                if (parent != null) return ReflectionUtil.instantiate(javaClass, parent)
            } catch (ignored: Throwable) {
            }
            t.printStackTrace()
        }
        throw PluginException(
            "Could not instantiate menu of $javaClass, override 'newInstance' and ensure constructor is public!"
        )
    }
    //  -------------------------------------------------------------------------
    //  Rendering
    //  -------------------------------------------------------------------------

    /**
     * Build, render, and show our [UserInterface] to a player
     * Only used to firstly show it to a player, shouldn't be used to re-render
     *
     * @param player The player to show the menu to
     */
    fun show(player: Player) {
        Valid.checkNotNull(size, "Size not set in $this (call setSize in your constructor)")
        Valid.checkNotNull(title, "Title not set in $this (call setTitle in your constructor)")

        // Set our viewer
        viewer = player
        preDisplay()

        // If buttons didn't get registered, do it ourselves
        if (!buttonsRegisteredViaReflection) registerButtons()

        // Render the menu
        val drawer: InterfaceDrawer = InterfaceDrawer.Companion.of(size, title)

        // Compile bottom bar
        compileBottomBar().forEach { (slot: Int, item: ItemStack?) -> drawer.setItem(slot, item) }

        // Set items defined by classes upstream
        // Doesn't replace set items
        for (i in 0 until drawer.size) {
            val item = getItemAt(i)
            if (item != null && !drawer.isSet(i)) drawer.setItem(i, item)
        }

        // Call event
        onDisplay(drawer)

        // Set our previous menu if applicable
        val previous = getInterface(player)
        if (previous != null) player.setMetadata(
            TAG_PREVIOUS,
            FixedMetadataValue(SpigotPlugin.instance!!, previous)
        )

        // Register current menu
        MinecraftScheduler.Companion.get()!!.synchronize(Runnable {
            drawer.display(player)
            player.setMetadata(
                TAG_CURRENT,
                FixedMetadataValue(SpigotPlugin.instance!!, this@UserInterface)
            )
        }, 1)
    }

    /**
     * Run any last minute registering before the interface is displayed.
     * Good if you want a dynamic interface. Ensures viewer is set
     */
    protected fun preDisplay() {}

    /**
     * Called automatically before the menu is displayed but after all items have
     * been drawn
     *
     * Override for custom last-minute modifications
     *
     * @param drawer The drawer for the interface
     */
    protected open fun onDisplay(drawer: InterfaceDrawer) {}

    /**
     * "Restart" this interface. This means re-registering all buttons,
     * and redrawing all items
     */
    fun restart() {
        preDisplay()
        registerButtons()
        redraw()
    }

    /**
     * Tick rendering
     * To be overridden
     */
    override fun tick() {}

    /**
     * Simply re-render the inventory and bottom items
     */
    protected fun redraw() {
        val inv = viewer!!.openInventory.topInventory

        // Make sure a chest inventory and not something else
        Valid.checkBoolean(
            inv.type == InventoryType.CHEST,
            viewer!!.name + "'s inventory closed in the meanwhile (now == " + inv.type + ")."
        )
        for (i in 0 until size) {
            val item = getItemAt(i)
            Valid.checkBoolean(
                i < inv.size, "Item (" + (item?.type ?: "null") + ") position ("
                        + i + ") > inv size (" + inv.size + ")"
            )
            inv.setItem(i, item)
        }
        compileBottomBar().forEach { (i: Int?, itemStack: ItemStack?) ->
            inv.setItem(
                i, itemStack
            )
        }
        viewer!!.updateInventory()
    }

    /**
     * Map the buttons placed for navigation to their slots
     *
     * @return Map of items
     */
    private fun compileBottomBar(): Map<Int, ItemStack?> {
        val items: MutableMap<Int, ItemStack?> = HashMap()
        if (addInfoButton() && info != null) items[infoButtonPosition] = Button.makeInfo(*info!!).item
        if (addReturnButton() && returnButton !is Button.IconButton) items[returnButtonPosition] = returnButton.item
        return items
    }

    /**
     * Returns the item at a certain slot
     *
     *
     * To be overridden by the type of menu to get the item
     *
     * @param slot the slow
     * @return the item, or null if no icon at the given slot (default)
     */
    open fun getItemAt(slot: Int): ItemStack? {
        return null
    }

    /**
     * Get the info button position
     *
     * @return the slot which info buttons is located on
     */
    protected val infoButtonPosition: Int
        get() = size - 9

    /**
     * Should we automatically add the return button to the bottom left corner?
     *
     * @return true if the return button should be added, true by default
     */
    protected fun addReturnButton(): Boolean {
        return true
    }

    /**
     * Should we automatically add an info button [.getInfo] at the
     * [.getInfoButtonPosition] ?
     *
     * @return If to add button
     */
    protected fun addInfoButton(): Boolean {
        return true
    }

    /**
     * Get the return button position
     *
     * @return the slot which return buttons is located on
     */
    protected val returnButtonPosition: Int
        get() = size - 1

    /**
     * Calculates the center slot of this menu
     *
     * Credits to Gober at
     * https://www.spigotmc.org/threads/get-the-center-slot-of-a-menu.379586/
     *
     * @return the estimated center slot
     */
    protected val centerSlot: Int
        get() {
            val pos = size / 2
            return if (size % 2 == 1) pos else pos - 5
        }

    /**
     * Return the top opened inventory if viewer exists
     *
     * @return The open inventory instance
     */
    protected val inventory: Inventory
        get() {
            Valid.checkNotNull(viewer, "Cannot get inventory when there is no viewer!")
            val topInventory = viewer!!.openInventory.topInventory
            Valid.checkNotNull(topInventory, "Top inventory is null!")
            return topInventory
        }

    /**
     * Get the open inventory content to match the array length, cloning items
     * preventing ID mismatch in yaml files
     *
     * @param from The slot to start from
     * @param to   The slot to end at
     * @return The array of found [ItemStack] can contain [org.bukkit.Material.AIR]
     */
    protected fun getContent(from: Int, to: Int): Array<ItemStack?> {
        val content = inventory.contents
        val copy = arrayOfNulls<ItemStack>(content.size)
        for (i in from until copy.size) {
            val item = content[i]
            copy[i] = item?.clone()
        }
        return Arrays.copyOfRange(copy, from, to)
    }

    //  -------------------------------------------------------------------------
    //  Interface events
    //  -------------------------------------------------------------------------

    /**
     * Master method called when the interface is clicked on. Calls methods to be implemented
     * and handles click logic.
     *
     * It passes down to [.onInterfaceClick]
     *
     * @param player    The player clicking the menu
     * @param slot      The slot that was clicked
     * @param action    The type of action performed
     * @param click     How the slot was clicked
     * @param cursor    What [ItemStack] was on the cursor
     * @param clicked   The clicked [ItemStack]
     * @param cancelled If the event was cancelled
     * @param event     The actual event if needed
     */
    open fun onInterfaceClick(
        player: Player, slot: Int, action: InventoryAction, click: ClickType?,
        cursor: ItemStack?, clicked: ItemStack?, cancelled: Boolean, event: InventoryClickEvent
    ) {
        val openedInventory = player.openInventory
        onInterfaceClick(player, slot, clicked, event, cancelled)

        // Delay by 1 tick to get the accurate item in slot
        MinecraftScheduler.get()!!.synchronize({
            // Make sure inventory is still open 1 tick later
            if (openedInventory == player.openInventory) {
                val topInventory = openedInventory.topInventory
                if (action.toString().contains("PLACE") || action.toString() == "SWAP_WITH_CURSOR") onItemPlace(
                    player,
                    slot,
                    topInventory.getItem(slot),
                    event
                )
            }
        }, 1)
    }

    /**
     * Called automatically when the interface is clicked
     *
     * @param player  The player clicking the menu
     * @param slot    The slot that was clicked
     * @param clicked The clicked [ItemStack]
     */
    protected open fun onInterfaceClick(
        player: Player?,
        slot: Int,
        clicked: ItemStack?,
        event: InventoryClickEvent,
        cancelled: Boolean = true
    ) {
        // By default cancel moving items
        event.isCancelled = cancelled
    }

    /**
     * Called automatically when an item is placed to the menu
     *
     * @param player The player clicking the menu
     * @param slot   The slot that was clicked
     * @param placed The [ItemStack] that was placed
     */
    protected open fun onItemPlace(player: Player?, slot: Int, placed: ItemStack?, event: InventoryClickEvent) {
        // By default cancel moving items
        event.isCancelled = true
    }

    /**
     * Called when a registered button is clicked on
     *
     * @param player The player clicking the menu
     * @param slot   The slot that was clicked
     * @param action The type of action performed
     * @param click  How the slot was clicked
     * @param button The [Button] object clicked
     */
    open fun onButtonClick(
        player: Player?, slot: Int, action: InventoryAction?,
        click: ClickType?, button: Button, event: InventoryClickEvent
    ) {
        // By default cancel moving items
        event.isCancelled = true
        button.listener?.onClickInInterface(player, this, click, event)
    }

    /**
     * Called when the interface is closed
     *
     * @param player    The player who closed the interface
     * @param inventory The [Inventory] instance ended
     */
    fun onInterfaceClose(player: Player?, inventory: Inventory?) {}

    companion object {
        /**
         * This is an internal metadata tag that the player has.
         *
         * This will set the name of the current menu in order to keep
         * track of what menu is currently open
         */
        val TAG_CURRENT = "UI_" + SpigotPlugin.pluginName

        /**
         * This is an internal metadata tag that the player has.
         *
         * This will set the name of the previous menu in order to
         * backtrack for returning menus
         */
        val TAG_PREVIOUS = "UI_PREVIOUS_" + SpigotPlugin.pluginName

        /**
         * Get the currently active menu for the player
         *
         * @param player The player to get menu for
         * @return Found interface or `null` See [.getInterfaceViaTag]
         */
        fun getInterface(player: Player): UserInterface? {
            return getInterfaceViaTag(player, TAG_CURRENT)
        }

        /**
         * Get the previous active menu for the player
         *
         * @param player The player to get menu for
         * @return Found interface or `null` See [.getInterfaceViaTag]
         */
        fun getPrevious(player: Player): UserInterface? {
            return getInterfaceViaTag(player, TAG_PREVIOUS)
        }

        /**
         * Get a [UserInterface] from the metadata on a player
         *
         * @param player The player to check metadata
         * @param tag    The name of the tag storing the interface
         * @return Found [UserInterface] otherwise `null`
         */
        fun getInterfaceViaTag(player: Player, tag: String): UserInterface? {
            if (player.hasMetadata(tag)) {
                // Cast from tag
                val userInterface = player.getMetadata(tag)[0].value() as UserInterface?
                Valid.checkNotNull(
                    userInterface,
                    "Interface was missing from " + player.name + "'s metadata " + tag + "tag!"
                )
                return userInterface
            }
            return null
        }
    }

    init {
        // Auto set return button to set parent menu
        returnButton = parent?.let { ReturnBackButton(it, newInstance = makeNewInstance) }
            ?: Button.makeEmpty()
    }
}