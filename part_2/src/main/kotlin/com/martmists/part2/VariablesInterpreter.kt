package com.martmists.part2

class VariablesInterpreter {
    private val scopeStack = mutableListOf(InterpreterScope(null))
    val currentScope: InterpreterScope
        get() = scopeStack.last()

    init {
        currentScope.apply {
            this["println"] = InterpreterBuiltinFunction { args ->
                println(args.joinToString(", "))
                InterpreterNull
            }
        }
    }

    fun scoped(block: () -> Unit) {
        scopeStack.add(InterpreterScope(currentScope))
        block()
        scopeStack.removeLast()
    }

    fun visit(ast: AST): InterpreterValue {
        return when (ast) {
            is Assign -> {
                currentScope[ast.variable.value] = visit(ast.value)
                InterpreterNull
            }
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
            is Call -> {
                val callee = visit(ast.func)
                val args = ast.args.map { visit(it) }
                callee(this, args)
            }
            is FloatLiteral -> InterpreterFloat(ast.value)
            is Function -> {
                val func = InterpreterUserFunction(ast.args, ast.body)
                currentScope[ast.name] = func
                InterpreterNull
            }
            is IntLiteral -> InterpreterInt(ast.value)
            is Statements -> {
                for (stmt in ast.statements) {
                    val res = visit(stmt)
                    if (stmt is Return) {
                        return res
                    }
                }
                InterpreterNull
            }
            is Variable -> currentScope[ast.value]
            is Return -> visit(ast.value)
        }
    }
}
