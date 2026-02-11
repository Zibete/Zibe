package com.zibete.proyecto1.ui.main.search

import androidx.appcompat.widget.SearchView
import com.zibete.proyecto1.ui.search.SearchHandler

class MainSearchCoordinator(
    private val handlerProvider: () -> SearchHandler?
) {

    private var searchView: SearchView? = null
    private var searchEnabled: Boolean = false

    fun bind(searchView: SearchView?) {
        this.searchView = searchView
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (searchEnabled) {
                    handlerProvider()?.onSearchQueryChanged(query)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (searchEnabled) {
                    handlerProvider()?.onSearchQueryChanged(newText)
                }
                return true
            }
        })

        if (!searchEnabled) {
            collapseAndClear()
        }
    }

    fun updateAvailability(enabled: Boolean) {
        searchEnabled = enabled
        if (!enabled) {
            collapseAndClear()
        }
    }

    fun isSearchOpen(): Boolean = searchView?.isIconified == false

    fun collapseAndClear(): Boolean {
        val view = searchView ?: return false
        handlerProvider()?.onSearchQueryChanged("")
        if (!view.isIconified) {
            view.onActionViewCollapsed()
        }
        view.setQuery("", false)
        return true
    }
}
