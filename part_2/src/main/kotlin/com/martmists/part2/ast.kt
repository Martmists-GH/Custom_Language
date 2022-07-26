package com.martmists.part2

sealed interface AST

data class IntLiteral(val value: Int) : AST
data class FloatLiteral(val value: Float) : AST

data class Variable(val value: String) : AST

data class BinOp(val left: AST, val op: Operator, val right: AST) : AST {
    enum class Operator {
        PLUS, MINUS, TIMES, DIVIDE
    }
}

data class Statements(val statements: List<AST>) : AST

data class Assign(val variable: Variable, val value: AST) : AST

data class Call(val func: AST, val args: List<AST>) : AST

data class Return(val value: AST) : AST

data class Function(val name: String, val args: List<String>, val body: Statements) : AST
