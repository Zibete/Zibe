package com.zibete.proyecto1.di.firebase

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.zibete.proyecto1.ui.constants.Constants.APP_NAME
import com.zibete.proyecto1.ui.constants.Constants.NODE_CHATS_ROOT
import com.zibete.proyecto1.ui.constants.Constants.NODE_DM
import com.zibete.proyecto1.ui.constants.Constants.NODE_GROUPS_CHAT
import com.zibete.proyecto1.ui.constants.Constants.NODE_GROUPS_META
import com.zibete.proyecto1.ui.constants.Constants.NODE_GROUPS_ROOT
import com.zibete.proyecto1.ui.constants.Constants.NODE_GROUPS_USERS
import com.zibete.proyecto1.ui.constants.Constants.NODE_GROUP_DM
import com.zibete.proyecto1.ui.constants.Constants.NODE_SESSIONS
import com.zibete.proyecto1.ui.constants.Constants.NODE_USERS_ACCOUNTS
import com.zibete.proyecto1.ui.constants.Constants.NODE_USERS_DATA
import com.zibete.proyecto1.ui.constants.Constants.NODE_USERS_ROOT
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRefsContainer @Inject constructor(
    val firebaseDatabase: FirebaseDatabase, val firebaseStorage: FirebaseStorage
) {

    // ================= Usuarios =================
    val refUsers: DatabaseReference = firebaseDatabase.getReference(NODE_USERS_ROOT)
    val refData: DatabaseReference = refUsers.child(NODE_USERS_DATA)
    val refAccounts: DatabaseReference = refUsers.child(NODE_USERS_ACCOUNTS)

    // ================= Chats =================

    val refChatsRoot: DatabaseReference = firebaseDatabase.getReference(NODE_CHATS_ROOT)
    val refChatsDm: DatabaseReference = refChatsRoot.child(NODE_DM)
    val refChatsGroupDm: DatabaseReference = refChatsRoot.child(NODE_GROUP_DM)

    // ================= Sesiones =================

    val refSessions: DatabaseReference = firebaseDatabase.getReference(NODE_SESSIONS)

    // ================= Grupos =================

    val refGroupsRoot: DatabaseReference = firebaseDatabase.getReference(NODE_GROUPS_ROOT)
    val refGroupMeta: DatabaseReference = refGroupsRoot.child(NODE_GROUPS_META)
    val refGroupChat: DatabaseReference = refGroupsRoot.child(NODE_GROUPS_CHAT)
    val refGroupUsers: DatabaseReference = refGroupsRoot.child(NODE_GROUPS_USERS)

    // ================= Zibe =================

    val refZibe: DatabaseReference = firebaseDatabase.getReference(APP_NAME)

}
