package com.dacostafaro.remy.tpgoffline

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.annotation.LayoutRes
import com.dacostafaro.remy.tpgoffline.json.Departure
import java.text.Normalizer
import java.util.ArrayList
import kotlin.Comparator

/**
 * Created by remy on 02/02/2018.
 */

class App {
    companion object {
        var stops: ArrayList<Stop> = ArrayList()
        var darkMode = false
        var linesColors: ArrayList<LineColor> = ArrayList()
        const val tpgApiKey: String = "d95be980-0830-11e5-a039-0002a5d5c51b"
        fun backgroundForLine(line: String, alpha: String): Int = Color.parseColor("#" + alpha + (linesColors.firstOrNull { it.line == line }?.background ?: ""))
        fun textForLine(line: String, alpha: String): Int = Color.parseColor("#" + alpha + (linesColors.firstOrNull { it.line == line }?.text ?: ""))
    }
}

class TransitionsObjects {
    companion object {
        var departure: Departure? = null
    }
}

val String.escaped
    get() = this.toLowerCase().replace("+", "").replace(" ", "").replace("-", "").removeAccents

val String.removeAccents
    get() = Normalizer.normalize(this, Normalizer.Form.NFD).replace("[\\p{InCombiningDiacriticalMarks}]", "")

fun Iterable<String>.sortedWithInt(): List<String> {
    return this.sortedWith(Comparator { lhs, rhs ->
        try {
            val i1 = lhs.toInt()
            val i2 = rhs.toInt()
            when {
                i1 < i2 -> -1
                i1 > i2 -> 1
                else -> 0
            }
        } catch (e: NumberFormatException) {
            when {
                lhs < rhs -> -1
                lhs > rhs -> 1
                else -> 0
            }
        }

    })
}

class LineColor(
    val line: String,
    val text: String,
    val background: String
)

fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
}

fun Activity.dismissKeyboard() {
    val inputMethodManager = getSystemService( Context.INPUT_METHOD_SERVICE ) as InputMethodManager
    if( inputMethodManager.isAcceptingText )
        inputMethodManager.hideSoftInputFromWindow(this.currentFocus.windowToken, /*flags:*/ 0)
}