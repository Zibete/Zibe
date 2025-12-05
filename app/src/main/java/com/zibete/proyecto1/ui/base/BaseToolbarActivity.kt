package com.zibete.proyecto1.ui.base

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import com.zibete.proyecto1.R

abstract class BaseToolbarActivity : AppCompatActivity() {

    protected var toolbarMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView + setSupportActionBar los hace cada Activity concreta
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        toolbarMenu = menu

//        // SearchView (solo si la pantalla lo necesita)
//        val searchItem = menu.findItem(R.id.action_search)
//        val searchView = searchItem?.actionView as? SearchView
//        searchView?.let { onSearchViewReady(it) }

        return true
    }

    protected open fun onSearchViewReady(searchView: SearchView) {
        // override en Activities que usen búsqueda
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)

        for (i in 0 until menu.size()) {
            menu.getItem(i).isVisible = false
        }

        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }

//            // ---------- MainActivity ----------
//            R.id.action_settings -> {
//                onSettingsClicked()
//                true
//            }
//
//            R.id.action_unblock_users -> {
//                onUnblockUsersClicked()
//                true
//            }
//
//            R.id.action_unhide_chats -> {
//                onUnhideChatsClicked()
//                true
//            }
//
//            R.id.action_favorites -> {
//                onFavoritesClicked()
//                true
//            }
//
//            // ---------- ChatGroupFragment ----------
//            R.id.action_exit_group -> {
//                onExitGroupClicked()
//                true
//            }
//
//            // ---------- ChatActivity / SlideProfileActivity ----------
//            R.id.action_notifications_on -> {
//                onNotificationsOnClicked()
//                true
//            }
//
//            R.id.action_notifications_off -> {
//                onNotificationsOffClicked()
//                true
//            }
//
//            R.id.action_block -> {
//                onBlockUserClicked()
//                true
//            }
//
//            R.id.action_unblock -> {
//                onUnblockUserClicked()
//                true
//            }
//
//            R.id.action_delete_chat -> {
//                onDeleteChatClicked()
//                true
//            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // ===== Hooks para que cada Activity haga lo suyo =====

    protected open fun onSettingsClicked() {}
    protected open fun onUnblockUsersClicked() {}
    protected open fun onUnhideChatsClicked() {}
    protected open fun onFavoritesClicked() {}
    protected open fun onExitGroupClicked() {}
    protected open fun onNotificationsOnClicked() {}
    protected open fun onNotificationsOffClicked() {}
    protected open fun onBlockUserClicked() {}
    protected open fun onUnblockUserClicked() {}
    protected open fun onDeleteChatClicked() {}
}
