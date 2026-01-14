package com.zibete.proyecto1.ui.base

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

abstract class BaseToolbarActivity : AppCompatActivity() {

    protected lateinit var toolbar: MaterialToolbar
    protected open val toolbarMenuRes: Int? = null
    protected open val toolbarMenuVisiblePredicate: (Menu) -> Unit = { }
    protected fun setupToolbar(
        toolbar: MaterialToolbar,
        showBack: Boolean = true,
        handleBackInBase: Boolean = true,
        title: CharSequence = ""
    ) {
        this.toolbar = toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(showBack)
        supportActionBar?.title = title

        if (handleBackInBase) {
            toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        toolbarMenuRes?.let { menuInflater.inflate(it, menu) }
        return toolbarMenuRes != null
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        toolbarMenuVisiblePredicate(menu)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed(); true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}