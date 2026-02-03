package com.zibete.proyecto1.core.utils

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Copia el contenido de un Uri a un archivo temporal en la caché de la aplicación.
 * Útil para evitar errores de permisos "Unable to check Uri permission because caller is holding WM lock"
 * al procesar imágenes de la galería o cámara en segundo plano.
 */
fun Uri.copyToTempFile(context: Context, fileName: String = "temp_profile_upload.jpg"): Uri? {
    return try {
        val tempFile = File(context.cacheDir, fileName)
        context.contentResolver.openInputStream(this)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        Uri.fromFile(tempFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
