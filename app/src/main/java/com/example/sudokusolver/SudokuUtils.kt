package com.example.sudokusolver

import java.lang.Integer.min
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.random.nextInt


class Action(var row: Int, var column: Int, var number: Int)

val actions = ArrayList<Action>()
val sudokuNumbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)

fun checkValidRowSudoku(board: Array<Array<Int>>, row: Int, acceptEmpty: Boolean = false): Boolean {
    val list = board[row].toMutableList()

    if (acceptEmpty) {
        list.removeAll { it == 0 }
    }

    sudokuNumbers.forEach {
        list.remove(it)
    }

    return list.isEmpty()
}

fun checkValidColumnSudoku(
    board: Array<Array<Int>>,
    column: Int,
    acceptEmpty: Boolean = false
): Boolean {
    val list = board.map { it[column] }.toMutableList()

    if (acceptEmpty) {
        list.removeAll { it == 0 }
    }

    sudokuNumbers.forEach {
        list.remove(it)
    }

    return list.isEmpty()
}

fun checkValidBlockSudoku(
    board: Array<Array<Int>>,
    row: Int,
    column: Int,
    acceptEmpty: Boolean = false
): Boolean {
    val rowStart = (row / 3) * 3
    val rowEnd = rowStart + 2
    val columnStart = (column / 3) * 3
    val columnEnd = columnStart + 2

    val list = ArrayList<Int>()

    for (i in rowStart..rowEnd) {
        for (j in columnStart..columnEnd) {
            list.add(board[i][j])
        }
    }

    if (acceptEmpty) {
        list.removeAll { it == 0 }
    }

    sudokuNumbers.forEach {
        list.remove(it)
    }

    return list.isEmpty()
}

fun isValidSudokuCell(
    board: Array<Array<Int>>,
    row: Int,
    column: Int,
    acceptEmpty: Boolean
): Boolean {
    return checkValidRowSudoku(board, row, acceptEmpty) &&
            checkValidColumnSudoku(board, column, acceptEmpty) &&
            checkValidBlockSudoku(board, row, column, acceptEmpty)
}

fun isValidSudokuAll(board: Array<Array<Int>>, acceptEmpty: Boolean = false): Boolean {
    val size = sqrt(board.size.toDouble()).toInt()
    repeat(size) { i ->
        repeat(size) { j ->
            val index = i * size + j
            if (!checkValidRowSudoku(board, index, acceptEmpty) ||
                !checkValidColumnSudoku(board, index, acceptEmpty) ||
                !checkValidBlockSudoku(board, 3 * i, 3 * j, acceptEmpty)
            ) {
                return false
            }
        }
    }
    return true
}

fun solveSudokuDriver(grid: Array<Array<Int>>): Boolean {
    actions.clear()
    return solveSudoku(grid, 0, 0).also {
        actions.reverse()
    }
}

fun solveSudoku(grid: Array<Array<Int>>, row: Int, col: Int): Boolean {
    var row = row
    var col = col
    if (row == grid.size - 1 && col == grid[0].size) return true
    if (col == grid[0].size) {
        row++
        col = 0
    }
    if (grid[row][col] != 0) return solveSudoku(grid, row, col + 1)
    for (num in 1..9) {
        if (isValidNumber(grid, row, col, num)) {
            grid[row][col] = num
            if (solveSudoku(grid, row, col + 1)) {
                actions.add(Action(row, col, num))
                return true
            }
        }
        grid[row][col] = 0
    }
    return false
}

fun isValidNumber(grid: Array<Array<Int>>, row: Int, col: Int, num: Int): Boolean {
    for (x in 0..8) if (grid[row][x] == num) return false
    for (x in 0..8) if (grid[x][col] == num) return false

    val startRow = row - row % 3
    val startCol = col - col % 3
    for (i in 0..2) for (j in 0..2) if (grid[i + startRow][j + startCol] == num) return false
    return true
}

class Sudoku internal constructor(
    var dimension: Int, var emptyCellsCount: Int
)
{
    var grid: Array<Array<Int>>
    var dimensionSquare: Int

    init {
        val dimensionSquareDouble = sqrt(dimension.toDouble())
        dimensionSquare = dimensionSquareDouble.toInt()
        grid = Array(dimension) { Array(dimension) { 0 } }
    }

    fun generateRandom() {
        fillDiagonal()
        fillRemaining(0, dimensionSquare)
        removeCells()
    }

    fun fillDiagonal() {
        var i = 0
        while (i < dimension) {
            fillBox(i, i)
            i += dimensionSquare
        }
    }

    fun unUsedInBox(rowStart: Int, colStart: Int, num: Int): Boolean {
        for (i in 0 until dimensionSquare)
            for (j in 0 until dimensionSquare)
                if (grid[rowStart + i][colStart + j] == num)
                    return false
        return true
    }

    fun fillBox(row: Int, col: Int) {
        var num: Int
        for (i in 0 until dimensionSquare) {
            for (j in 0 until dimensionSquare) {
                do {
                    num = Random.nextInt(1..dimension)
                } while (!unUsedInBox(row, col, num))
                grid[row + i][col + j] = num
            }
        }
    }

    fun CheckIfSafe(i: Int, j: Int, num: Int): Boolean {
        return unUsedInRow(i, num) && unUsedInCol(j, num) && unUsedInBox(
            i - i % dimensionSquare,
            j - j % dimensionSquare,
            num
        )
    }

    fun unUsedInRow(i: Int, num: Int): Boolean {
        for (j in 0 until dimension)
            if (grid[i][j] == num)
                return false
        return true
    }

    fun unUsedInCol(j: Int, num: Int): Boolean {
        for (i in 0 until dimension)
            if (grid[i][j] == num)
                return false
        return true
    }

    fun fillRemaining(i: Int, j: Int): Boolean {
        // System.out.println(i+" "+j);
        var i = i
        var j = j
        if (j >= dimension && i < dimension - 1) {
            i += 1
            j = 0
        }
        if (i >= dimension && j >= dimension)
            return true
        if (i < dimensionSquare) {
            if (j < dimensionSquare) j = dimensionSquare
        } else if (i < dimension - dimensionSquare) {
            if (j == (i / dimensionSquare) * dimensionSquare) j += dimensionSquare
        } else {
            if (j == dimension - dimensionSquare) {
                i += 1
                j = 0
                if (i >= dimension)
                    return true
            }
        }
        for (num in 1..dimension) {
            if (CheckIfSafe(i, j, num)) {
                grid[i][j] = num
                if (fillRemaining(i, j + 1))
                    return true
                grid[i][j] = 0
            }
        }
        return false
    }

    fun removeCells() {
        val fillCells = grid.sumOf { it.count { it in 1..9 } }
        repeat(min(emptyCellsCount, fillCells)) {
            while (true) {
                val row = Random.nextInt(0, dimension)
                val column = Random.nextInt(0, dimension)
                if (grid[row][column] != 0) {
                    grid[row][column] = 0
                    break
                }
            }
        }
    }

    fun printSudoku() {
        for (i in 0 until dimension) {
            for (j in 0 until dimension)
                print(grid[i][j].toString() + " ")
            println()
        }
        println()
    }

    companion object {
        fun generateSudoku(k: Int): Array<Array<Int>> {
            val sudoku = Sudoku(9, k)
            sudoku.generateRandom()
            return sudoku.grid
        }
    }

}