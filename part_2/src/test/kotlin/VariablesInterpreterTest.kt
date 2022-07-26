import com.martmists.part2.InterpreterNull
import com.martmists.part2.VariablesInterpreter
import com.martmists.part2.VariablesParser
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.StringWriter
import kotlin.test.Test
import kotlin.test.assertEquals

class VariablesInterpreterTest {
    private fun withOutput(block: () -> Unit): String {
        val oldOut = System.out
        val out = ByteArrayOutputStream()
        System.setOut(PrintStream(out))
        try {
            block()
        } finally {
            System.setOut(oldOut)
        }
        return out.toString(Charsets.UTF_8)
    }

    @Test
    fun `test simple variables`() {
        val input = """
            |a = 1;
            |b = 2;
            |c = a + b;
        """.trimMargin()
        val interpreter = VariablesInterpreter()

        val ast = VariablesParser(input).tryParse()
        val result = interpreter.visit(ast)

        assertEquals(InterpreterNull, result)
    }

    @Test
    fun `test builtin function call`() {
        val input = """
            |a = 1;
            |b = 2;
            |c = a + b;
            |println(c);
        """.trimMargin()
        val interpreter = VariablesInterpreter()

        val ast = VariablesParser(input).tryParse()
        val output = withOutput {
            interpreter.visit(ast)
        }

        assertEquals("3\n", output)
    }

    @Test
    fun `test user function`() {
        val input = """
            |a = 1;
            |b = 2;
            |function addB(a) {
            |    c = a + b;
            |    return (c + c);
            |};
            |println(addB(a));
        """.trimMargin()
        val interpreter = VariablesInterpreter()

        val ast = VariablesParser(input).tryParse()
        val output = withOutput {
            interpreter.visit(ast)
        }

        assertEquals("6\n", output)
    }

    @Test
    fun `test nested functions`() {
        val input = """
            |a = 1;
            |b = 2;
            |function addB(a) {
            |    c = a + b;
            |    function addC(a) {
            |        return (a + c);
            |    };
            |    return addC(c);
            |};
            |println(addB(a + 1));
        """.trimMargin()
        val interpreter = VariablesInterpreter()

        val ast = VariablesParser(input).tryParse()
        val output = withOutput {
            interpreter.visit(ast)
        }

        assertEquals("8\n", output)
    }
}
