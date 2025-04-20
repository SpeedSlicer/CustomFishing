package com.illuzionzstudios.mist.config.format

import java.io.IOException
import java.io.Writer
import java.util.stream.Collectors

/**
 * This represents a comment in a [com.illuzionzstudios.mist.config.YamlConfig]
 * Usually declared by the "#" char
 */
class Comment(
    var styling: CommentStyle?,
    val lines: List<String?>?
) {

    constructor(vararg lines: String?) : this(null, listOf<String?>(*lines))
    constructor(lines: List<String?>?) : this(null, lines)
    constructor(commentStyle: CommentStyle?, vararg lines: String?) : this(
        commentStyle,
        listOf<String?>(*lines)
    )

    /**
     * @return Convert comment to lines separated by <char>"\n"</char>
     */
    override fun toString(): String {
        return if (lines!!.isEmpty()) "" else java.lang.String.join("\n", lines)
    }

    /**
     * Write our comments to a writer
     *
     * @param output       The writer to output to
     * @param offset       The offset amount of chars indent
     * @param defaultStyle The default styling for comments
     * @throws IOException If couldn't write comments
     */
    @Throws(IOException::class)
    fun writeComment(output: Writer, offset: Int, defaultStyle: CommentStyle?) {
        val style = styling ?: defaultStyle!!
        var minSpacing = 0
        var borderSpacing = 0
        // first draw the top of the comment
        if (style.drawBorder) {
            // grab the longest line in the list of lines
            minSpacing = lines!!.stream().max { s1: String?, s2: String? -> s1!!.length - s2!!.length }.get().length
            borderSpacing = minSpacing + style.commentPrefix.length + style.commentSuffix.length
            // draw the first line
            output.write(
                """
    ${String(CharArray(offset)).replace('\u0000', ' ')}${String(CharArray(borderSpacing + 2)).replace('\u0000', '#')}
    
    """.trimIndent()
            )
            if (style.drawSpace) {
                output.write(
                    """
    ${
                        String(CharArray(offset)).replace(
                            '\u0000',
                            ' '
                        )
                    }#${style.spacePrefixTop}${
                        String(CharArray(borderSpacing - style.spacePrefixTop.length - style.spaceSuffixTop.length)).replace(
                            '\u0000',
                            style.spaceCharTop
                        )
                    }${style.spaceSuffixTop}#
    
    """.trimIndent()
                )
            }
        } else if (style.drawSpace) {
            output.write(
                """
    ${String(CharArray(offset)).replace('\u0000', ' ')}#
    
    """.trimIndent()
            )
        }
        // then the actual comment lines
        for (line in lines!!) {
            // todo? should we auto-wrap comment lines that are longer than 80 characters?
            output.write(
                String(CharArray(offset)).replace('\u0000', ' ') + "#" + style.commentPrefix
                        + (if (minSpacing == 0) line else line + String(CharArray(minSpacing - line!!.length)).replace(
                    '\u0000',
                    ' '
                )) + style.commentSuffix + if (style.drawBorder) "#\n" else "\n"
            )
        }
        // now draw the bottom of the comment border
        if (style.drawBorder) {
            if (style.drawSpace) {
                output.write(
                    """
    ${
                        String(CharArray(offset)).replace(
                            '\u0000',
                            ' '
                        )
                    }#${style.spacePrefixBottom}${
                        String(CharArray(borderSpacing - style.spacePrefixBottom.length - style.spaceSuffixBottom.length)).replace(
                            '\u0000',
                            style.spaceCharBottom
                        )
                    }${style.spaceSuffixBottom}#
    
    """.trimIndent()
                )
            }
            output.write(
                """
    ${String(CharArray(offset)).replace('\u0000', ' ')}${String(CharArray(borderSpacing + 2)).replace('\u0000', '#')}
    
    """.trimIndent()
            )
        } else if (style.drawSpace) {
            output.write(
                """
    ${String(CharArray(offset)).replace('\u0000', ' ')}#
    
    """.trimIndent()
            )
        }
    }

    companion object {
        /**
         * This will load a set of [String] lines into a [Comment] object.
         * Will automatically detect the comment styling so build the right comment
         *
         * @param lines The string lines including styling
         * @return The built [Comment] object
         */
        fun loadComment(lines: List<String>): Comment {
            val style: CommentStyle = CommentStyle.Companion.parseStyle(lines)
            val linePad = (if (style.drawBorder) 1 else 0) + if (style.drawSpace) 1 else 0
            val prefix = style.commentPrefix.length
            val suffix = style.commentSuffix.length
            return Comment(
                style,
                lines.subList(linePad, lines.size - linePad).stream()
                    .map { s: String -> s.substring(prefix, s.length - suffix).trim { it <= ' ' } }
                    .collect(Collectors.toList()))
        }
    }
}