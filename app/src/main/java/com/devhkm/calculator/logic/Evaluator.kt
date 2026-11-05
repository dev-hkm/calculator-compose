package com.devhkm.calculator.logic

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Bộ phân tích & tính biểu thức số học thuần Kotlin (không phụ thuộc lib ngoài).
 * Hỗ trợ: + - × ÷ % ^ , ngoặc đơn, số âm, hằng số π/e, hàm sin/cos/tan/log/ln/√.
 * Dùng shunting-yard -> RPN rồi evaluate với BigDecimal.
 */
object Evaluator {

    private val MC = MathContext(16, RoundingMode.HALF_UP)

    sealed interface Result {
        data class Ok(val value: String) : Result
        data class Err(val message: String) : Result
    }

    // ---------------- public API ----------------

    fun evaluate(raw: String): Result {
        val src = normalize(raw)
        if (src.isBlank()) return Result.Ok("0")
        return try {
            val tokens = tokenize(src)
            val rpn = toRpn(tokens)
            val v = evalRpn(rpn)
            Result.Ok(format(v))
        } catch (e: EvalException) {
            Result.Err(e.message ?: "Lỗi")
        } catch (e: Exception) {
            Result.Err("Biểu thức không hợp lệ")
        }
    }

    /** Định dạng số cho vùng VN: dấu '.' ngăn cách nghìn, ',' thập phân. */
    fun format(v: BigDecimal): String {
        if (v.abs() > BigDecimal("1E15")) {
            return v.round(MathContext(10, RoundingMode.HALF_UP)).toString()
        }
        var b = v.stripTrailingZeros()
        if (b.scale() < 0) b = b.setScale(0)
        val plain = b.toPlainString()
        val neg = plain.startsWith("-")
        val abs = plain.removePrefix("-")
        val parts = abs.split(".")
        val intPart = parts[0]
        val decPart = if (parts.size > 1) parts[1] else ""

        val sb = StringBuilder()
        intPart.reversed().forEachIndexed { i, c ->
            if (i > 0 && i % 3 == 0) sb.append('.')
            sb.append(c)
        }
        var out = sb.reverse().toString()
        if (decPart.isNotEmpty()) out += "," + decPart
        return if (neg) "-$out" else out
    }

    // ---------------- nội bộ ----------------

    private class EvalException(msg: String) : Exception(msg)

    private const val PI = Math.PI
    private const val E = Math.E

    private val CONSTANTS = mapOf("π" to PI, "e" to E)

    private sealed interface Tok {
        data class Num(val v: BigDecimal) : Tok
        data class Op(val s: String) : Tok
        data class Fn(val s: String) : Tok
        data object LParen : Tok
        data object RParen : Tok
    }

    /** Chuẩn hoá ký hiệu hiển thị -> ký hiệu tính toán. */
    private fun normalize(s: String): String {
        var t = s
            .replace("×", "*").replace("÷", "/").replace("−", "-")
            .replace("√", "sqrt")
            .replace("π", "π")
            .replace(Regex("\\s+"), "")
        // % đứng sau số hoặc ')' => /100
        t = Regex("([0-9.()]+)%").replace(t) { m ->
            val g = m.groupValues[1]
            "($g/100)"
        }
        return t
    }

    private fun tokenize(s: String): List<Tok> {
        val out = mutableListOf<Tok>()
        var i = 0
        var prev: Tok? = null
        while (i < s.length) {
            val c = s[i]
            when {
                c.isDigit() || c == '.' -> {
                    val start = i
                    var dots = 0
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) {
                        if (s[i] == '.') dots++
                        i++
                    }
                    if (dots > 1) throw EvalException("Số không hợp lệ")
                    val txt = s.substring(start, i)
                    out.add(Tok.Num(txt.toBigDecimalOrNull() ?: throw EvalException("Số không hợp lệ")))
                }
                else -> {
                    // đọc tên hàm / hằng
                    if (c.isLetter() || c == 'π') {
                        val start = i
                        while (i < s.length && (s[i].isLetter() || s[i] == 'π')) i++
                        var name = s.substring(start, i)
                        when (name) {
                            "π" -> out.add(Tok.Num(CONSTANTS["π"]!!.toBigDecimal(MC)))
                            "e" -> out.add(Tok.Num(CONSTANTS["e"]!!.toBigDecimal(MC)))
                            "sqrt", "sin", "cos", "tan", "log", "ln", "exp" -> out.add(Tok.Fn(name))
                            else -> throw EvalException("Không hiểu: $name")
                        }
                    } else {
                        when (c) {
                            '(' -> { out.add(Tok.LParen); i++ }
                            ')' -> { out.add(Tok.RParen); i++ }
                            '+', '-', '*', '/', '^' -> {
                                // dấu âm đơn hạng
                                val unary = (c == '-' || c == '+') && needUnary(prev, out)
                                if (unary) {
                                    if (c == '-') out.add(Tok.Op("u-")) 
                                } else {
                                    out.add(Tok.Op(c.toString()))
                                }
                                i++
                            }
                            else -> throw EvalException("Ký tự lạ: $c")
                        }
                    }
                    prev = out.lastOrNull()
                    continue
                }
            }
            prev = out.lastOrNull()
        }
        return out
    }

    private fun needUnary(prev: Tok?, out: List<Tok>): Boolean = when (prev) {
        null -> true
        is Tok.Op -> true
        Tok.LParen -> true
        else -> false
    }

    private fun prec(op: String): Int = when (op) {
        "u-" -> 5
        "^" -> 4
        "*", "/" -> 3
        "+", "-" -> 2
        else -> 0
    }

    private fun rightAssoc(op: String) = op == "^" || op == "u-"

    private fun toRpn(tokens: List<Tok>): List<Tok> {
        val out = mutableListOf<Tok>()
        val stack = ArrayDeque<Tok>()
        for (t in tokens) {
            when (t) {
                is Tok.Num -> out.add(t)
                is Tok.Fn -> stack.addLast(t)
                Tok.LParen -> stack.addLast(t)
                Tok.RParen -> {
                    var found = false
                    while (stack.isNotEmpty()) {
                        val top = stack.removeLast()
                        if (top == Tok.LParen) { found = true; break }
                        out.add(top)
                    }
                    if (!found) throw EvalException("Thiếu dấu ngoặc")
                    if (stack.isNotEmpty() && stack.last() is Tok.Fn) out.add(stack.removeLast())
                }
                is Tok.Op -> {
                    while (stack.isNotEmpty()) {
                        val top = stack.last()
                        val ok = when {
                            top is Tok.Fn -> true
                            top is Tok.Op -> {
                                val p1 = prec(t.s); val p2 = prec(top.s)
                                if (rightAssoc(t.s)) p2 > p1 else p2 >= p1
                            }
                            else -> false
                        }
                        if (ok) out.add(stack.removeLast()) else break
                    }
                    stack.addLast(t)
                }
            }
        }
        while (stack.isNotEmpty()) {
            val top = stack.removeLast()
            if (top == Tok.LParen || top == Tok.RParen) throw EvalException("Thiếu dấu ngoặc")
            out.add(top)
        }
        return out
    }

    private fun evalRpn(rpn: List<Tok>): BigDecimal {
        val st = ArrayDeque<BigDecimal>()
        for (t in rpn) {
            when (t) {
                is Tok.Num -> st.addLast(t.v)
                is Tok.Op -> {
                    if (t.s == "u-") {
                        val a = st.removeLastOrNull() ?: throw EvalException("Thiếu toán hạng")
                        st.addLast(a.negate())
                    } else {
                        val b = st.removeLastOrNull() ?: throw EvalException("Thiếu toán hạng")
                        val a = st.removeLastOrNull() ?: throw EvalException("Thiếu toán hạng")
                        st.addLast(
                            when (t.s) {
                                "+" -> a.add(b, MC)
                                "-" -> a.subtract(b, MC)
                                "*" -> a.multiply(b, MC)
                                "/" -> {
                                    if (b.compareTo(BigDecimal.ZERO) == 0) throw EvalException("Chia cho 0")
                                    a.divide(b, MC)
                                }
                                "^" -> {
                                    val da = a.toDouble(); val db = b.toDouble()
                                    val r = Math.pow(da, db)
                                    if (!r.isFinite()) throw EvalException("Kết quả không hợp lệ")
                                    BigDecimal(r.toString()).round(MC)
                                }
                                else -> throw EvalException("Toán tử lạ")
                            }
                        )
                    }
                }
                is Tok.Fn -> {
                    val a = st.removeLastOrNull() ?: throw EvalException("Thiếu toán hạng")
                    val d = a.toDouble()
                    val r = when (t.s) {
                        "sqrt" -> { if (d < 0) throw EvalException("Căn bậc 2 của số âm"); Math.sqrt(d) }
                        "sin" -> Math.sin(Math.toRadians(d))
                        "cos" -> Math.cos(Math.toRadians(d))
                        "tan" -> Math.tan(Math.toRadians(d))
                        "log" -> { if (d <= 0) throw EvalException("log của số ≤ 0"); Math.log10(d) }
                        "ln" -> { if (d <= 0) throw EvalException("ln của số ≤ 0"); Math.log(d) }
                        "exp" -> Math.exp(d)
                        else -> throw EvalException("Hàm lạ")
                    }
                    if (!r.isFinite()) throw EvalException("Kết quả không hợp lệ")
                    st.addLast(BigDecimal(r.toString()).round(MC))
                }
                else -> throw EvalException("Biểu thức không hợp lệ")
            }
        }
        return st.removeLastOrNull() ?: throw EvalException("Biểu thức rỗng")
    }
}
