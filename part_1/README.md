# Part 1: A simple calculator

The first step is to identify all parts of your grammar. In part 1 I'll go over simple math with the four basic operators, as well as parentheses.
By using a grammar parser library with left-recursion, we can use this to easily parse expressions left-to-right.

The `src` folder contains the same code as shown below, in case you want to run it without having to copy everything.

### Writing the grammar

The first things we should write down are the terminals, which are basically the smallest pieces of our language. For a calculator, this means numbers.

```
integer := r"\d+"
float   := r"\d+\.\d+"
number  := float 
         | integer
```

We now have a grammar for both integers and decimal.
Now, we simply add the other parts of our calculator, in order of how we want them to be handled.
This means we add parentheses first, then multiplication and division, then addition and subtraction.

```
atom        := '(' expression ')'  # expression is not yet defined; we'll get there later
             | number
factor      := '-' atom
             | atom
term        := term '*' factor     # In the code for this a regex will be used; for the grammar this provides more clarity
             | term '/' factor
             | factor
expression  := expression '+' term
             | expression '-' term
             | term
```

When given an expression like `1+2*3.1`, the parser will parse it like this:

```
expression (
    expression (
        term(
            factor (
                atom (
                    number (
                        integer (
                            "1"
                        )
                    )
                )
            )
        )
    )
    '+'
    term(
        term(
            factor (
                atom (
                    number (
                        integer (
                            "2"
                        )
                    )
                )
            )
        )
        '*'
        factor (
            atom (
                number (
                    float (
                        "3.1"
                    )
                )
            )
        )
    )
)
```
Or, when trimmed (which is what we want to end up with in our parser implementation):
```
expression(integer(1), '+', term(integer(2), '*', float(3.1)))
(1) + ((2) * (3.1))
```
which matches the priorities of the operators as we specified.

### Writing the AST nodes

AST is basically a tree representation of a parse result, taking the previous example:

```
    expression
    /    |   \
integer  +   term
            /  |  \
      integer  *  float
```
Let's convert this to some classes:
```kotlin
sealed interface AST

sealed interface NumberLiteral: AST
data class IntLiteral(val value: Int) : NumberLiteral
data class FloatLiteral(val value: Float) : NumberLiteral

data class BinOp(val left: AST, val op: Operator, val right: AST) : AST {
    enum class Operator {
        PLUS, MINUS, TIMES, DIVIDE
    }
}
```
With these classes, we can create any expression we want, for example `(1 + 2) * 3`:
```kotlin
BinOp(
    BinOp(
        IntLiteral(1),
        BinOp.Operator.PLUS,
        IntLiteral(2)
    ),
    BinOp.Operator.TIMES,
    IntLiteral(3)
)
```

### Writing the parser

Luckily, once we have a grammar and AST nodes, this part becomes fairly trivial.

First, we write the small parts again:
```kotlin
val integer by regex("\\d+") {
    // In Kotpack, this block is a transformation with the result,
    // so we can use it to create a new AST node very easily.
    IntLiteral(it.toInt())  
}.memo()
// .memo() calls are just for optimization; they use slightly more memory but are faster because they cache the result.

val float by regex("\\d+\\.\\d+") {
    FloatLiteral(it.toFloat())
}.memo()

val number by first(
    // Order is important! integer matches the start of float, so we need to put float first.
    // This is also critical in recursive rules.
    ::float,
    ::integer,
).memo()
```
Then we can add the rest of the grammar:
```kotlin
val atom by first(
    firstBlock {
        char('(')
        val x = expression()
        char(')')
        x
    },
    ::number,
).memo()

private val factor: () -> AST by first(
    firstBlock {
        char('-')
        when (val x = atom()) {
            is IntLiteral -> IntLiteral(-x.value)
            is FloatLiteral -> FloatLiteral(-x.value)
            else -> BinOp(IntLiteral(0), BinOp.Operator.MINUS, x)
        }
    },
    ::atom,
).memo()

val term: () -> AST by first(
    firstBlock {
        val left = term()  // Uh oh! we have left recursion here!
        val op = when(val x = regex("[*/]").first()) {
            '*' -> BinOp.Operator.TIMES
            '/' -> BinOp.Operator.DIVIDE
            else -> throw IllegalStateException("Unexpected operator $x")
        }
        val right = factor()
        BinOp(left, op, right)
    },
    ::factor,
).memoLeft()  // to deal with left-recursion, a memoLeft call is REQUIRED!
// More information here: https://medium.com/@gvanrossum_83706/left-recursive-peg-grammars-65dab3c580e1

val expression: () -> AST by first(
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
```
And finally, kotpack requires us to specify the root:
```kotlin
override val root by sequence {
    val exp = expression()
    eoi()
    exp
}
```

One downside of our grammar is that whitespace breaks everything, but our language doesn't need them anywhere, as there's no type for strings.
This means we can solve the problem in the constructor:
```kotlin
class CalculatorParser(input: String) : 
    GrammarParser<AST>(input.replace(Regex("\\s+"), "")) {
    // ...
}
```

### Interpreting the AST

Now we're on to the easy part. Depending on your language, implementations might become very complex,
but let's keep it simple for now. We'll also be lazy and do all math as floats.

```kotlin
class CalculatorInterpreter {
    fun visit(ast: AST): Float {  // This uses a recursive visitor pattern, which I won't cover here.
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
```

### Combining it all

We can now write a simple CLI for our calculator:

```kotlin
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
```

Congrats, you've now made a functional calculator!
