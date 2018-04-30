package com.dacostafaro.remy.tpgoffline

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Created by remy on 01/03/2018.
 */

class TrackView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    var color = Color.BLACK
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var isStart = false
    var isEnd = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawCircle(canvas)
        drawLine(canvas)
    }

    private fun drawCircle(canvas: Canvas) {
        paint.color = color
        paint.style = Paint.Style.FILL
        canvas.drawCircle(width / 2f, height / 2f, width / 3f, paint)
    }

    private fun drawLine(canvas: Canvas) {
        paint.color = color
        paint.style = Paint.Style.FILL
        val rect = when {
            isStart -> RectF(width / 9f * 3, height / 2f, (width / 9f) * 6, height + 0f)
            isEnd -> RectF(width / 9f * 3, 0f, (width / 9f) * 6, height / 2f)
            //else ->  RectF(width / 4f, 0f, width / 4f, 0f)
            else -> RectF(width / 9f * 3, 0f,  (width / 9f) * 6, height + 0f)
        }
        canvas.drawRect(rect, paint)
    }
}