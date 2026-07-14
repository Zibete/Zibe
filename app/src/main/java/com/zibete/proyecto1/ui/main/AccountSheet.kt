package com.zibete.proyecto1.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.UiTags.ACCOUNT_AVATAR
import com.zibete.proyecto1.core.constants.Constants.UiTags.ACCOUNT_EDIT_PROFILE
import com.zibete.proyecto1.core.constants.Constants.UiTags.ACCOUNT_LOGOUT
import com.zibete.proyecto1.core.constants.Constants.UiTags.ACCOUNT_SETTINGS
import com.zibete.proyecto1.core.constants.Constants.UiTags.ACCOUNT_SHEET
import com.zibete.proyecto1.ui.components.ZibeBottomSheet
import com.zibete.proyecto1.ui.motion.zibePressable
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors

@Composable
fun AccountAvatarButton(
    photoUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val description = stringResource(R.string.account_open)
    Surface(
        modifier = modifier
            .size(48.dp)
            .testTag(ACCOUNT_AVATAR)
            .semantics { contentDescription = description }
            .clip(CircleShape)
            .zibePressable(role = Role.Button, onClick = onClick),
        shape = CircleShape,
        color = LocalZibeExtendedColors.current.cardBackground
    ) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            placeholder = painterResource(R.mipmap.logo_zibe_icon),
            error = painterResource(R.mipmap.logo_zibe_icon),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(4.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AccountSheet(
    isOpen: Boolean,
    displayName: String,
    email: String,
    photoUrl: String,
    onDismiss: () -> Unit,
    onEditProfile: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val colors = LocalZibeExtendedColors.current
    ZibeBottomSheet(
        isOpen = isOpen,
        onCancel = onDismiss,
        showCancelButton = false
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ACCOUNT_SHEET),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                placeholder = painterResource(R.mipmap.logo_zibe_icon),
                error = painterResource(R.mipmap.logo_zibe_icon),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
            )
            Column {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.lightText
                )
                if (email.isNotBlank()) {
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.hintText
                    )
                }
            }
        }

        AccountActionRow(
            iconRes = R.drawable.ic_person_24,
            title = stringResource(R.string.menu_edit_profile),
            testTag = ACCOUNT_EDIT_PROFILE,
            onClick = onEditProfile
        )
        AccountActionRow(
            iconRes = R.drawable.ic_settings_rounded_24,
            title = stringResource(R.string.menu_settings),
            testTag = ACCOUNT_SETTINGS,
            onClick = onSettings
        )
        AccountActionRow(
            iconRes = R.drawable.ic_logout_rounded_24,
            title = stringResource(R.string.logout),
            testTag = ACCOUNT_LOGOUT,
            onClick = onLogout
        )
    }
}

@Composable
private fun AccountActionRow(
    iconRes: Int,
    title: String,
    testTag: String,
    onClick: () -> Unit
) {
    val colors = LocalZibeExtendedColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag(testTag)
            .zibePressable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = colors.accent
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.lightText
        )
    }
}
