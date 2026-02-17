package com.zibete.proyecto1.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.components.ZibeMenuDefaults
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import com.zibete.proyecto1.ui.theme.ZibeTheme
import de.hdodenhof.circleimageview.CircleImageView

@Composable
fun ChatTopBar(
    name: String,
    status: String,
    photoUrl: String?,
    notificationsEnabled: Boolean,
    selectionCount: Int,
    onBackClick: () -> Unit,
    onProfileClick: () -> Unit,
    onToggleNotifications: () -> Unit,
    onDeleteChat: () -> Unit,
    onDeleteSelected: () -> Unit,
    onClearSelection: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val zibeExtendedColors = LocalZibeExtendedColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(zibeExtendedColors.contentDarkBg)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (selectionCount > 0) onClearSelection() else onBackClick() }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.content_description_back),
                    tint = zibeExtendedColors.lightText
                )
            }

            AndroidView(
                modifier = Modifier.size(44.dp),
                factory = { context ->
                    CircleImageView(context).apply {
                        borderWidth = 0
                    }
                },
                update = { imageView ->
                    Glide.with(imageView)
                        .load(photoUrl)
                        .into(imageView)
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .clickable { onProfileClick() }
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = zibeExtendedColors.lightText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = zibeExtendedColors.hintText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (selectionCount > 0) {
                Text(
                    text = selectionCount.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = zibeExtendedColors.lightText,
                    modifier = Modifier.padding(end = 6.dp)
                )
                IconButton(onClick = onDeleteSelected) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        tint = zibeExtendedColors.lightText,
                        contentDescription = stringResource(R.string.delete)
                    )
                }
            } else {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            tint = zibeExtendedColors.lightText,
                            contentDescription = null
                        )
                    }
                    MaterialTheme(
                        shapes = MaterialTheme.shapes.copy(
                            extraSmall = RoundedCornerShape(ZibeMenuDefaults.Corner)
                        )
                    ) {
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            offset = ZibeMenuDefaults.Offset,
                            modifier = Modifier
                                .background(
                                    colorResource(R.color.zibe_surface).copy(
                                        alpha = ZibeMenuDefaults.BgAlpha
                                    )
                                )
                                .padding(vertical = 4.dp),
                            containerColor = Color.Transparent,
                            shadowElevation = ZibeMenuDefaults.Shadow
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(
                                            if (notificationsEnabled) R.string.menu_user_notifications_off
                                            else R.string.menu_user_notifications_on
                                        )
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onToggleNotifications()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_delete_chat)) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteChat()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "ChatTopBar_Normal", showBackground = true)
@Composable
private fun ChatTopBarPreviewNormal() {
    ZibeTheme {
        ChatTopBar(
            name = "Sofia",
            status = "online",
            photoUrl = null,
            notificationsEnabled = true,
            selectionCount = 0,
            onBackClick = {},
            onProfileClick = {},
            onToggleNotifications = {},
            onDeleteChat = {},
            onDeleteSelected = {},
            onClearSelection = {}
        )
    }
}

@Preview(name = "ChatTopBar_NotificationsOff", showBackground = true)
@Composable
private fun ChatTopBarPreviewNotificationsOff() {
    ZibeTheme {
        ChatTopBar(
            name = "Sofia",
            status = "last seen 5m",
            photoUrl = null,
            notificationsEnabled = false,
            selectionCount = 0,
            onBackClick = {},
            onProfileClick = {},
            onToggleNotifications = {},
            onDeleteChat = {},
            onDeleteSelected = {},
            onClearSelection = {}
        )
    }
}

@Preview(name = "ChatTopBar_SelectionMode", showBackground = true)
@Composable
private fun ChatTopBarPreviewSelectionMode() {
    ZibeTheme {
        ChatTopBar(
            name = "Sofia",
            status = "online",
            photoUrl = null,
            notificationsEnabled = true,
            selectionCount = 3,
            onBackClick = {},
            onProfileClick = {},
            onToggleNotifications = {},
            onDeleteChat = {},
            onDeleteSelected = {},
            onClearSelection = {}
        )
    }
}
