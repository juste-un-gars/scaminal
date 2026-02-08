/**
 * @file AnsiParser.kt
 * @description Parser ANSI escape codes pour affichage coloré du terminal SSH.
 *              Machine à états : NORMAL → ESCAPE → CSI, conserve l'état entre les chunks.
 */
package com.scaminal.ssh

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import javax.inject.Inject
import javax.inject.Singleton

/** Un segment de texte avec ses attributs visuels. */
data class TerminalSpan(
    val text: String,
    val foreground: Color? = null,
    val background: Color? = null,
    val isBold: Boolean = false,
    val isUnderline: Boolean = false
)

@Singleton
class AnsiParser @Inject constructor() {

    private enum class State { NORMAL, ESCAPE, CSI }

    private var state = State.NORMAL
    private var csiBuffer = StringBuilder()
    private var currentFg: Color? = null
    private var currentBg: Color? = null
    private var currentBold = false
    private var currentUnderline = false

    /**
     * Parse un chunk de texte brut contenant potentiellement des codes ANSI.
     *
     * @param raw Le texte brut reçu du terminal
     * @return Liste de [TerminalSpan] avec attributs visuels
     */
    fun parse(raw: String): List<TerminalSpan> {
        val spans = mutableListOf<TerminalSpan>()
        val textBuffer = StringBuilder()

        for (char in raw) {
            when (state) {
                State.NORMAL -> {
                    if (char == '\u001B') {
                        if (textBuffer.isNotEmpty()) {
                            spans.add(makeSpan(textBuffer.toString()))
                            textBuffer.clear()
                        }
                        state = State.ESCAPE
                    } else {
                        textBuffer.append(char)
                    }
                }
                State.ESCAPE -> {
                    when (char) {
                        '[' -> {
                            state = State.CSI
                            csiBuffer.clear()
                        }
                        ']' -> {
                            // OSC sequence — skip until ST or BEL
                            state = State.NORMAL
                        }
                        else -> {
                            // Unknown escape, return to normal
                            state = State.NORMAL
                        }
                    }
                }
                State.CSI -> {
                    if (char in '0'..'9' || char == ';') {
                        csiBuffer.append(char)
                    } else {
                        if (char == 'm') {
                            applySgr(csiBuffer.toString())
                        }
                        // All other CSI commands (cursor, clear, etc.) are stripped
                        state = State.NORMAL
                        csiBuffer.clear()
                    }
                }
            }
        }

        if (textBuffer.isNotEmpty()) {
            spans.add(makeSpan(textBuffer.toString()))
        }

        return spans
    }

    /** Convertit une liste de spans en AnnotatedString pour Compose. */
    fun toAnnotatedString(spans: List<TerminalSpan>): AnnotatedString = buildAnnotatedString {
        for (span in spans) {
            val style = SpanStyle(
                color = span.foreground ?: Color.Unspecified,
                background = span.background ?: Color.Transparent,
                fontWeight = if (span.isBold) FontWeight.Bold else FontWeight.Normal,
                textDecoration = if (span.isUnderline) TextDecoration.Underline else TextDecoration.None
            )
            pushStyle(style)
            append(span.text)
            pop()
        }
    }

    /** Remet le parser dans son état initial. */
    fun reset() {
        state = State.NORMAL
        csiBuffer.clear()
        currentFg = null
        currentBg = null
        currentBold = false
        currentUnderline = false
    }

    private fun makeSpan(text: String) = TerminalSpan(
        text = text,
        foreground = currentFg,
        background = currentBg,
        isBold = currentBold,
        isUnderline = currentUnderline
    )

    private fun applySgr(params: String) {
        val codes = if (params.isEmpty()) listOf(0) else {
            params.split(';').mapNotNull { it.toIntOrNull() }
        }
        for (code in codes) {
            when (code) {
                0 -> { currentFg = null; currentBg = null; currentBold = false; currentUnderline = false }
                1 -> currentBold = true
                4 -> currentUnderline = true
                22 -> currentBold = false
                24 -> currentUnderline = false
                in 30..37 -> currentFg = standardColor(code - 30, currentBold)
                39 -> currentFg = null
                in 40..47 -> currentBg = standardColor(code - 40, false)
                49 -> currentBg = null
                in 90..97 -> currentFg = brightColor(code - 90)
                in 100..107 -> currentBg = brightColor(code - 100)
            }
        }
    }

    companion object {
        private val STANDARD_COLORS = arrayOf(
            Color(0xFF000000), // Black
            Color(0xFFCC0000), // Red
            Color(0xFF00CC00), // Green
            Color(0xFFCCCC00), // Yellow
            Color(0xFF0000CC), // Blue
            Color(0xFFCC00CC), // Magenta
            Color(0xFF00CCCC), // Cyan
            Color(0xFFCCCCCC)  // White
        )

        private val BRIGHT_COLORS = arrayOf(
            Color(0xFF555555), // Bright Black
            Color(0xFFFF5555), // Bright Red
            Color(0xFF55FF55), // Bright Green
            Color(0xFFFFFF55), // Bright Yellow
            Color(0xFF5555FF), // Bright Blue
            Color(0xFFFF55FF), // Bright Magenta
            Color(0xFF55FFFF), // Bright Cyan
            Color(0xFFFFFFFF)  // Bright White
        )

        private fun standardColor(index: Int, bold: Boolean): Color =
            if (bold) BRIGHT_COLORS[index] else STANDARD_COLORS[index]

        private fun brightColor(index: Int): Color = BRIGHT_COLORS[index]
    }
}
