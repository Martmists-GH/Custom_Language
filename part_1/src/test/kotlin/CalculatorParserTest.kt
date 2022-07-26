import com.martmists.part1.BinOp
import com.martmists.part1.CalculatorParser
import com.martmists.part1.IntLiteral
import kotlin.test.Test
import kotlin.test.assertEquals

class CalculatorParserTest {
    @Test
    fun `simple addition and subtraction`() {
        val input = "1 + 2 - 3 + 4"
        val expected = BinOp(
            BinOp(
                BinOp(
                    IntLiteral(1),
                    BinOp.Operator.PLUS,
                    IntLiteral(2)
                ),
                BinOp.Operator.MINUS,
                IntLiteral(3)
            ),
            BinOp.Operator.PLUS,
            IntLiteral(4)
        )

        val result = CalculatorParser(input).tryParse()

        assertEquals(expected, result)
    }

    @Test
    fun `simple multiplication and division`() {
        val input = "1 * 2 / 3 * 4"
        val expected = BinOp(
            BinOp(
                BinOp(
                    IntLiteral(1),
                    BinOp.Operator.TIMES,
                    IntLiteral(2)
                ),
                BinOp.Operator.DIVIDE,
                IntLiteral(3)
            ),
            BinOp.Operator.TIMES,
            IntLiteral(4)
        )

        val result = CalculatorParser(input).tryParse()

        assertEquals(expected, result)
    }

    @Test
    fun `modifed order with parentheses`() {
        val input = "1 * (2 + 3) / 4"
        val expected = BinOp(
            BinOp(
                IntLiteral(1),
                BinOp.Operator.TIMES,
                BinOp(
                    IntLiteral(2),
                    BinOp.Operator.PLUS,
                    IntLiteral(3)
                )
            ),
            BinOp.Operator.DIVIDE,
            IntLiteral(4)
        )

        val result = CalculatorParser(input).tryParse()

        assertEquals(expected, result)
    }

    @Test
    fun `nested parentheses`() {
        val input = "5 * (((1 + 3) * -2) + -1)"
        val expected = BinOp(
            IntLiteral(5),
            BinOp.Operator.TIMES,
            BinOp(
                BinOp(
                    BinOp(
                        IntLiteral(1),
                        BinOp.Operator.PLUS,
                        IntLiteral(3)
                    ),
                    BinOp.Operator.TIMES,
                    IntLiteral(-2)
                ),
                BinOp.Operator.PLUS,
                IntLiteral(-1)
            )
        )

        val result = CalculatorParser(input).tryParse()

        assertEquals(expected, result)
    }
}
