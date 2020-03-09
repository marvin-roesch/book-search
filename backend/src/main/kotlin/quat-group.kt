import Complex.Companion.I
import Complex.Companion.ONE
import Complex.Companion.ZERO

data class Complex(val real: Int, val imaginary: Int) {
    operator fun plus(c: Complex) = Complex(real + c.real, imaginary + c.imaginary)

    operator fun times(c: Complex) = Complex(real * c.real - imaginary * c.imaginary, imaginary * c.real + real * c.imaginary)

    operator fun unaryMinus() = Complex(-real, -imaginary)

    companion object {
        val ZERO = Complex(0, 0)
        val ONE = Complex(1, 0)
        val I = Complex(0, 1)
    }
}

data class Matrix(val a11: Complex, val a12: Complex, val a21: Complex, val a22: Complex) {
    operator fun plus(m: Matrix) = Matrix(a11 + m.a11, a12 + m.a12, a21 + m.a21, a22 + m.a22)

    operator fun times(m: Matrix) = Matrix(
        a11 * m.a11 + a12 * m.a21,
        a11 * m.a12 + a12 * m.a22,
        a21 * m.a11 + a22 * m.a21,
        a21 * m.a12 + a22 * m.a22
    )

    operator fun unaryMinus() = Matrix(-a11, -a12, -a21, -a22)
}

fun main() {
    val e = Matrix(
        ONE, ZERO,
        ZERO, ONE
    )
    val i = Matrix(
        I, ZERO,
        ZERO, -I
    )
    val j = Matrix(
        ZERO, ONE,
        -ONE, ZERO
    )
    val k = Matrix(
        ZERO, I,
        I, ZERO
    )

    val matrices = listOf(e, -e, i, -i, j, -j, k, -k)
    val names = mapOf(e to "E", i to "I", j to "J", k to "K")

    fun matrixName(m: Matrix): String? {
        return names[m] ?: names[-m]?.let { "-$it" }
    }

    val table = matrices.joinToString("\\\\\n\\hline\n") { a ->
        val aName = matrixName(a)
        val results = matrices.joinToString(" & ") { "\\(${matrixName(a * it)}\\)" }

        "\\($aName\\) & $results"
    }

    println(table)
}
