package com.zibete.proyecto1;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class FixedSwipeRefreshLayout extends SwipeRefreshLayout {

    private RecyclerView recyclerView;

    public FixedSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedSwipeRefreshLayout(Context context) {
        super(context);
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    @Override
    public boolean canChildScrollUp() {
        if (recyclerView!=null) {
            return recyclerView.canScrollVertically(-1);
        }

        return false;
    }

}
