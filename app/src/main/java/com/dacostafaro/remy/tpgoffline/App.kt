package com.dacostafaro.remy.tpgoffline

import android.graphics.Color
import android.support.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dacostafaro.remy.tpgoffline.departures.Stop
import java.util.ArrayList
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.text.Normalizer

/**
 * Created by remy on 02/02/2018.
 */

class App {
    companion object {
        var stops: ArrayList<Stop> = ArrayList()
        var linesColors: ArrayList<LineColor> = ArrayList()
        fun backgroundForLine(line: String, alpha: String): Int = Color.parseColor("#" + alpha + (linesColors.firstOrNull { it.line == line }?.background ?: ""))
        fun textForLine(line: String, alpha: String): Int = Color.parseColor("#" + alpha + (linesColors.firstOrNull { it.line == line }?.text ?: ""))
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