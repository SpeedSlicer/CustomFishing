package com.illuzionzstudios.mist.ui.type

import com.cryptomorin.xseries.XMaterial
import com.illuzionzstudios.mist.Mist
import com.illuzionzstudios.mist.exception.PluginException
import com.illuzionzstudios.mist.item.ItemCreator
import com.illuzionzstudios.mist.ui.UserInterface
import com.illuzionzstudios.mist.ui.button.Button
import com.illuzionzstudios.mist.ui.render.InterfaceDrawer
import com.illuzionzstudios.mist.util.MathUtil
import com.illuzionzstudios.mist.util.Valid
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

/**
 * An interface that displays elements and can switch between pages
 *
 * @param pageSize               size of the menu, a multiple of 9 (keep in mind we already add
 * 1 row there)
 * @param parent                 the parent menu
 * @param pages                  the pages the pages
 * @param returnMakesNewInstance should we re-instatiate the parent menu when returning to it?
 * @param <T> The type of [Object] displayed in the interface
 */
abstract class PagedInterface<T> private constructor(
    pageSize: Int?,
    parent: UserInterface?,
    pages: Iterable<T>,
    returnMakesNewInstance: Boolean
) : UserInterface(parent, returnMakesNewInstance) {

    /**
     * The pages by the page number, containing a list of items
     */
    private val pages: Map<Int, List<T>>

    /**
     * The current page
     */
    private var currentPage = 1

    /**
     * The next button automatically generated
     */
    private var nextButton: Button? = null

    /**
     * The "go to previous page" button automatically generated
     */
    private var prevButton: Button? = null

    /**
     * Create a new paged menu where each page has 3 rows + 1 bottom bar
     *
     * @param pages the pages
     */
    protected constructor(pages: Iterable<T>) : this(null, pages)

    /**
     * Create a new paged menu
     *
     * @param parent the parent menu
     * @param pages  the pages the pages
     */
    protected constructor(parent: UserInterface?, pages: Iterable<T>) : this(null, parent, pages, false)

    /**
     * Create a new paged menu
     *
     * @param parent
     * @param pages
     * @param returnMakesNewInstance
     */
    protected constructor(
        parent: UserInterface?, pages: Iterable<T>,
        returnMakesNewInstance: Boolean
    ) : this(null, parent, pages, returnMakesNewInstance)

    /**
     * Create a new paged menu
     *
     * @param pageSize size of the menu, a multiple of 9 (keep in mind we already add
     * 1 row there)
     * @param pages    the pages
     */
    @Deprecated("we recommend you don't set the page size for the menu to autocalculate")
    protected constructor(pageSize: Int, pages: Iterable<T>) : this(pageSize, null, pages)

    /**
     * Create a new paged menu
     *
     * @param pageSize size of the menu, a multiple of 9 (keep in mind we already add
     * 1 row there)
     * @param parent   the parent menu
     * @param pages    the pages the pages
     */
    @Deprecated("we recommend you don't set the page size for the menu to autocalculate")
    protected constructor(
        pageSize: Int, parent: UserInterface?,
        pages: Iterable<T>
    ) : this(pageSize, parent, pages, false)

    /**
     * Dynamically populates the pages
     *
     * @param items all items that will be split
     * @return the map containing pages and their items
     */
    private fun fillPages(cellSize: Int, items: Iterable<T>): Map<Int, List<T>> {
        val allItems = Mist.toList(items)
        val pages: MutableMap<Int, List<T>> = HashMap()
        val pageCount = if (allItems!!.size == cellSize) 0 else allItems.size / cellSize
        for (i in 0..pageCount) {
            val pageItems: MutableList<T> = ArrayList()
            val down = cellSize * i
            val up = down + cellSize
            for (valueIndex in down until up) if (valueIndex < allItems.size) {
                val page = allItems[valueIndex]
                pageItems.add(page)
            } else break
            pages[i] = pageItems
        }
        return pages
    }

    private fun getItemAmount(pages: Iterable<T>): Int {
        var amount = 0
        for (t in pages) amount++
        return amount
    }

    // Render the next/prev buttons
    private fun setButtons() {
        val hasPages = pages.size > 1

        // Set previous button
        prevButton = if (hasPages) object : Button() {
            val canGo = currentPage > 1
            override val listener: ButtonListener
                get() = object : ButtonListener {
                    override fun onClickInInterface(
                        player: Player?,
                        ui: UserInterface?,
                        type: ClickType?,
                        event: InventoryClickEvent?
                    ) {
                        if (canGo) {
                            currentPage = MathUtil.range(
                                currentPage - 1, 1,
                                pages.size
                            )
                            updatePage()
                        }
                    }
                }
            override val item: ItemStack
                get() {
                    val str = currentPage - 1
                    return ItemCreator.builder()
                        .material(if (canGo) XMaterial.LIME_DYE else XMaterial.GRAY_DYE)
                        .name(if (str == 0) "&7First Page" else "&8<< &fPage $str")
                        .build().make()
                }
        } else Button.makeEmpty()

        // Set next page button
        nextButton = if (hasPages) object : Button() {
            val canGo = currentPage < pages.size
            override val listener: ButtonListener
                get() = object : ButtonListener {
                    override fun onClickInInterface(
                        player: Player?,
                        ui: UserInterface?,
                        type: ClickType?,
                        event: InventoryClickEvent?
                    ) {
                        if (canGo) {
                            currentPage = MathUtil.range(
                                currentPage + 1, 1,
                                pages.size
                            )
                            updatePage()
                        }
                    }
                }
            override val item: ItemStack
                get() {
                    val last = currentPage == pages.size
                    return ItemCreator.builder()
                        .material(if (canGo) XMaterial.LIME_DYE else XMaterial.GRAY_DYE)
                        .name(if (last) "&7Last Page" else "Page " + (currentPage + 1) + " &8>>")
                        .build().make()
                }
        } else Button.makeEmpty()
    }

    // Reinits the menu and plays the anvil sound
    private fun updatePage() {
        setButtons()
        redraw()
        registerButtons()
    }

    // Compile title and page numbers
    private fun compileTitle(): String {
        val canAddNumbers = addPageNumbers() && pages.size > 1
        return title + if (canAddNumbers) " &8" + currentPage + "/" + pages.size else ""
    }

    /**
     * Automatically prepend the title with page numbers
     *
     *
     * Override for a custom last-minute implementation, but
     * ensure to call the super method otherwise no title will
     * be set in [InterfaceDrawer]
     */
    override fun onDisplay(drawer: InterfaceDrawer) {
        drawer.title = compileTitle()
    }

    /**
     * Return the [ItemStack] representation of an item on a certain page
     *
     *
     * Use [ItemCreator] for easy creation.
     *
     * @param item the given object, for example Arena
     * @return the itemstack, for example diamond sword having arena name
     */
    protected abstract fun convertToItemStack(item: T): ItemStack?

    /**
     * Called automatically when an item is clicked
     *
     * @param player the player who clicked
     * @param item   the clicked item
     * @param click  the click type
     * @param event  the click event
     */
    protected abstract fun onPageClick(player: Player?, item: T, click: ClickType?, event: InventoryClickEvent?)

    /**
     * Utility: Shall we send update packet when the menu is clicked?
     *
     * @return true by default
     */
    protected fun updateButtonOnClick(): Boolean {
        return true
    }

    /**
     * Return true if you want our system to add page/totalPages suffix after
     * your title, true by default
     *
     * @return
     */
    protected fun addPageNumbers(): Boolean {
        return true
    }

    /**
     * Return if there are no items at all
     *
     * @return
     */
    protected val isEmpty: Boolean
        get() = pages.isEmpty() || pages[0]!!.isEmpty()

    /**
     * Automatically get the correct item from the actual page, including
     * prev/next buttons
     *
     * @param slot the slot
     * @return the item, or null
     */
    override fun getItemAt(slot: Int): ItemStack? {
        if (slot < currentPageItems.size) {
            val `object`: T? = currentPageItems[slot]
            if (`object` != null) return convertToItemStack(`object`)
        }
        if (slot == size - 6) return prevButton?.item
        return if (slot == size - 4) nextButton?.item else null
    }

    override fun onInterfaceClick(
        player: Player, slot: Int,
        action: InventoryAction, click: ClickType?,
        cursor: ItemStack?, clicked: ItemStack?,
        cancelled: Boolean, event: InventoryClickEvent
    ) {
        if (slot < currentPageItems.size) {
            val obj: T? = currentPageItems[slot]
            if (obj != null) {
                val prevType = player.openInventory.type
                onPageClick(player, obj, click, event)
                if (updateButtonOnClick()
                    && prevType == player.openInventory.type
                ) player.openInventory.topInventory.setItem(
                    slot,
                    getItemAt(slot)
                )
            }
        }
    }

    public final override fun onInterfaceClick(
        player: Player?, slot: Int,
        clicked: ItemStack?, event: InventoryClickEvent, cancelled: Boolean
    ) {
        throw PluginException("Simplest click unsupported")
    }

    /**
     * Get current items displayed on the page
     */
    private val currentPageItems: List<T>
        get() {
            Valid.checkBoolean(
                pages.containsKey(currentPage - 1),
                "The menu has only " + pages.size + " pages, not "
                        + currentPage + "!"
            )
            return pages[currentPage - 1]!!
        }

    init {
        val items = getItemAmount(pages)
        val autoPageSize = pageSize
            ?: if (items <= 9) 9 * 1 else if (items <= 9 * 2) 9 * 2 else if (items <= 9 * 3) 9 * 3 else if (items <= 9 * 4) 9 * 4 else 9 * 5
        currentPage = 1
        this.pages = fillPages(autoPageSize, pages)
        size = 9 + autoPageSize
        setButtons()
    }
}