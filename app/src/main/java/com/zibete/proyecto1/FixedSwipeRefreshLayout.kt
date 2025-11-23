package com.zibete.proyecto1

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class FixedSwipeRefreshLayout : SwipeRefreshLayout {
    private var recyclerView: RecyclerView? = null

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context) : super(context)

    fun setRecyclerView(recyclerView: RecyclerView?) {
        this.recyclerView = recyclerView
    }

    override fun canChildScrollUp(): Boolean {
        if (recyclerView != null) {
            return recyclerView!!.canScrollVertically(-1)
        }

        return false
    }
}
