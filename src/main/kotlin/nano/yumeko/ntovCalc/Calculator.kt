package nano.yumeko.ntovCalc

import java.util.Stack

class Calculator {
    @JvmField
    val scanSignList = "()+-*/%^"

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

    class AnalyzeResult(_result: Array<Token>, reason: Token) {
        val result = _result
        val errorReason = reason
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
        fun opFunc(a: Float, b: Float, o: String): Float {
            // 词法分析后不可能出现除 + - * / % ( ) 外的其他字符
            // 调用时在括号判断后只会出现 + - * / %
            return when (o) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> a / b
                "%" -> a % b
                else -> a
            }
        }

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
//                        val tsrpn = rpnStack.peek().op
//                        if (tsrpn == "+" || tsrpn == "-" || tsrpn == "(") {// 操作符比栈顶优先级大
//                            rpnStack.push(token)
//
//                        } else {
//                            rpn.add(token)
//                        }
//                    }
                        val topOp = rpnStack.peek().op
                        if (topOp == "+" || topOp == "-" || topOp == "(") {// 操作符比栈顶优先级大
                            rpnStack.push(token)
                        } else {
                            while (true) {
                                if (rpnStack.isEmpty()) break
                                val topOp = rpnStack.peek().op
                                if (topOp == "+" || topOp == "-" || topOp == "(") break
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
//                    } else if (rpnStack.peek().op == "(") {
//                        rpnStack.push(token)
//                    } else {
//                        rpn.add(token)
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
                    while (true) {
                        // 右括号，出栈直到遇到左括号
                        // 如果栈空了表示表达式不正确
                        if (rpnStack.isEmpty()) {
                            return AnalyzeResult(arrayOf(), unkToken(token.op))
                        }
                        val sign = rpnStack.pop()
                        if (sign.op == "(") break
                        else rpn.add(sign)
                    }
                }
            } else if (token.type == TokenType.FUNC) {
                // 运算时得处理
//                rpn.add(token)
                rpnStack.push(token)
            }
        }

        // 判断操作符栈是否不为空
        while (true) {
            if (rpnStack.isEmpty()) break
            if (rpnStack.peek().op == ")") return AnalyzeResult(arrayOf(), unkToken(")"))
            rpn.add(rpnStack.pop())
        }

        return AnalyzeResult(rpn.toTypedArray(), unkToken(""))
    }

    fun calc(rpnExpr: Array<Token>): Float {
        val stack = Stack<Float>()
        for (token in rpnExpr) {
            if (token.type == TokenType.NUM) stack.push(token.num)
            else if (token.type == TokenType.OP) {
                val b = stack.pop()
                val a = stack.pop()
                when (token.op) {
                    "+" -> stack.push(a + b)
                    "-" -> stack.push(a - b)
                    "*" -> stack.push(a * b)
                    "/" -> stack.push(a / b)
                    "^" -> stack.push(Math.pow(a.toDouble(), b.toDouble()).toFloat())
                    else -> {}
                }
            } else if (token.type == TokenType.FUNC) {
                val a = stack.pop()
                when (token.op.lowercase()) {
                    "sqrt" -> stack.push(Math.sqrt(a.toDouble()).toFloat())
                    else -> {} // TODO: 异常处理
                }
            }
        }
        return stack.pop()
    }
}