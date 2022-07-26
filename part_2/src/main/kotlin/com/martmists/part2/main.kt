package com.martmists.part2

import com.martmists.kotpack.NoMatchException

fun main() {
    val interpreter = VariablesInterpreter()
    println("press ^D or an empty line to quit:")

    var buffer = ""
    outer@while (true) {
        inner@while (true) {
            if (buffer.isEmpty()) {
                print(">>> ")
            } else {
                print("... ")
            }
            val input = readLine() ?: break@outer
            if (input.isBlank()) break@inner
            buffer += input + "\n"
        }

        val parser = VariablesParser(buffer)
        buffer = ""
        val ast = try {
            parser.tryParse()
        } catch (e: NoMatchException) {
            println("Invalid input: $e")
            continue
        }

        val result = interpreter.visit(ast)
        if (result != InterpreterNull) {
            println(result)
        }
    }
}
