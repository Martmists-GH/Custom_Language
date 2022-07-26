package com.martmists.part1

sealed interface AST

sealed interface NumberLiteral: AST
data class IntLiteral(val value: Int) : NumberLiteral
data class FloatLiteral(val value: Float) : NumberLiteral

data class BinOp(val left: AST, val op: Operator, val right: AST) : AST {
    enum class Operator {
        PLUS, MINUS, TIMES, DIVIDE
    }
}
