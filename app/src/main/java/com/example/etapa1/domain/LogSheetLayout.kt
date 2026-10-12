package com.example.etapa1.domain

/**
 * Superficie donde se dibuja la hoja. En la app la implementa el PDF de Android;
 * en pruebas se puede usar una que solo mide o que guarda una imagen.
 * Todas las medidas están en puntos (1/72 de pulgada).
 */
interface SheetSurface {
    fun measure(text: String, size: Float, bold: Boolean): Float
    fun beginPage()
    fun endPage()
    fun drawRect(left: Float, top: Float, right: Float, bottom: Float, fillColor: Int?, strokeColor: Int?)
    fun drawText(text: String, x: Float, baseline: Float, size: Float, bold: Boolean, color: Int)
}

/**
 * Acomoda la [LogSheet] en páginas tamaño carta horizontal, con el mismo orden y encabezados
 * que la hoja oficial: columna de preguntas a la izquierda y una columna por día (lunes a viernes).
 */
object LogSheetLayout {

    const val PAGE_WIDTH = 792f   // 11 in
    const val PAGE_HEIGHT = 612f  // 8.5 in
    private const val MARGIN = 24f
    private const val BOTTOM_LIMIT = PAGE_HEIGHT - 34f
    private const val LABEL_WIDTH = 214f
    private const val PADDING = 3f
    private const val BODY_SIZE = 6.6f
    private const val LINE_HEIGHT = 8.2f
    private const val MIN_ROW = 12f
    private const val SIGNATURE_ROW = 24f
    private const val SIGNATURE_SPACE = 12f
    private const val BOX = 5.2f
    private const val OPTION_GAP = 5f

    private const val BLACK = 0xFF000000.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val BORDER = 0xFF8C8C8C.toInt()
    private const val SECTION_FILL = 0xFF6D6D6D.toInt()
    private const val SUBSECTION_FILL = 0xFFD9D9D9.toInt()
    private const val HEADER_FILL = 0xFFEDEDED.toInt()
    private const val MUTED = 0xFF6B6B6B.toInt()

    private val contentWidth = PAGE_WIDTH - 2 * MARGIN
    private val dayWidth = (contentWidth - LABEL_WIDTH) / 5f

    /** Dibuja la hoja y devuelve el número de páginas. [footer] recibe (página, total). */
    fun draw(sheet: LogSheet, surface: SheetSurface, footer: (page: Int, total: Int) -> String): Int {
        // Primera pasada solo para contar páginas (el pie dice "Página n de N").
        val total = Renderer(sheet, CountingSurface(surface), footer, totalPages = 0).run()
        return Renderer(sheet, surface, footer, total).run()
    }

    /** Parte un texto en renglones que caben en [width]. */
    fun wrap(text: String, width: Float, size: Float, bold: Boolean, surface: SheetSurface): List<String> {
        if (text.isEmpty()) return listOf("")
        val lines = mutableListOf<String>()
        text.split("\n").forEach { paragraph ->
            var current = ""
            paragraph.split(" ").filter { it.isNotEmpty() }.forEach { word ->
                val candidate = if (current.isEmpty()) word else "$current $word"
                when {
                    surface.measure(candidate, size, bold) <= width -> current = candidate
                    current.isNotEmpty() && surface.measure(word, size, bold) <= width -> {
                        lines += current
                        current = word
                    }
                    else -> {
                        // Palabra más ancha que la celda: se corta por letras.
                        if (current.isNotEmpty()) lines += current
                        current = ""
                        var piece = ""
                        word.forEach { ch ->
                            if (surface.measure(piece + ch, size, bold) > width && piece.isNotEmpty()) {
                                lines += piece
                                piece = ""
                            }
                            piece += ch
                        }
                        current = piece
                    }
                }
            }
            lines += current
        }
        return lines
    }

    private class CountingSurface(private val measurer: SheetSurface) : SheetSurface {
        override fun measure(text: String, size: Float, bold: Boolean) = measurer.measure(text, size, bold)
        override fun beginPage() = Unit
        override fun endPage() = Unit
        override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, fillColor: Int?, strokeColor: Int?) = Unit
        override fun drawText(text: String, x: Float, baseline: Float, size: Float, bold: Boolean, color: Int) = Unit
    }

    private class Renderer(
        private val sheet: LogSheet,
        private val s: SheetSurface,
        private val footer: (Int, Int) -> String,
        private val totalPages: Int
    ) {
        private var page = 0
        private var y = 0f

        fun run(): Int {
            sheet.pages.forEachIndexed { index, sheetPage ->
                startPage(includeGeneralData = index == 0)
                sheetPage.sections.forEachIndexed { sectionIndex, section ->
                    val isMain = section.title.matches(Regex("^[IV]+\\..*"))
                    // El encabezado de días va después del primer apartado principal de cada página.
                    ensureSpace(14f + (if (isMain) 14f else 0f) + MIN_ROW)
                    sectionBar(section.title, isMain)
                    if (isMain) dayHeader()
                    section.rows.forEach { row ->
                        val height = rowHeight(row)
                        if (y + height > BOTTOM_LIMIT) {
                            finishPage()
                            startPage(includeGeneralData = false)
                            sectionBar("${section.title} (continuación)", isMain)
                            dayHeader()
                        }
                        drawRow(row, height)
                    }
                    if (sectionIndex == sheetPage.sections.lastIndex) y += 4f
                }
                finishPage()
            }
            return page
        }

        private fun startPage(includeGeneralData: Boolean) {
            page += 1
            s.beginPage()
            y = MARGIN
            sheet.titleLines.forEach { line ->
                centered(line, MARGIN, contentWidth, y + 9f, 8.6f, true, BLACK)
                y += 11f
            }
            y += 4f
            if (includeGeneralData) generalData()
        }

        private fun finishPage() {
            val text = footer(page, totalPages)
            s.drawText(text, MARGIN, PAGE_HEIGHT - 18f, 6f, false, MUTED)
            s.endPage()
        }

        private fun ensureSpace(needed: Float) {
            if (y + needed > BOTTOM_LIMIT) {
                finishPage()
                startPage(includeGeneralData = false)
            }
        }

        private fun generalData() {
            sectionBar("I. Datos Generales", true)
            val height = 16f
            val estanciaW = contentWidth * 0.40f
            val nameW = contentWidth * 0.45f
            val groupW = contentWidth - estanciaW - nameW
            cell(MARGIN, estanciaW, height, null)
            labeled("Estancia Infantil: ", sheet.estancia, MARGIN + PADDING, y + 10.5f, estanciaW - 2 * PADDING)
            cell(MARGIN + estanciaW, nameW, height, null)
            labeled(
                "Nombre completo de la niña o niño: ", sheet.childName,
                MARGIN + estanciaW + PADDING, y + 10.5f, nameW - 2 * PADDING
            )
            val groupX = MARGIN + estanciaW + nameW
            cell(groupX, groupW, height, null)
            s.drawText("Grupo:", groupX + PADDING, y + 10.5f, BODY_SIZE + 0.4f, true, BLACK)
            var letterX = groupX + PADDING + s.measure("Grupo:", BODY_SIZE + 0.4f, true) + 8f
            listOf("L", "M", "P").forEach { letter ->
                val selected = letter == sheet.group
                s.drawRect(letterX - 2f, y + 3f, letterX + 9f, y + 13f, if (selected) SUBSECTION_FILL else null, BLACK)
                centered(letter, letterX - 2f, 11f, y + 10.5f, BODY_SIZE + 0.4f, true, BLACK)
                if (selected) {
                    // Marca visible también al imprimir en blanco y negro.
                    s.drawRect(letterX - 3.5f, y + 1.5f, letterX + 10.5f, y + 14.5f, null, BLACK)
                }
                letterX += 18f
            }
            y += height + 6f
        }

        private fun labeled(label: String, value: String, x: Float, baseline: Float, maxWidth: Float) {
            val size = BODY_SIZE + 0.4f
            s.drawText(label, x, baseline, size, true, BLACK)
            val labelWidth = s.measure(label, size, true) + 3f
            val fitted = fit(value, maxWidth - labelWidth, size)
            s.drawText(fitted, x + labelWidth, baseline, size, false, BLACK)
        }

        private fun fit(text: String, width: Float, size: Float): String {
            if (s.measure(text, size, false) <= width) return text
            var cut = text
            while (cut.isNotEmpty() && s.measure("$cut…", size, false) > width) cut = cut.dropLast(1)
            return "$cut…"
        }

        private fun sectionBar(title: String, isMain: Boolean) {
            val height = if (isMain) 13f else 11f
            s.drawRect(MARGIN, y, MARGIN + contentWidth, y + height, if (isMain) SECTION_FILL else SUBSECTION_FILL, BORDER)
            if (isMain) {
                s.drawText(title, MARGIN + PADDING + 1f, y + 9.4f, 8f, true, WHITE)
            } else {
                centered(title, MARGIN, contentWidth, y + 8f, 7.2f, true, BLACK)
            }
            y += height
        }

        /** Encabezado de cada columna: "Fecha: dd/mm/aaaa" y L M M J V con el día marcado. */
        private fun dayHeader() {
            val height = 13f
            val size = 6.2f
            cell(MARGIN, LABEL_WIDTH, height, HEADER_FILL)
            s.drawText(fit(sheet.weekLabel, LABEL_WIDTH - 2 * PADDING, 7f), MARGIN + PADDING, y + 9f, 7f, true, BLACK)
            sheet.dayHeaders.forEachIndexed { i, header ->
                val x = MARGIN + LABEL_WIDTH + i * dayWidth
                cell(x, dayWidth, height, HEADER_FILL)
                // "Fecha:" en negritas y la fecha normal, para que quepan las letras del día.
                val label = header.substringBefore(" ", header) + " "
                s.drawText(label, x + PADDING, y + 9f, size, true, BLACK)
                s.drawText(header.removePrefix(label), x + PADDING + s.measure(label, size, true), y + 9f, size, false, BLACK)
                val letterW = 6.6f
                var lx = x + dayWidth - PADDING - letterW * LogSheetFormatter.DAY_LETTERS.size
                LogSheetFormatter.DAY_LETTERS.forEachIndexed { li, letter ->
                    if (li == i) s.drawRect(lx, y + 2.5f, lx + letterW, y + 10.5f, SUBSECTION_FILL, BLACK)
                    centered(letter, lx, letterW, y + 9f, size, li == i, BLACK)
                    lx += letterW
                }
            }
            y += height
        }

        private val cellTextWidth get() = dayWidth - 2 * PADDING

        private fun rowHeight(row: LogSheetRow): Float {
            if (row.isCaption) return MIN_ROW
            val labelLines = wrap(row.label, LABEL_WIDTH - 2 * PADDING, BODY_SIZE, false, s).size
            val valueLines = row.cells.maxOfOrNull { cellLines(it) } ?: 1
            val lines = maxOf(labelLines, valueLines)
            val height = maxOf(MIN_ROW, lines * LINE_HEIGHT + 4f)
            return if (row.isSignature) maxOf(SIGNATURE_ROW, height + SIGNATURE_SPACE) else height
        }

        private fun cellLines(cell: LogSheetCell): Int = when (cell) {
            is LogSheetCell.Text -> wrap(cell.text, cellTextWidth, BODY_SIZE, false, s).size
            is LogSheetCell.Choice -> choiceLayout(cell).maxOf { it.line } + 1
        }

        private fun drawRow(row: LogSheetRow, height: Float) {
            if (row.isCaption) {
                cell(MARGIN, contentWidth, height, null)
                s.drawText(row.label, MARGIN + PADDING, y + 8.4f, BODY_SIZE, true, BLACK)
                y += height
                return
            }
            cell(MARGIN, LABEL_WIDTH, height, null)
            lines(wrap(row.label, LABEL_WIDTH - 2 * PADDING, BODY_SIZE, false, s), MARGIN + PADDING, BLACK)
            row.cells.forEachIndexed { i, content ->
                val x = MARGIN + LABEL_WIDTH + i * dayWidth
                cell(x, dayWidth, height, null)
                when (content) {
                    is LogSheetCell.Text -> lines(wrap(content.text, cellTextWidth, BODY_SIZE, false, s), x + PADDING, BLACK)
                    is LogSheetCell.Choice -> drawChoice(content, x + PADDING)
                }
                if (row.isSignature) {
                    // Renglón para firmar a mano.
                    val lineY = y + height - 4f
                    s.drawRect(x + 8f, lineY, x + dayWidth - 8f, lineY + 0.3f, MUTED, null)
                }
            }
            y += height
        }

        private class OptionSpot(val text: String, val x: Float, val line: Int, val selected: Boolean)

        /** Acomoda las opciones en renglones: casilla + texto, una tras otra. */
        private fun choiceLayout(choice: LogSheetCell.Choice): List<OptionSpot> {
            val spots = mutableListOf<OptionSpot>()
            var x = 0f
            var line = 0
            choice.options.forEachIndexed { index, option ->
                val width = BOX + 2f + s.measure(option, BODY_SIZE, index == choice.selected)
                if (x > 0f && x + width > cellTextWidth) {
                    line += 1
                    x = 0f
                }
                spots += OptionSpot(option, x, line, index == choice.selected)
                x += width + OPTION_GAP
            }
            return spots
        }

        private fun drawChoice(choice: LogSheetCell.Choice, left: Float) {
            choiceLayout(choice).forEach { spot ->
                val baseline = y + 2f + LINE_HEIGHT * (spot.line + 1) - 1.6f
                val boxLeft = left + spot.x
                val boxTop = baseline - BOX + 0.6f
                s.drawRect(boxLeft, boxTop, boxLeft + BOX, boxTop + BOX, if (spot.selected) BLACK else null, BLACK)
                if (spot.selected) {
                    // "X" blanca dentro de la casilla negra: se ve también impreso en blanco y negro.
                    centered("x", boxLeft, BOX, baseline - 0.4f, BOX + 0.6f, true, WHITE)
                }
                s.drawText(spot.text, boxLeft + BOX + 2f, baseline, BODY_SIZE, spot.selected, BLACK)
            }
        }

        private fun lines(lines: List<String>, x: Float, color: Int) {
            lines.forEachIndexed { index, line ->
                s.drawText(line, x, y + 2f + LINE_HEIGHT * (index + 1) - 1.6f, BODY_SIZE, false, color)
            }
        }

        private fun cell(x: Float, width: Float, height: Float, fill: Int?) {
            s.drawRect(x, y, x + width, y + height, fill, BORDER)
        }

        private fun centered(text: String, x: Float, width: Float, baseline: Float, size: Float, bold: Boolean, color: Int) {
            val textWidth = s.measure(text, size, bold)
            s.drawText(text, x + (width - textWidth) / 2f, baseline, size, bold, color)
        }
    }
}
