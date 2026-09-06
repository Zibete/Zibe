package com.zibete.proyecto1.local

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/** Runs before Application.onCreate and Hilt can construct any Firebase consumer. */
class LocalFirebaseInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        LocalFirebaseBackend.initialize(checkNotNull(context))
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = throw UnsupportedOperationException("Initialization provider has no data API")

    override fun getType(uri: Uri): String? =
        throw UnsupportedOperationException("Initialization provider has no data API")

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("Initialization provider has no data API")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("Initialization provider has no data API")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = throw UnsupportedOperationException("Initialization provider has no data API")
}
