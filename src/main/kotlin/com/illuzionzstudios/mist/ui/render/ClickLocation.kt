package com.illuzionzstudios.mist.ui.render

/**
 * Places the player can click when having an open inventory
 */
enum class ClickLocation {
    /**
     * Clicked in our custom [com.illuzionzstudios.mist.ui.UserInterface]
     */
    INTERFACE,

    /**
     * Clicked in the bottom player inventory
     */
    PLAYER_INVENTORY,

    /**
     * Didn't click in a inventory at all, could produce null
     */
    OUTSIDE
}