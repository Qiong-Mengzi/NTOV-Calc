package nano.yumeko.ntovCalc

import java.util.Stack
import kotlin.math.*

class Calculator(maxToken: Int = 256) {
    @JvmField
    val scanSignList = "()+-*/%^"

    @JvmField
    val supportFunc = setOf(
        "sqrt",
        "ln",
        "sin",
        "cos",
        "tan",
        "asin",
        "acos",
        "atan"
    )

    private var maxTokenLimit = maxToken

    enum class TokenType {
        UNK,
        NUM,
        OP,
        FUNC,
    }

    enum class ScanStatus {
        START,
        NEG_INT,
        POS_INT,
        NEG_POINT,
        POS_POINT,
        SIGN,
        ID,
        NEXT,
        ERROR
    }

    class Token {
        var num = 0f
        var op = ""
        var type = TokenType.UNK

        override fun toString(): String {
            val sb = StringBuilder()
            when (type) {
                TokenType.OP -> sb.append("{ ").append(op).append(" ( ").append(TokenType.OP.toString()).append(" ) }")
                TokenType.UNK -> sb.append("{ '").append(op).append("' ( ").append(TokenType.UNK.toString())
                    .append(" ) }")

                TokenType.NUM -> sb.append("{ ").append(num).append(" ( ").append(TokenType.NUM.toString())
                    .append(" ) }")

                TokenType.FUNC -> sb.append("{ ").append(op).append(" ( ").append(TokenType.FUNC.toString())
                    .append(" ) }")
            }
            return sb.toString()
        }
    }

    class ScanResult(result: Array<Token>, sei: Int, sec: Char) {
        private val r = result
        private val errorIndex = sei
        private val errorChar = sec

        override fun toString(): String {
            if (errorIndex < 0) {
                val sb = StringBuilder()
                for (t in r) {
                    sb.append(t.toString()).append('\n')
                }
                return sb.toString()
            } else {
                return StringBuilder().append("ScanError: ").append(errorChar).append(" on index ").append(errorIndex)
                    .toString()
            }
        }

        fun getTokens(): Array<Token> {
            return r
        }

        fun getError(): Int {
            return errorIndex
        }
    }

    enum class AnalyzeFlag {
        SUCCEED,
        UNK_FUNC,
        UNMATCHED_PARENTHESES,
        EMPTY_SUB_EXPR,

    }

    class AnalyzeResult(analyzeResult: Array<Token>, reason: Token, analyzeFlag: AnalyzeFlag = AnalyzeFlag.SUCCEED) {
        val result = analyzeResult
        val errorReason = reason
        val flag = analyzeFlag
    }

    /**
     * 将token数组转为字符串 (此功能可以改进)
     *
     * @param at token数组
     * @return 转换后的字符串
     */
    fun arrayTokenToString(at: Array<Token>): String {
        val sb = StringBuilder()
        for (t in at) {
            sb.append(t.toString()).append('\n')
        }
        return sb.toString()
    }

    private fun numToken(n: Float): Token {
        val r = Token()
        r.num = n
        r.type = TokenType.NUM
        return r
    }

    private fun opToken(o: String): Token {
        val r = Token()
        r.op = o
        r.type = TokenType.OP
        return r
    }

    private fun funcToken(f: String): Token {
        val r = Token()
        r.op = f
        r.type = TokenType.FUNC
        return r
    }

    private fun unkToken(s: String): Token {
        val r = Token()
        r.op = s
        r.type = TokenType.UNK
        return r
    }

    private var scanErrorIndex = -1
    private var scanErrorChar = (0).toChar()

    private fun clearScanError() {
        scanErrorIndex = -1
        scanErrorChar = (0).toChar()
    }

    fun getTokenLimit(): Int {
        return maxTokenLimit
    }

    fun setTokenLimit(newTokenLimit: Int = 256) {
        maxTokenLimit = newTokenLimit
    }

    /**
     * 对表达式进行词法分析
     *
     * @param s 进行词法分析的表达式
     * @return 分析结果，如果出现错误 scanErrorIndex 将不为-1
     */
    fun scan(s: String): ScanResult {
        val tokenBuf = ArrayList<Token>()
        var status = ScanStatus.START
        var numBuf = 0f
        var exp = 1f
        val strBuf = StringBuilder()

        clearScanError()

        var i = 0
        for (c in s) {
            if (tokenBuf.size < maxTokenLimit)
                when (status) {
                    ScanStatus.ERROR -> break

                    ScanStatus.START -> {
                        numBuf = 0f
                        strBuf.clear()
                        if (c.isDigit()) {
                            status = ScanStatus.POS_INT
                            numBuf = (c - '0').toFloat()
                        } else if (c.isLetter()) {
                            status = ScanStatus.ID
                            strBuf.append(c)
                        } else if (c == '+') {
                            status = ScanStatus.POS_INT
                        } else if (c == '-') {
                            status = ScanStatus.NEG_INT
                        } else if (c == '(' || c == ')') {
                            status = ScanStatus.SIGN
                            strBuf.append(c)
                        } else if (c == ' ' || c == '\t') {
                            status = ScanStatus.START
                        } else {
                            status = ScanStatus.ERROR
                        }
                    }

                    ScanStatus.NEXT -> {
                        numBuf = 0f
                        if (c.isDigit()) {
                            status = ScanStatus.POS_INT
                            numBuf = (c - '0').toFloat()
                        } else if (c.isLetter()) {
                            status = ScanStatus.ID
                            strBuf.clear()
                            strBuf.append(c)
                        } else if (c in scanSignList) {
                            status = ScanStatus.SIGN
                            strBuf.append(c)
                        } else if (c == ' ' || c == '\t') {
                            status = ScanStatus.START
                        } else {
                            status = ScanStatus.ERROR
                        }
                    }

                    ScanStatus.SIGN -> {
                        tokenBuf += opToken(strBuf.toString())
                        strBuf.clear()

                        if (c.isDigit()) {
                            status = ScanStatus.POS_INT
                            numBuf = (c - '0').toFloat()
                        } else if (c.isLetter()) {
                            status = ScanStatus.ID
                            strBuf.append(c)
                        } else if (c in scanSignList) {
                            status = ScanStatus.SIGN
                            strBuf.append(c)
                        } else if (c == ' ' || c == '\t') {
                            status = ScanStatus.NEXT
                        } else {
                            status = ScanStatus.ERROR
                        }
                    }

                    ScanStatus.ID -> {
                        if (c.isDigit()) {
                            status = ScanStatus.POS_INT
                            tokenBuf.add(funcToken(strBuf.toString()))
                            strBuf.clear()
                        } else if (c.isLetter()) {
                            status = ScanStatus.ID
                            strBuf.append(c)
                        } else if (c == ' ' || c == '\t') {
                            status = ScanStatus.NEXT
                            tokenBuf.add(funcToken(strBuf.toString()))
                            strBuf.clear()
                        } else if (c in scanSignList) {
                            status = ScanStatus.SIGN
                            tokenBuf.add(funcToken(strBuf.toString()))
                            strBuf.clear()
                            strBuf.append(c)
                        } else status = ScanStatus.ERROR
                    }

                    ScanStatus.POS_INT -> {
                        if (c.isDigit()) {
                            status = ScanStatus.POS_INT
                            numBuf = numBuf * 10f + (c - '0').toFloat()
                        } else if (c == '.') {
                            status = ScanStatus.POS_POINT
                        } else if (c.isLetter()) {
                            status = ScanStatus.ID
                            tokenBuf.add(numToken(numBuf))
                            strBuf.append(c)
                            numBuf = 0f
                        } else if (c in scanSignList) {
                            status = ScanStatus.SIGN
                            tokenBuf.add(numToken(numBuf))
                            strBuf.append(c)
                            numBuf = 0f
                        } else if (c == ' ' || c == '\t') {
                            status = ScanStatus.NEXT
                            tokenBuf.add(numToken(numBuf))
                            numBuf = 0f
                        } else status = ScanStatus.ERROR
                    }

                    ScanStatus.NEG_INT -> {
                        if (c.isDigit()) {
                            numBuf = numBuf * 10f - (c - '0').toFloat()
                        } else if (c == '.') {
                            status = ScanStatus.NEG_POINT
                        } else if (c.isLetter()) {
                            status = ScanStatus.ID
                            tokenBuf.add(numToken(numBuf))
                            strBuf.append(c)
                            numBuf = 0f
                        } else if (c in scanSignList) {
                            status = ScanStatus.SIGN
                            tokenBuf.add(numToken(numBuf))
                            strBuf.append(c)
                            numBuf = 0f
                        } else if (c == ' ' || c == '\t') {
                            status = ScanStatus.NEXT
                            tokenBuf.add(numToken(numBuf))
                            numBuf = 0f
                        }
                    }

                    ScanStatus.POS_POINT -> {
                        if (c.isDigit()) {
                            status = ScanStatus.POS_POINT
                            exp /= 10f
                            numBuf += (c - '0').toFloat() * exp
                        } else if (c.isLetter()) {
                            status = ScanStatus.ID
                            tokenBuf.add(numToken(numBuf))
                            strBuf.append(c)
                            numBuf = 0f
                            exp = 1f
                        } else if (c in scanSignList) {
                            status = ScanStatus.SIGN
                            tokenBuf.add(numToken(numBuf))
                            strBuf.append(c)
                            numBuf = 0f
                            exp = 1f
                        } else if (c == ' ' || c == '\t') {
                            status = ScanStatus.NEXT
                            tokenBuf.add(numToken(numBuf))
                            numBuf = 0f
                            exp = 1f
                        } else status = ScanStatus.ERROR
                    }

                    ScanStatus.NEG_POINT -> {
                        if (c.isDigit()) {
                            status = ScanStatus.NEG_POINT
                            exp /= 10f
                            numBuf -= (c - '0').toFloat() * exp
                        } else if (c.isLetter()) {
                            status = ScanStatus.ID
                            tokenBuf.add(numToken(numBuf))
                            strBuf.append(c)
                            numBuf = 0f
                            exp = 1f
                        } else if (c in scanSignList) {
                            status = ScanStatus.SIGN
                            tokenBuf.add(numToken(numBuf))
                            strBuf.append(c)
                            numBuf = 0f
                            exp = 1f
                        } else if (c == ' ' || c == '\t') {
                            status = ScanStatus.NEXT
                            tokenBuf.add(numToken(numBuf))
                            numBuf = 0f
                            exp = 1f
                        } else status = ScanStatus.ERROR
                    }
                }
            else
                return ScanResult(arrayOf(), -1145, scanErrorChar)
            if (status == ScanStatus.ERROR) {
                scanErrorChar = c
                scanErrorIndex = i
            }
            i += 1
        }

        return ScanResult(tokenBuf.toTypedArray(), scanErrorIndex, scanErrorChar)
    }

    /**
     * 将词法分析后的表达式进行中缀表达式到后缀表达式的转换
     *
     * @param scanResult 词法分析结果
     * @return 转换后的后缀表达式
     */
    fun analyze(scanResult: ScanResult): AnalyzeResult {

        // 后缀表达式存储
        val rpn = arrayListOf<Token>()
        // 操作符栈
        val rpnStack = Stack<Token>()

        // 中缀表达式转后缀表达式
        for (token in scanResult.getTokens()) {
            if (token.type == TokenType.NUM) {
                rpn.add(token)
            } else if (token.type == TokenType.OP) {
                if (token.op == "*" || token.op == "/" || token.op == "%" || token.op == "^") {
                    // 优先级最高的
                    if (rpnStack.size == 0) {
                        // 操作符栈空
                        rpnStack.push(token)
                    } else {
                        val topOp = rpnStack.peek().op
                        if (topOp == "+" || topOp == "-" || topOp == "(") {// 操作符比栈顶优先级大
                            rpnStack.push(token)
                        } else {
                            while (true) {
                                if (rpnStack.isEmpty()) break
                                val topOp2 = rpnStack.peek().op
                                if (topOp2 == "+" || topOp2 == "-" || topOp2 == "(") break
                                else rpn.add(rpnStack.pop())
                            }
                            rpnStack.push(token)
                        }
                    }
                } else if (token.op == "+" || token.op == "-") {
                    // 优先级低
                    if (rpnStack.size == 0) {
                        // 操作符栈空
                        rpnStack.push(token)
                    } else {
                        while (true) {
                            if (rpnStack.isEmpty()) break
                            if (rpnStack.peek().op == "(") break
                            rpn.add(rpnStack.pop())
                        }
                        rpnStack.push(token)
                    }
                } else if (token.op == "(") {
                    // 左括号，直接入栈
                    rpnStack.push(token)
                } else if (token.op == ")") {
                    // 括号内空的表达式
                    if (rpnStack.isNotEmpty()) if (rpnStack.peek().op == "(") return AnalyzeResult(
                        arrayOf(),
                        unkToken(token.op),
                        AnalyzeFlag.EMPTY_SUB_EXPR
                    )
                    while (true) {
                        // 右括号，出栈直到遇到左括号
                        // 如果栈空了表示表达式不正确
                        if (rpnStack.isEmpty()) {
                            return AnalyzeResult(arrayOf(), unkToken(token.op), AnalyzeFlag.UNMATCHED_PARENTHESES)
                        }
                        val sign = rpnStack.pop()
                        if (sign.op == "(") break
                        else rpn.add(sign)
                    }
                }
            } else if (token.type == TokenType.FUNC) {
                if (supportFunc.contains(token.op))
                    rpnStack.push(token)
                else
                    return AnalyzeResult(arrayOf(), unkToken(token.op), AnalyzeFlag.UNK_FUNC)
            }
        }

        // 判断操作符栈是否不为空
        while (true) {
            if (rpnStack.isEmpty()) break
            // 超出预期的右括号 (通常不会发生)
            if (rpnStack.peek().op == ")") return AnalyzeResult(
                arrayOf(),
                unkToken(")"),
                AnalyzeFlag.UNMATCHED_PARENTHESES
            )
            rpn.add(rpnStack.pop())
        }

        return AnalyzeResult(rpn.toTypedArray(), unkToken(""))
    }

    fun calc(rpnExpr: Array<Token>): Float {
        val stack = Stack<Float>()
        for (token in rpnExpr) {
            when (token.type) {
                TokenType.NUM -> stack.push(token.num)
                TokenType.OP -> {
                    val b = stack.pop()
                    val a = stack.pop()
                    when (token.op) {
                        "+" -> stack.push(a + b)
                        "-" -> stack.push(a - b)
                        "*" -> stack.push(a * b)
                        "/" -> stack.push(a / b)
                        "^" -> stack.push(a.pow(b))
                        else -> {
                            // TODO: 异常处理，但是没必要
                        }
                    }
                }
                TokenType.FUNC -> {
                    val a = stack.pop()
                    when (token.op.lowercase()) {
                        "sqrt" -> stack.push(sqrt(a))
                        "ln" -> stack.push(ln(a))
                        "sin" -> stack.push(sin(a))
                        "cos" -> stack.push(cos(a))
                        "tan" -> stack.push(tan(a))
                        "asin" -> stack.push(asin(a))
                        "acos" -> stack.push(acos(a))
                        "atan" -> stack.push(atan(a))
                        else -> {} // TODO: 异常处理
                    }
                }
                else -> {
                    // TODO: Nothing to do.
                }
            }
        }
        return stack.pop()
    }
}