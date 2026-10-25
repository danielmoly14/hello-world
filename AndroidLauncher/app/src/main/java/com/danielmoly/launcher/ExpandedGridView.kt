package com.danielmoly.launcher

import android.content.Context
import android.util.AttributeSet
import android.widget.GridView

/** GridView que crece hasta mostrar todas sus filas (un GridView normal con wrap_content muestra solo una). */
class ExpandedGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GridView(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val expanded = MeasureSpec.makeMeasureSpec(Int.MAX_VALUE shr 2, MeasureSpec.AT_MOST)
        super.onMeasure(widthMeasureSpec, expanded)
    }
}
