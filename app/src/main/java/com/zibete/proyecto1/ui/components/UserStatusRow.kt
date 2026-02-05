package com.zibete.proyecto1.ui.components

import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.ui.theme.LocalZibeTypography

data class UserStatusUi(
    val text: String,
    val fontStyle: FontStyle
)

@Composable
fun UserStatus.toUi(): UserStatusUi {
    return when (this) {
        is UserStatus.Online -> UserStatusUi(
            text = stringResource(R.string.online),
            fontStyle = FontStyle.Normal
        )
        is UserStatus.TypingOrRecording -> UserStatusUi(
            text = text,
            fontStyle = FontStyle.Italic
        )
        is UserStatus.LastSeen -> UserStatusUi(
            text = text,
            fontStyle = FontStyle.Normal
        )
        is UserStatus.Offline -> UserStatusUi(
            text = stringResource(R.string.offline),
            fontStyle = FontStyle.Normal
        )
    }
}

@Composable
fun UserStatusRow(
    userStatus: UserStatus,
    modifier: Modifier = Modifier
) {
    val zibeColors = LocalZibeExtendedColors.current
    val zibeTypography = LocalZibeTypography.current
    val ui = userStatus.toUi()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.FiberManualRecord,
            contentDescription = null,
            tint = zibeColors.lightText,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = ui.text,
            style = zibeTypography.label.copy(
                fontStyle = ui.fontStyle,
                fontSize = 14.sp
            ),
            color = zibeColors.lightText
        )
    }
}
