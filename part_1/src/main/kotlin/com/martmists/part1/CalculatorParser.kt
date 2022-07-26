package com.martmists.part1

import com.martmists.kotpack.GrammarParser

class CalculatorParser(input: String) : GrammarParser<AST>(input.replace(Regex("\\s+"), "")) {
    private val integer by regex("\\d+") {
        IntLiteral(it.toInt())
    }.memo()
    private val float by regex("\\d+\\.\\d+") {
        FloatLiteral(it.toFloat())
    }.memo()
    private val number by first(
        ::float,
        ::integer,
    ).memo()
    private val atom by first(
        firstBlock {
            char('-')
            when (val x = number()) {
                is IntLiteral -> IntLiteral(-x.value)
                is FloatLiteral -> FloatLiteral(-x.value)
            }
        },
        ::number,
    ).memo()
    private val factor: () -> AST by first(
        firstBlock {
            char('(')
            val x = expression()
            char(')')
            x
        },
        ::atom,
    ).memo()
    private val term: () -> AST by first(
        firstBlock {
            val left = term()
            val op = when(val x = regex("[*/]").first()) {
                '*' -> BinOp.Operator.TIMES
                '/' -> BinOp.Operator.DIVIDE
                else -> throw IllegalStateException("Unexpected operator $x")
            }
            val right = factor()
            BinOp(left, op, right)
        },
        ::factor,
    ).memoLeft()
    private val expression: () -> AST by first(
        firstBlock {
            val left = expression()
            val op = when(val x = regex("[+-]").first()) {
                '+' -> BinOp.Operator.PLUS
                '-' -> BinOp.Operator.MINUS
                else -> throw IllegalStateException("Unexpected operator $x")
            }
            val right = term()
            BinOp(left, op, right)
        },
        ::term,
    ).memoLeft()

    override val root: () -> AST
        get() = expression
}
