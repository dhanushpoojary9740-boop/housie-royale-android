
package com.houseroyale.app

typealias TambolaTicket = List<List<Int?>>

enum class WinningPattern {
    EARLY_FIVE,
    TOP_LINE,
    MIDDLE_LINE,
    BOTTOM_LINE,
    FOUR_CORNERS,
    FULL_HOUSE
}

object TambolaGameEngine {

    private const val ROWS = 3
    private const val COLUMNS = 9
    private const val NUMBERS_PER_ROW = 5
    private const val TOTAL_NUMBERS = 15

    val supportedBundleSizes = listOf(1, 6, 12)

    private fun columnRange(column: Int): IntRange {
        return when (column) {
            0 -> 1..9
            8 -> 80..90
            else -> (column * 10)..(column * 10 + 9)
        }
    }

    fun generateTicket(): TambolaTicket {

        while (true) {

            val positions = List(ROWS) {
                (0 until COLUMNS)
                    .shuffled()
                    .take(NUMBERS_PER_ROW)
                    .toSet()
            }

            val columnCounts = (0 until COLUMNS).map { column ->
                positions.count { column in it }
            }

            if (columnCounts.any { it == 0 || it > ROWS }) {
                continue
            }

            val ticket = MutableList(ROWS) {
                MutableList<Int?>(COLUMNS) { null }
            }

            for (column in 0 until COLUMNS) {

                val occupiedRows = (0 until ROWS)
                    .filter { column in positions[it] }
                    .sorted()

                val numbers = columnRange(column)
                    .shuffled()
                    .take(occupiedRows.size)
                    .sorted()

                occupiedRows.forEachIndexed { index, row ->
                    ticket[row][column] = numbers[index]
                }
            }

            val result = ticket.map { it.toList() }

            if (isValidTicket(result)) {
                return result
            }
        }
    }

    fun generateTickets(count: Int): List<TambolaTicket> {

        require(count in supportedBundleSizes) {
            "Bundle size must be 1, 6 or 12"
        }

        val tickets = mutableListOf<TambolaTicket>()
        val signatures = mutableSetOf<String>()

        while (tickets.size < count) {

            val ticket = generateTicket()

            val signature = ticket
                .flatten()
                .filterNotNull()
                .sorted()
                .joinToString(",")

            if (signatures.add(signature)) {
                tickets.add(ticket)
            }
        }

        return tickets
    }

    fun isValidTicket(ticket: TambolaTicket): Boolean {

        if (ticket.size != ROWS) return false

        if (ticket.any { it.size != COLUMNS }) return false

        val allNumbers = ticket.flatten().filterNotNull()

        if (allNumbers.size != TOTAL_NUMBERS) return false

        if (allNumbers.distinct().size != TOTAL_NUMBERS) {
            return false
        }

        if (ticket.any { row ->
                row.count { it != null } != NUMBERS_PER_ROW
            }
        ) return false

        for (column in 0 until COLUMNS) {

            val values = ticket.mapNotNull { it[column] }

            if (values.isEmpty() || values.size > ROWS) {
                return false
            }

            if (values != values.sorted()) {
                return false
            }

            if (values.any { it !in columnRange(column) }) {
                return false
            }
        }

        return true
    }

    fun completedPatterns(
        ticket: TambolaTicket,
        calledNumbers: Set<Int>
    ): Set<WinningPattern> {

        require(isValidTicket(ticket)) {
            "Invalid Tambola ticket"
        }

        val markedNumbers = ticket
            .flatten()
            .filterNotNull()
            .filter { it in calledNumbers }

        val completed = mutableSetOf<WinningPattern>()

        if (markedNumbers.size >= 5) {
            completed.add(WinningPattern.EARLY_FIVE)
        }

        val linePatterns = listOf(
            WinningPattern.TOP_LINE,
            WinningPattern.MIDDLE_LINE,
            WinningPattern.BOTTOM_LINE
        )

        ticket.forEachIndexed { rowIndex, row ->

            val rowNumbers = row.filterNotNull()

            if (rowNumbers.all { it in calledNumbers }) {
                completed.add(linePatterns[rowIndex])
            }
        }

        val topRow = ticket.first().filterNotNull()
        val bottomRow = ticket.last().filterNotNull()

        val fourCorners = listOf(
            topRow.first(),
            topRow.last(),
            bottomRow.first(),
            bottomRow.last()
        )

        if (fourCorners.all { it in calledNumbers }) {
            completed.add(WinningPattern.FOUR_CORNERS)
        }

        if (markedNumbers.size == TOTAL_NUMBERS) {
            completed.add(WinningPattern.FULL_HOUSE)
        }

        return completed
    }

    fun getRemainingNumbers(
        calledNumbers: Set<Int>
    ): List<Int> {
        return (1..90).filterNot { it in calledNumbers }
    }

    fun callNextNumber(
        calledNumbers: Set<Int>
    ): Int? {
        return getRemainingNumbers(calledNumbers).randomOrNull()
    }
}
