package com.martmists.part1

import com.martmists.kotpack.NoMatchException

fun main() {
    val interpreter = CalculatorInterpreter()
    println("Enter a mathematical expression or press Enter to quit:")
    while (true) {
        print(">>> ")
        val input = readLine() ?: break
        if (input.isBlank()) break

        val parser = CalculatorParser(input)
        val ast = try {
            parser.tryParse()
        } catch (e: NoMatchException) {
            println("Invalid input: $e")
            continue
        }
        val result = interpreter.visit(ast)
        println("$input = $result")
    }
}
