package com.zibete.proyecto1.ui.groups.host

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.zibete.proyecto1.R
import com.zibete.proyecto1.model.UserGroup

@Composable
fun GroupUsersTab(
    state: GroupHostUiState,
    onUserClick: (UserGroup) -> Unit,
    onDismissActions: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenPrivateChat: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    ) {
        if (state.users.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.rooms_members_empty),
                    modifier = Modifier.padding(28.dp)
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(
                    items = state.users.sortedWith(
                        compareByDescending<UserGroup> { state.isCreator(it) }
                            .thenByDescending { state.isCurrentUser(it) }
                            .thenBy { it.resolvedDisplayName().lowercase() }
                    ),
                ) { member ->
                    RoomMemberRow(
                        member = member,
                        isCurrentUser = state.isCurrentUser(member),
                        isCreator = state.isCreator(member),
                        onClick = { onUserClick(member) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 76.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }

    state.selectedMember?.let { member ->
        MemberActionsSheet(
            member = member,
            onDismiss = onDismissActions,
            onOpenProfile = onOpenProfile,
            onOpenPrivateChat = onOpenPrivateChat
        )
    }
}

@Composable
private fun RoomMemberRow(
    member: UserGroup,
    isCurrentUser: Boolean,
    isCreator: Boolean,
    onClick: () -> Unit
) {
    val enabled = !isCurrentUser && member.userId.isNotBlank()
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button }
            .clickable(enabled = enabled, onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        leadingContent = { RoomMemberAvatar(member) },
        headlineContent = {
            Text(
                text = member.resolvedDisplayName(),
                fontWeight = if (isCurrentUser) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isCurrentUser) MemberLabel(stringResource(R.string.rooms_you))
                if (isCreator) MemberLabel(stringResource(R.string.rooms_creator))
                MemberLabel(
                    stringResource(
                        if (member.isAnonymous) {
                            R.string.rooms_anonymous_identity
                        } else {
                            R.string.rooms_public_identity
                        }
                    )
                )
            }
        }
    )
}

@Composable
private fun RoomMemberAvatar(member: UserGroup) {
    val publicPhoto = member.photoUrl.takeIf { !member.isAnonymous && it.isNotBlank() }
    if (publicPhoto != null) {
        Image(
            painter = rememberAsyncImagePainter(publicPhoto),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
    } else {
        Icon(
            imageVector = if (member.isAnonymous) {
                Icons.Outlined.AccountCircle
            } else {
                Icons.Outlined.Person
            },
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun MemberLabel(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MemberActionsSheet(
    member: UserGroup,
    onDismiss: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenPrivateChat: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.rooms_member_actions,
                    member.resolvedDisplayName()
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))
            if (!member.isAnonymous) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenProfile
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.rooms_open_profile))
                }
            }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                onClick = onOpenPrivateChat
            ) {
                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.rooms_open_private_chat))
            }
            Spacer(Modifier.size(20.dp))
        }
    }
}
