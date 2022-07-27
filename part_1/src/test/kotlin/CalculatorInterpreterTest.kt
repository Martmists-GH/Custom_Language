import com.martmists.part1.CalculatorInterpreter
import com.martmists.part1.CalculatorParser
import kotlin.test.Test
import kotlin.test.assertEquals

class CalculatorInterpreterTest {
    @Test
    fun `simple addition and subtraction`() {
        val input = "1 + 2 - 3 + 4"
        val expected = 4f

        val ast = CalculatorParser(input).tryParse()
        val result = CalculatorInterpreter().visit(ast)

        assertEquals(expected, result)
    }

    @Test
    fun `simple multiplication and division`() {
        val input = "1 * 2 / 3 * 4"

        val expected = 2.6666667f

        val ast = CalculatorParser(input).tryParse()
        val result = CalculatorInterpreter().visit(ast)

        assertEquals(expected, result)
    }

    @Test
    fun `modified order with parentheses`() {
        val input = "1 * (2 + 3) / 4"
        val expected = 1.25f

        val ast = CalculatorParser(input).tryParse()
        val result = CalculatorInterpreter().visit(ast)

        assertEquals(expected, result)
    }

    @Test
    fun `nested parentheses`() {
        val input = "5 * (((1 + 3) * -2) + -1)"
        val expected = -45f

        val ast = CalculatorParser(input).tryParse()
        val result = CalculatorInterpreter().visit(ast)

        assertEquals(expected, result)
    }
}
