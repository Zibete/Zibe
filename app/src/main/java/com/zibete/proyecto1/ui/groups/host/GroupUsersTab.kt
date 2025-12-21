package com.zibete.proyecto1.ui.groups.host

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.model.UserGroup

@Composable
fun GroupUsersTab(
    state: GroupHostUiState,
    onUserClick: (UserGroup) -> Unit
) {
    if (state.users.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("Sin usuarios")
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        items(state.users, key = { it.userId + it.userName }) { user ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onUserClick(user) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(text = user.userName.ifBlank { "(sin nombre)" })
                Text(text = "uid: ${user.userId}", modifier = Modifier.padding(top = 2.dp))
            }
            Divider()
        }
    }
}
