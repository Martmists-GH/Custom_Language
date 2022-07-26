package com.martmists.part1

class CalculatorInterpreter {
    fun visit(ast: AST): Float {
        return when (ast) {
            is IntLiteral -> ast.value.toFloat()  // Because we're lazy
            is FloatLiteral -> ast.value
            is BinOp -> {
                val left = visit(ast.left)
                val right = visit(ast.right)
                when (ast.op) {
                    BinOp.Operator.PLUS -> left + right
                    BinOp.Operator.MINUS -> left - right
                    BinOp.Operator.TIMES -> left * right
                    BinOp.Operator.DIVIDE -> left / right
                }
            }
        }
    }
}
