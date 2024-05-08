//package org.NuclearPasta.brainklot.interpreter

const val TT_INC: String = "INC"
const val TT_DEC: String = "DEC"
const val TT_FORE: String = "FORE"
const val TT_BACK: String = "BACK"
const val TT_OPENING_BRACKET: String = "OPENING_BRACKET"
const val TT_CLOSING_BRACKET: String = "CLOSING_BRACKET"
const val TT_INPUT: String = "INPUT"
const val TT_PRINT: String = "PRINT"
const val TT_NEWLINE: String = "NEWLINE"
const val TT_EOF: String = "EOF"

//const val VALID_SYMBOLS: String = "+-<>[],."

/*
open class Error(private var cn: Int, private var ln: Int, private val errorName: String, private val details: String) {

    override fun toString(): String {
        return "errorName: $details at line $ln, character $cn"
    }
}

class MismatchedBracketError(charIndex: Int, lineIndex: Int) : Error(charIndex, lineIndex, "MismatchedBracketError", "mis-matched bracket('[' and ']')")

*/

class Token(private val type: String, private val value: Int? = null) {

    fun getType(): String {
        return type
    }

    fun getValue(): Int? {
        return value
    }

    override fun toString(): String {
        return "$type: $value"
    }
}

class Position(private var col: Int = -1, private var ln: Int = -1) {
    fun advance(char: Char? = null) {
        col++
        if (char == '\n') ln++
    }

    fun getCol(): Int {
        return col
    }

    /*
    fun getLn(): Int {
        return ln
    }

    fun copy(): Position {
        return Position(col, ln)
    }
     */
}

class Lexer(private val code: String) {
    private var currentCharacter: Char? = null
    private val pos = Position()

    private fun advance() {
        pos.advance(currentCharacter)
        currentCharacter = if (pos.getCol() > code.length - 1) null else code[pos.getCol()]
    }

    fun lexToTokens(): (MutableList<Token>) {
        val tokens: MutableList<Token> = arrayListOf()
        if (currentCharacter == null) advance()

        while (currentCharacter != null) {
            when (currentCharacter) {
                '+' -> {
                    tokens.add(Token(TT_INC, getSymbol('+')))
                }

                '-' -> {
                    tokens.add(Token(TT_DEC, getSymbol('-')))
                }

                '>' -> {
                    tokens.add(Token(TT_FORE, getSymbol('>')))
                }

                '<' -> {
                    tokens.add(Token(TT_BACK, getSymbol('<')))
                }

                '[' -> {
                    tokens.add(Token(TT_OPENING_BRACKET)); advance()
                }

                ']' -> {
                    tokens.add(Token(TT_CLOSING_BRACKET)); advance()
                }

                '.' -> {
                    tokens.add(Token(TT_PRINT, getSymbol('.')))
                }

                ',' -> {
                    tokens.add(Token(TT_INPUT)); advance()
                }

                '\n' -> {
                    tokens.add(Token(TT_NEWLINE)); advance()
                }

                else -> {
                    advance()
                }
            }
        }

        tokens.add(Token(TT_EOF))
        return tokens
    }

    private fun getSymbol(symbol: Char): Int {
        var symbolCount = 0
        while (currentCharacter == symbol) {
            symbolCount++; advance()
        }
        return symbolCount
    }
}

fun resolveOverflow(number: Int, amount: Int): Int {
    var out = number
    while (out > amount) {
        out -= amount
    }
    return out
}

fun resolveUnderflow(number: Int, amount: Int): Int {
    var out = number
    while (out < amount) {
        out += amount
    }
    return out
}

fun interpreter(tokens: MutableList<Token>) {
    val bytes = IntArray(30000)
    var pointer = 0

    // parser
    val jumps: MutableMap<Int, Int> = mutableMapOf()
    val jumpStack: MutableList<Int> = arrayListOf()
    var jumpNest = 0
    var line = 1
    var i = 0
    var actual = 0
    while (i < tokens.size) {
        if (tokens[i].getType() == TT_OPENING_BRACKET && jumpNest == 0) {
            jumpStack.addLast(i)
            jumpNest++
            i++
        } else if (tokens[i].getType() == TT_CLOSING_BRACKET) {
            if (jumpNest > 0) {
                val begin = jumpStack.removeLast()
                jumps[begin] = i
                jumps[i] = begin
                jumpNest--
            } else {
                println("Error: unopened ']' at line $line, character ${((i + actual) + 1) - 2}")
                return
            }
            i++
        } else if (tokens[i].getType() == TT_NEWLINE) {
            line++
            i++
        } else if (tokens[i].getType() == TT_EOF && jumpNest > 0) {
            println("Error: unclosed '[' at line $line, character ${((jumpStack[jumpStack.size - 1] + actual) + 1) - 1}")
            return
        } else if (jumpNest < 0) {
            println("Error: jumpNest has gone into the negatives")
            return
        } else {
            if (tokens[i].getValue() != null) {
                if (tokens[i].getValue()!! > 1) actual += tokens[i].getValue()!!
            }
            i++
        }
    }

    // interpreter
    var tn = 0
    while (tn < tokens.size) {
        val t = tokens[tn]
        //println(t.getType())
        when (t.getType()) {
            TT_EOF -> break
            TT_INC -> {
                val value = t.getValue()!!
                bytes[pointer] += if (value > 127) resolveOverflow(value, 127) else value
                tn++
            }

            TT_DEC -> {
                val value = t.getValue()!!
                bytes[pointer] -= if (value < 0) resolveUnderflow(value, 0) else value
                tn++
            }

            TT_FORE -> {
                val value = t.getValue()!!
                pointer += if (value > 29999) resolveOverflow(value, 29999) else value
                tn++
            }

            TT_BACK -> {
                val value = t.getValue()!!
                pointer -= if (value < 0) resolveUnderflow(value, 0) else value
                tn++
            }

            TT_OPENING_BRACKET -> {
                if (bytes[pointer] == 0) {
                    tn = jumps[tn]!! + 1
                } else {
                    tn++
                }
            }

            TT_CLOSING_BRACKET -> {
                if (bytes[pointer] != 0) {
                    tn = jumps[tn]!! + 1
                } else {
                    tn++
                }
            }

            TT_PRINT -> {
                println("b$pointer: ${bytes[pointer]}"); tn++
            }

            TT_INPUT -> {
                println("please input a number between 0 and 127 inclusive")
                while (true) {
                    print(":: ")
                    val given: String = readlnOrNull() ?: continue
                    try {
                        val num = given.toInt()
                        if (num < 0 || num > 127) {
                            println("not between 0 and 127 inclusive")
                            continue
                        }
                        bytes[pointer] = num
                    } catch (e: NumberFormatException) {
                        println("invalid input"); continue
                    }
                    break
                }
                tn++
            }

            else -> tn++
        }
    }
}

fun runInterpreter(code: String) {
    val lexer = Lexer(code)
    val tokenizedCode = lexer.lexToTokens()
    println(tokenizedCode)
    interpreter(tokenizedCode)
}