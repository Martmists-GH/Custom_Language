package com.martmists.part2

import com.martmists.kotpack.GrammarParser

class VariablesParser(input: String) : GrammarParser<AST>(input) {
    private val integer by regex("\\d+") {
        IntLiteral(it.toInt())
    }.memo()
    private val float by regex("\\d+\\.\\d+") {
        FloatLiteral(it.toFloat())
    }.memo()
    private val variable by regex("[a-zA-Z_][a-zA-Z\\d_]*") {
        Variable(it)
    }.memo()
    private val number by first(
        ::float,
        ::integer,
    ).memo()
    private val atom by first(
        firstBlock {
            char('(')
            whitespace(optional = true)
            val x = expression()
            whitespace(optional = true)
            char(')')
            x
        },
        ::variable,
        ::number,
    ).memo()
    private val primary: () -> AST by first(
        firstBlock {
            val root = primary()
            char('(')
            whitespace(optional = true)
            val args = optional {
                arguments()
            }
            whitespace(optional = true)
            char(')')
            Call(root, args ?: emptyList())
        },
        ::atom
    ).memoLeft()
    private val factor: () -> AST by first(
        firstBlock {
            char('-')
            space(optional = true)
            when (val x = primary()) {
                is IntLiteral -> IntLiteral(-x.value)
                is FloatLiteral -> FloatLiteral(-x.value)
                else -> BinOp(IntLiteral(0), BinOp.Operator.MINUS, x)
            }
        },
        ::primary,
    ).memo()
    private val term: () -> AST by first(
        firstBlock {
            val left = term()
            whitespace(optional = true)
            val op = when(val x = regex("[*/]").first()) {
                '*' -> BinOp.Operator.TIMES
                '/' -> BinOp.Operator.DIVIDE
                else -> throw IllegalStateException("Unexpected operator $x")
            }
            whitespace(optional = true)
            val right = factor()
            BinOp(left, op, right)
        },
        ::factor,
    ).memoLeft()
    private val expression: () -> AST by first(
        firstBlock {
            val left = expression()
            whitespace(optional = true)
            val op = when(val x = regex("[+-]").first()) {
                '+' -> BinOp.Operator.PLUS
                '-' -> BinOp.Operator.MINUS
                else -> throw IllegalStateException("Unexpected operator $x")
            }
            whitespace(optional = true)
            val right = term()
            BinOp(left, op, right)
        },
        ::term,
    ).memoLeft()
    private val arguments by sequence {
        val items = mutableListOf(expression())
        do {
            val extra = optional {
                whitespace(optional = true)
                char(',')
                whitespace(optional = true)
                expression().also(items::add)
            }
        } while (extra != null)
        items
    }.memo()
    private val parameters by sequence {
        val items = mutableListOf(variable())
        do {
            val extra = optional {
                whitespace(optional = true)
                char(',')
                whitespace(optional = true)
                variable().also(items::add)
            }
        } while (extra != null)
        items
    }.memo()
    private val assignment by sequence {
        val left = variable()
        space(optional = true)
        char('=')
        space(optional = true)
        val right = expression()
        Assign(left, right)
    }.memo()
    private val functionDef by sequence {
        string("function")
        space(optional = false)
        val name = variable()
        space(optional = true)
        char('(')
        whitespace(optional = true)
        val args = optional {
            parameters()
        } ?: emptyList()
        whitespace(optional = true)
        char(')')
        whitespace(optional = true)
        char('{')
        whitespace(optional = true)
        val body = functionStatements()
        whitespace(optional = true)
        char('}')
        Function(name.value, args.map(Variable::value), body)
    }.memo()
    private val statement: () -> AST by first(
        ::assignment,
        ::functionDef,
        ::expression,
    ).memo()
    private val functionStatement: () -> AST by first(
        firstBlock {
            string("return")
            whitespace()
            val x = expression()
            Return(x)
        },
        ::statement,
    ).memo()
    private val statements by sequence {
        val items = zeroOrMore {
            whitespace(optional = true)
            val x = statement()
            whitespace(optional = true)
            char(';')
            whitespace(optional = true)
            x
        }
        Statements(items)
    }.memo()
    private val functionStatements by sequence {
        val items = zeroOrMore {
            whitespace(optional = true)
            val x = functionStatement()
            whitespace(optional = true)
            char(';')
            whitespace(optional = true)
            x
        }
        Statements(items)
    }.memo()

    override val root by sequence {
        val stmts = statements()
        eoi()
        stmts
    }
}
