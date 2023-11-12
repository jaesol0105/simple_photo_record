package com.beinny.android.photorecord.ui.common

object OrderKoreanFirst {
    private const val LEFT = -1
    private const val RIGHT = 1

    /** [한글 > 영문 > 숫자 > 특수기호 순으로 비교]*/
    fun compare(left: String, right: String) : Int {
        val _left = left.uppercase().filterNot(Char::isWhitespace)
        val _right = right.uppercase().filterNot(Char::isWhitespace)

        val (llen, rlen) = _left.length to _right.length
        val mlen = llen.coerceAtMost(rlen)

        for(i in 0 until mlen){
            var (lc, rc) = _left[i] to _right[i]

            if(lc != rc) {
                lc = isKoreanConsonant(lc)
                rc = isKoreanConsonant(rc)
                return if(conditionKoreanAndEnglish(lc, rc) || conditionKoreanAndNumber(lc, rc) || conditionKoreanAndSpecial(lc, rc) || conditionEnglishAndNumber(lc, rc))
                    -(lc - rc)
                else if(conditionEnglishAndSpecial(lc, rc) || conditionNumberAndSpecial(lc, rc)){
                    if(isEnglish(lc) || isNumber(lc)) LEFT else RIGHT
                }
                else { lc - rc }
            }
        }

        return llen - rlen
    }

    // 어차피 대문자로 치환한 후 비교 하기 때문에 대문자만 비교하는 것으로 변경
    private fun isEnglish(ch: Char) : Boolean = ch in 'A'..'Z'
    private fun isKorean(ch: Char) : Boolean = ch in '가'..'힣'
    private fun isNumber(ch: Char) : Boolean = ch in '0'..'9'
    private fun isSpecial(ch : Char) : Boolean = ch in '!'..'/' || ch in ':'..'@' || ch in '['..'`' || ch in '{'.. '~'
    private fun isKoreanConsonant(ch: Char): Char {
        return when (ch) {
            'ㄱ' -> ('가'-1)
            'ㄴ' -> ('나'-1)
            'ㄷ' -> ('다'-1)
            'ㄹ' -> ('라'-1)
            'ㅁ' -> ('마'-1)
            'ㅂ' -> ('바'-1)
            'ㅅ' -> ('사'-1)
            'ㅇ' -> ('아'-1)
            'ㅈ' -> ('자'-1)
            'ㅊ' -> ('차'-1)
            'ㅋ' -> ('카'-1)
            'ㅌ' -> ('타'-1)
            'ㅍ' -> ('파'-1)
            'ㅎ' -> ('하'-1)
            else -> ch
        }
    }

    private fun conditionKoreanAndEnglish(c1: Char, c2: Char) = isKorean(c1) && isEnglish(c2) || isEnglish(c1) && isKorean(c2)
    private fun conditionEnglishAndSpecial(c1: Char, c2: Char) = isEnglish(c1) && isSpecial(c2) || isSpecial(c1) && isEnglish(c2)
    private fun conditionNumberAndSpecial(c1: Char, c2: Char) = isNumber(c1) && isSpecial(c2) || isSpecial(c1) && isNumber(c2)
    private fun conditionKoreanAndNumber(c1: Char, c2: Char) = isKorean(c1) && isNumber(c2) || isNumber(c1) && isKorean(c2)
    private fun conditionKoreanAndSpecial(c1: Char, c2: Char) = isKorean(c1) && isSpecial(c2) || isSpecial(c1) && isKorean(c2)
    private fun conditionEnglishAndNumber(c1: Char, c2: Char) = isEnglish(c1) && isNumber(c2) || isNumber(c1) && isEnglish(c2)
}