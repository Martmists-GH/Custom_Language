# Part 2: Variables and functions

Let's start by modifying our grammar to allow for variables and functions.
For variables, we need to use them as a value, and assign them a value.
Functions need to be defined and be called, and need to be able to return a value.

### Writing the parsing-related code

Let's do the simple part; Calling functions and getting variables as values:
```
variable   := r"[a-zA-Z_][a-zA-Z\d_]*"
atom       := variable
            | number
            | '(' expression ')'
primary    := primary '(' arguments? ')'
            | atom
factor     := '-' primary
            | primary
arguments  := expression (',' expression)*
parameters := variable (',' variable)*
```

Next, we'll need to add statements
```
assignment   := variable '=' expression
functiondef  := 'function' variable '(' parameters ')' '{' statements '}' 
statement    := assignment
              | functiondef
              | expression
functionStmt := 'return' expression  # custom statements type for functions to prevent top-level returns
              | statement
statements   := (statement ';')*
funcStmts    := (functionStmt ';')*
```

Note that this grammar doesn require you to put a semicolon at the end of a function definition. While ideally this 
shouldn't be the case, I decided to keep it this way for now to keep it simple.  
Next up, our AST dataclasses:

```kotlin
data class Statements(val statements: List<AST>) : AST

data class Assign(val variable: Variable, val value: AST) : AST

data class Call(val func: AST, val args: List<AST>) : AST

data class Return(val value: AST) : AST

data class Function(val name: String, val args: List<String>, val body: Statements) : AST
```

I'll leave out the parser code, most of that is trivial if you've read part 1. If you're interested anyway, check the `src` folder.


### The interpreter

Now that we're working with functions and variables, we need to worry about scopes. So let's make an interface for variables, and a class to hold scopes.
Scopes should inherit their parent scopes, but not modify them.

```kotlin
// Custom class to show errors from interpreter runtime
class InterpreterException(message: String) : Exception(message)

sealed interface InterpreterValue {
    // We'll fill this in later
}

class InterpreterScope(parent: InterpreterScope?) {  // allow for a null parent for the root scope
    private val values = mutableMapOf<String, InterpreterValue>()

    init {
        parent?.values?.let {
            values.putAll(it)
        }
    }

    operator fun get(name: String): InterpreterValue {
        return values[name] ?: throw InterpreterException("Variable $name not found")
    }

    operator fun set(name: String, value: InterpreterValue) {
        values[name] = value
    }
}
```

Now that we're working with variables, we should make it so all behavior "exists" on all types. We've got calling 
functions and basic math, so let's add them to InterpreterValue.

```kotlin
sealed interface InterpreterValue {
    operator fun plus(other: InterpreterValue): InterpreterValue
    operator fun minus(other: InterpreterValue): InterpreterValue
    operator fun times(other: InterpreterValue): InterpreterValue
    operator fun div(other: InterpreterValue): InterpreterValue
    operator fun invoke(interpreter: VariablesInterpreter, args: List<InterpreterValue>): InterpreterValue
}
```

Now let's also define a null type:

```kotlin
object InterpreterNull : InterpreterValue {
    override fun plus(other: InterpreterValue): InterpreterValue {
        throw InterpreterException("null")
    }
    override fun minus(other: InterpreterValue): InterpreterValue {
        throw InterpreterException("null")
    }
    override fun times(other: InterpreterValue): InterpreterValue {
        throw InterpreterException("null")
    }
    override fun div(other: InterpreterValue): InterpreterValue {
        throw InterpreterException("null")
    }
    override fun invoke(interpreter: VariablesInterpreter, args: List<InterpreterValue>): InterpreterValue {
        throw InterpreterException("null")
    }
}
```

Now we can implement behavior for int and float types, as well as functions. I'll show InterpreterFloat, the others can be found under `src`

```kotlin
// Small utility interface to make math easier
sealed interface InterpreterNumber : InterpreterValue {
    val value: Number
}

data class InterpreterFloat(override val value: Float) : InterpreterNumber {
    override fun plus(other: InterpreterValue): InterpreterValue {
        if (other !is InterpreterNumber) {
            throw InterpreterException("Cannot add non-number to float!")
        }
        return InterpreterFloat(value + other.value.toFloat())
    }
    override fun minus(other: InterpreterValue): InterpreterValue {
        if (other !is InterpreterNumber) {
            throw InterpreterException("Cannot subtract non-number from float!")
        }
        return InterpreterFloat(value - other.value.toFloat())
    }
    override fun times(other: InterpreterValue): InterpreterValue {
        if (other !is InterpreterNumber) {
            throw InterpreterException("Cannot multiply float by non-number!")
        }
        return InterpreterFloat(value * other.value.toFloat())
    }
    override fun div(other: InterpreterValue): InterpreterValue {
        if (other !is InterpreterNumber) {
            throw InterpreterException("Cannot divide float by non-number!")
        }
        return InterpreterFloat(value / other.value.toFloat())
    }
    override fun invoke(interpreter: VariablesInterpreter, args: List<InterpreterValue>): InterpreterValue {
        throw InterpreterException("Cannot call float!")
    }
}
```

While we want to have user-defined functions, we also want builtin functions, for e.g. printing.

```kotlin
class InterpreterBuiltinFunction(private val block: VariablesInterpreter.(List<InterpreterValue>) -> InterpreterValue) : InterpreterFunction() {
    override fun invoke(interpreter: VariablesInterpreter, args: List<InterpreterValue>): InterpreterValue {
        return block(interpreter, args)
    }
}
```

Now we have all the building blocks we need to write the interpreter. We'll use a visitor pattern like last time, and just expand it with the new tokens.

```kotlin
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
```
