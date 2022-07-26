package com.martmists.part2

class InterpreterException(message: String) : Exception(message)

sealed interface InterpreterValue {
    operator fun plus(other: InterpreterValue): InterpreterValue
    operator fun minus(other: InterpreterValue): InterpreterValue
    operator fun times(other: InterpreterValue): InterpreterValue
    operator fun div(other: InterpreterValue): InterpreterValue
    operator fun invoke(interpreter: VariablesInterpreter, args: List<InterpreterValue>): InterpreterValue
}

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

    override fun toString(): String {
        return "null"
    }
}

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

    override fun toString(): String {
        return value.toString()
    }
}

data class InterpreterInt(override val value: Int) : InterpreterNumber {
    override fun plus(other: InterpreterValue): InterpreterValue {
        return when (other) {
            !is InterpreterNumber -> {
                throw InterpreterException("Cannot add non-number to int!")
            }
            is InterpreterFloat -> {
                InterpreterFloat(value.toFloat() + other.value)
            }
            else -> {
                InterpreterInt(value + other.value.toInt())
            }
        }
    }

    override fun minus(other: InterpreterValue): InterpreterValue {
        return when (other) {
            !is InterpreterNumber -> {
                throw InterpreterException("Cannot subtract non-number from int!")
            }
            is InterpreterFloat -> {
                InterpreterFloat(value.toFloat() - other.value)
            }
            else -> {
                InterpreterInt(value - other.value.toInt())
            }
        }
    }

    override fun times(other: InterpreterValue): InterpreterValue {
        return when (other) {
            !is InterpreterNumber -> {
                throw InterpreterException("Cannot multiply int by non-number!")
            }
            is InterpreterFloat -> {
                InterpreterFloat(value.toFloat() * other.value)
            }
            else -> {
                InterpreterInt(value * other.value.toInt())
            }
        }
    }

    override fun div(other: InterpreterValue): InterpreterValue {
        return when (other) {
            !is InterpreterNumber -> {
                throw InterpreterException("Cannot divide int by non-number!")
            }
            is InterpreterFloat -> {
                InterpreterFloat(value.toFloat() / other.value)
            }
            else -> {
                InterpreterInt(value / other.value.toInt())
            }
        }
    }

    override fun invoke(interpreter: VariablesInterpreter, args: List<InterpreterValue>): InterpreterValue {
        throw InterpreterException("Cannot call int!")
    }

    override fun toString(): String {
        return value.toString()
    }
}

abstract class InterpreterFunction : InterpreterValue {
    override fun plus(other: InterpreterValue): InterpreterValue {
        throw InterpreterException("Cannot add function!")
    }

    override fun minus(other: InterpreterValue): InterpreterValue {
        throw InterpreterException("Cannot subtract function!")
    }

    override fun times(other: InterpreterValue): InterpreterValue {
        throw InterpreterException("Cannot multiply function!")
    }

    override fun div(other: InterpreterValue): InterpreterValue {
        throw InterpreterException("Cannot divide function!")
    }
}

class InterpreterUserFunction(private val parameters: List<String>, private val statements: Statements) : InterpreterFunction() {
    override fun invoke(interpreter: VariablesInterpreter, args: List<InterpreterValue>): InterpreterValue {
        if (args.size != parameters.size) {
            throw InterpreterException("Wrong number of arguments! Expected ${parameters.size}, got ${args.size}")
        }

        var res: InterpreterValue = InterpreterNull

        interpreter.scoped {
            for ((param, arg) in parameters.zip(args)) {
                interpreter.currentScope[param] = arg
            }

            res = interpreter.visit(statements)
        }

        return res
    }
}

class InterpreterBuiltinFunction(private val block: VariablesInterpreter.(List<InterpreterValue>) -> InterpreterValue) : InterpreterFunction() {
    override fun invoke(interpreter: VariablesInterpreter, args: List<InterpreterValue>): InterpreterValue {
        return block(interpreter, args)
    }
}

class InterpreterScope(parent: InterpreterScope?) {
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
