package com.illuzionzstudios.mist.config.format

/**
 * These are the different ways to display a [Comment]
 */
enum class CommentStyle {
    /**
     * # Comment
     */
    SIMPLE(false, false, " ", ""),

    /**
     * #           <br></br>
     * # Comment   <br></br>
     * #           <br></br>
     */
    SPACED(false, true, " ", ""),

    /**
     * ########### <br></br>
     * # Comment # <br></br>
     * ########### <br></br>
     */
    BLOCKED(true, false, " ", " "),

    /**
     * ############# <br></br>
     * #|¯¯¯¯¯¯¯¯¯|# <br></br>
     * #| Comment |# <br></br>
     * #|_________|# <br></br>
     * ############# <br></br>
     */
    BLOCKSPACED(true, true, "|\u00AF", '\u00AF', "\u00AF|", "| ", " |", "|_", '_', "_|");

    /**
     * Store the different options in order to draw the comment styling
     */
    val drawBorder: Boolean
    val drawSpace: Boolean
    val commentPrefix: String
    val spacePrefixTop: String
    val spacePrefixBottom: String
    val commentSuffix: String
    val spaceSuffixTop: String
    val spaceSuffixBottom: String
    val spaceCharTop: Char
    val spaceCharBottom: Char

    constructor(
        drawBorder: Boolean, drawSpace: Boolean,
        spacePrefixTop: String, spaceCharTop: Char, spaceSuffixTop: String,
        commentPrefix: String, commentSuffix: String,
        spacePrefixBottom: String, spaceCharBottom: Char, spaceSuffixBottom: String
    ) {
        this.drawBorder = drawBorder
        this.drawSpace = drawSpace
        this.commentPrefix = commentPrefix
        this.spacePrefixTop = spacePrefixTop
        this.spacePrefixBottom = spacePrefixBottom
        this.commentSuffix = commentSuffix
        this.spaceSuffixTop = spaceSuffixTop
        this.spaceSuffixBottom = spaceSuffixBottom
        this.spaceCharTop = spaceCharTop
        this.spaceCharBottom = spaceCharBottom
    }

    constructor(drawBorder: Boolean, drawSpace: Boolean, commentPrefix: String, commentSuffix: String) {
        this.drawBorder = drawBorder
        this.drawSpace = drawSpace
        this.commentPrefix = commentPrefix
        this.commentSuffix = commentSuffix
        spacePrefixBottom = ""
        spacePrefixTop = spacePrefixBottom
        spaceCharBottom = ' '
        spaceCharTop = spaceCharBottom
        spaceSuffixBottom = ""
        spaceSuffixTop = spaceSuffixBottom
    }

    companion object {
        /**
         * An easy way to detect what type of styling
         * a comment has
         *
         * @param lines The lines with styling
         * @return The relevant comment styling
         */
        fun parseStyle(lines: List<String>?): CommentStyle {
            if (lines == null || lines.size <= 2) {
                return SIMPLE
            } else if (lines[0].trim { it <= ' ' } == "#" && lines[lines.size - 1].trim { it <= ' ' } == "#") {
                return SPACED
            }
            val hasBorders =
                lines[0].trim { it <= ' ' }.matches(Regex("^##+$")) && lines[lines.size - 1].trim { it <= ' ' }
                    .matches(Regex("^##+$"))
            if (!hasBorders) {
                // default return
                return SIMPLE
            }
            // now need to figure out if this is blocked or not
            val replace = ("^#"
                    + BLOCKSPACED.spacePrefixTop + BLOCKSPACED.spaceCharTop + "+"
                    + BLOCKSPACED.spaceSuffixTop + "#$").replace("|", "\\|")
            return if (lines.size > 4 && lines[1].trim { it <= ' ' }.matches(Regex(replace))
                && lines[1].trim { it <= ' ' }.matches(Regex(replace))
            ) {
                BLOCKSPACED
            } else BLOCKED
        }
    }
}