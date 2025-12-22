package com.zibete.proyecto1.ui.constants

object Constants {

    const val APP_NAME = "Zibe"
    const val DEFAULT_PROFILE_PHOTO_URL =
        "https://firebasestorage.googleapis.com/v0/b/zproyecto1.appspot.com/o/Users%2FimgPerfil%2Fuser%403x.png?alt=media&token=7a04b75f-abea-4670-928f-bc897524e8e8"


    // ==============================
    // Navigation & Requests IDs
    // ==============================
    const val REQUEST_LOCATION = 0
    const val FRAGMENT_ID_CHATLIST = 1
    const val FRAGMENT_ID_CHATGROUPLIST = 2
    const val CAMERA_SELECTED = 22
    const val PHOTO_SELECTED = 33
    const val MIC_SELECTED = 44
    const val PERMISSIONS_EDIT_PROFILE = 11

    // ==============================
    // Intents / Bundle Extras
    // ==============================
    const val EXTRA_SESSION_CONFLICT = "extra_session_conflict"
    const val EXTRA_USER_ID = "EXTRA_USER_ID"
    const val EXTRA_USER_IDS = "EXTRA_USER_IDS"
    const val EXTRA_START_INDEX = "EXTRA_START_INDEX"
    const val EXTRA_CHAT_ID = "otherUid"
    const val EXTRA_CHAT_NODE = "nodeType"

    const val EXTRA_GROUP_NAME = "groupName"

    // ==============================
    // Firebase RTDB Root Nodes
    // ==============================
    const val NODE_CHATS_ROOT = "Chats"
    const val NODE_GROUPS_ROOT = "Groups"
    const val NODE_USERS_ROOT = "Users"
    const val NODE_SESSIONS = "Sessions"

    // Sub-nodos comunes
    const val NODE_DM = "dm"
    const val NODE_GROUP_DM = "group_dm"
    const val NODE_CLIENT_DATA = "ClientData"
    const val NODE_STATUS = "Status"
    const val NODE_ACTIVE_VIEW = "ActiveView"
    const val NODE_CHATLIST = "ChatList"
    const val NODE_USERS_ACCOUNTS = "Accounts"
    const val NODE_USERS_DATA = "Data"
    const val NODE_FAVORITE_LIST = "FavoriteList"

    // ==============================
    // Model Keys (Inner Objects)
    // ==============================
    object StatusKeys {
        const val STATUS = "status"
        const val LAST_SEEN_MS = "lastSeenMs"
    }

    object ActiveViewKeys {
        const val ACTIVE_THREAD = "activeThread"
    }

    object ActiveThreadKeys {
        const val NODE_TYPE = "nodeType"
        const val OTHER_UID = "otherUid"
    }

    object ChatListKeys {
        const val READ_GROUP_MESSAGES = "readGroupMessages"
        const val UNREAD_GROUP_COUNT = "unreadGroupCount"
    }

    object ChatKeys {
        const val CONTENT = "content"
        const val DATE = "date"
        const val SENDER_UID = "senderUid"
        const val TYPE = "type"
        const val SEEN = "seen"
    }

    object ChatGroupKeys {
        const val CONTENT = "content"
        const val TIMESTAMP = "timestamp"
        const val USER_NAME = "userName"
        const val SENDER_UID = "senderUid"
        const val CHAT_TYPE = "chatType"
        const val USER_TYPE = "userType"
    }

    object GroupUserKeys {
        const val USER_TYPE = "type"
        const val USER_ID = "userId"
        const val USER_NAME = "userName"
        const val JOINED_AT_MS = "joinedAtMs"
    }

    object GroupChatKeys {
        const val CONTENT = "content"
        const val DATE = "date"
        const val NAME_USER = "nameUser"
        const val SENDER_UID = "senderUid"
        const val TYPE = "type"
        const val USER_TYPE = "userType"
    }

    object GroupMetaKeys {
        const val NAME = "name"
        const val DESCRIPTION = "description"
        const val CREATOR_UID = "creatorUid"
        const val TYPE = "type"
        const val USERS = "users"
        const val CREATED_AT = "createdAt"
        const val TOTAL_MESSAGES = "totalMessages"
    }

    object AccountsKeys {
        const val ID = "id"
        const val NAME = "name"
        const val BIRTHDATE = "birthDate"
        const val CREATED_AT = "createdAt"
        const val AGE = "age"
        const val EMAIL = "email"
        const val PHOTO_URL = "photoUrl"
        const val IS_ONLINE = "isOnline"
        const val DESCRIPTION = "description"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
    }

    object ConversationKeys {
        const val LAST_CONTENT = "lastContent"
        const val LAST_DATE = "lastDate"
        const val USER_ID = "userId"
        const val OTHER_ID = "otherId"
        const val OTHER_NAME = "otherName"
        const val OTHER_PHOTO = "otherPhotoUrl"
        const val STATE = "state"
        const val UNREAD_COUNT = "unreadCount"
        const val SEEN = "seen"
    }

    object SessionKeys {
        const val FCM_TOKEN = "fcmToken"
        const val ACTIVE_INSTALL_ID = "activeInstallId"
    }

    // ==============================
    // Message Types & States
    // ==============================
    const val MAX_CHAT_SIZE = 10_000

    const val MSG_TYPE_LEFT: Int = 1
    const val MSG_TYPE_MID: Int = 0
    const val MSG_TYPE_RIGHT: Int = 2

    const val MSG_INFO = 111
    const val MSG_TEXT = 100
    const val MSG_TEXT_SENDER_DLT = 101
    const val MSG_TEXT_RECEIVER_DLT = 102
    const val MSG_PHOTO = 200
    const val MSG_PHOTO_SENDER_DLT = 201
    const val MSG_PHOTO_RECEIVER_DLT = 202
    const val MSG_AUDIO = 300
    const val MSG_AUDIO_SENDER_DLT = 301
    const val MSG_AUDIO_RECEIVER_DLT = 302

    const val MSG_DELIVERED = 1
    const val MSG_RECEIVED = 2
    const val MSG_SEEN = 3

    const val CHAT_STATE_BLOQ = "bloq"
    const val CHAT_STATE_SILENT = "silent"
    const val CHAT_STATE_HIDE = "hide"

    const val PUBLIC_GROUP = 1
    const val PRIVATE_GROUP = 2
    const val ANONYMOUS_USER = 0
    const val PUBLIC_USER = 1

    const val EMPTY = "Empty"
    const val CALLING = "Calling"
    const val RINGING = "Ringing"

    // ==============================
    // Firebase Cloud Messaging (FCM)
    // ==============================
    const val AUTH = "key=AAAAhT_yccE:APA91bEJ26YPwH4F1a_ZQojK2jSmbTiA_v_-8j5EIDCiyuWFRJZtktMp3jr-5JB4YTcKbkVNdQN3t1U0C3UKp1XpxAZDR3DsW4nAlaTjfGVPE_BpD_sh0N8SH_eWdrcAhRPa6SW9W2Me"
    const val PAYLOAD_TYPE_DM = NODE_DM

    const val PAYLOAD_DISTANCE_METERS = "payload_distance_meters"
    const val PAYLOAD_AGE = "payload_age"
    const val PAYLOAD_NAME = "payload_name"
    const val PAYLOAD_PHOTO_URL = "payload_photo_url"
    const val PAYLOAD_DESCRIPTION = "payload_description"
    const val PAYLOAD_ONLINE = "payload_online"
    const val PAYLOAD_GROUPS_USERS = "payload_users"
    const val PAYLOAD_GROUPS_DATA = "payload_data"
    const val PAYLOAD_GROUPS_CATEGORY = "payload_category"

    object PayloadKeys {
        const val TYPE = ChatKeys.TYPE
        const val OTHER_ID = ConversationKeys.OTHER_ID
        const val OTHER_NAME = ConversationKeys.OTHER_NAME
        const val CONTENT = ChatKeys.CONTENT
        const val UNREAD_COUNT = ConversationKeys.UNREAD_COUNT
    }

    // ==============================
    // Storage & Paths
    // ==============================
    const val PATH_AUDIOS = "audios"
    const val PATH_PHOTOS = "photos"
    const val PATH_PROFILE_PHOTOS = "profile_photos"
    const val PROFILE_PHOTO = "profile.jpg"
    const val EXTENSION_AUDIO = ".m4a"
    const val EXTENSION_IMAGE = ".jpg"
    const val KEY_SEPARATOR = "_"

    // ==============================
    // Groups Logic (Sub-nodes)
    // ==============================
    const val NODE_GROUPS_USERS = "Users"
    const val NODE_GROUPS_CHAT = "Chat"
    const val NODE_GROUPS_META = "Meta"

    // ==============================
    // Legacy / Comentados
    // ==============================
    // const val NODE_UNREAD_COUNT = "msgNoLeidos"
    // const val NODE_CHAT_STATE = "estado"
    // const val KEY_ACTIVE_INSTALL_ID = "activeInstallId"
    // const val KEY_FCM_TOKEN = "fcmToken"
    // const val NODE_PUBLIC_GROUP_CHAT = ""
    // const val KEY_DELIVERY_STATUS = "wVisto"
    // const val KEY_RECEIVED_UNREAD = "noVisto"
    // const val KEY_SEEN_STATUS = "visto"
}