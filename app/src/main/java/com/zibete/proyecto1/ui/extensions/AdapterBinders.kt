package com.zibete.proyecto1.ui.extensions

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.zibete.proyecto1.core.constants.Constants.MSG_DELIVERED
import com.zibete.proyecto1.core.constants.Constants.MSG_SEEN
import com.zibete.proyecto1.core.designsystem.R as DsR
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.R

fun View.bindStatusIndicator(ctx: Context, status: UserStatus) {
    val fillRes = when (status) {
        is UserStatus.Online,
        is UserStatus.TypingOrRecording -> DsR.color.status_online

        is UserStatus.LastSeen,
        is UserStatus.Offline -> DsR.color.status_offline
    }
    val fillColor = ContextCompat.getColor(ctx, fillRes)
    val bg = (background?.mutate() as? GradientDrawable)
        ?: (ContextCompat.getDrawable(ctx, R.drawable.shape_status_circle)
            ?.mutate() as GradientDrawable)
    bg.setColor(fillColor)
    if (background !== bg) background = bg
}

fun ImageView.bindChecks(isMine: Boolean, seen: Int) {
    if (!isMine) {
        isVisible = false
        imageTintList = null
        return
    }

    isVisible = true
    val iconRes = if (seen == MSG_DELIVERED) {
        R.drawable.ic_check_24
    } else {
        R.drawable.ic_double_check_24
    }
    setImageResource(iconRes)

    val tintRes = if (seen == MSG_SEEN) DsR.color.check_seen else DsR.color.check_not_seen
    val tintColor = ContextCompat.getColor(context, tintRes)
    imageTintList = ColorStateList.valueOf(tintColor)
}

fun ImageView.loadAvatar(url: String?) {
    val safeUrl = url?.takeIf { it.isNotBlank() }
    loadProfileImageSafe(
        model = safeUrl,
        placeholderRes = R.drawable.ic_person_24,
        errorRes = R.drawable.ic_person_24
    )
}




