package com.zibete.proyecto1.ui.constants

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.Date

object Constants {


    const val DEFAULT_PROFILE_PHOTO_URL =
        "https://firebasestorage.googleapis.com/v0/b/zproyecto1.appspot.com/o/Users%2FimgPerfil%2Fuser%403x.png?alt=media&token=7a04b75f-abea-4670-928f-bc897524e8e8"



    //EditProfile:
    const val MSG_PROFILE_SAVED = "Perfil guardado con éxito"
    const val MSG_PROFILE_SAVE_ERROR = "Error guardando perfil"
    const val MSG_PROFILE_LOAD_ERROR = "No se pudo cargar el perfil"
    const val MSG_CAMERA_ERROR = "No se pudo abrir la cámara"
    const val MSG_CAMERA_PERMISSION_REQUIRED = "Necesitás otorgar permiso de cámara para sacar una foto."

    const val EXTRA_SESSION_CONFLICT = "extra_session_conflict"


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




    const val REQUEST_LOCATION = 0
    const val FRAGMENT_ID_CHATLIST = 1
    const val FRAGMENT_ID_CHATGROUPLIST = 2

    const val CAMERA_SELECTED = 22
    const val PHOTO_SELECTED = 33
    const val MIC_SELECTED = 44
    const val PERMISSIONS_EDIT_PROFILE = 11

    const val MAXCHATSIZE = 10000


    const val MSG_TYPE_LEFT: Int = 1
    const val MSG_TYPE_MID: Int = 0
    const val MSG_TYPE_RIGHT: Int = 2

    const val INFO = 111

    const val MSG_TEXT = 100
    const val MSG_TEXT_SENDER_DLT = 101
    const val MSG_TEXT_RECEIVER_DLT = 102

    const val MSG_PHOTO = 200
    const val MSG_PHOTO_SENDER_DLT = 201
    const val MSG_PHOTO_RECEIVER_DLT = 202

    const val MSG_AUDIO = 300
    const val MSG_AUDIO_SENDER_DLT = 301
    const val MSG_AUDIO_RECEIVER_DLT = 302

    const val PUBLIC_GROUP = 1
    const val PRIVATE_GROUP = 2


    const val ANONYMOUS_USER = 0
    const val PUBLIC_USER = 1

    // ChatActivity

    const val EXTRA_USER_ID = "EXTRA_USER_ID"
    const val EXTRA_USER_IDS = "EXTRA_USER_IDS"
    const val EXTRA_START_INDEX = "EXTRA_START_INDEX"




    const val EXTRA_CHAT_ID = "userId"
    const val EXTRA_CHAT_NODE = "nodeType"









//    const val NODE_UNREAD_COUNT = "msgNoLeidos"
//    const val NODE_CHAT_STATE = "estado"

//    const val KEY_ACTIVE_INSTALL_ID = "activeInstallId"
//    const val KEY_FCM_TOKEN = "fcmToken"

    const val APP_NAME = "Zibe"


    // ==============================
    // Firebase RTDB Root Nodes
    // ==============================
    const val NODE_CHATS_ROOT = "Chats"
    const val NODE_GROUPS_ROOT = "Groups"
    const val NODE_USERS_ROOT = "Users"
    const val NODE_SESSIONS = "Sessions"

    // ==============================
    // Chats node_type (under /Chats)
    // ==============================
    const val NODE_DM = "dm"                 // Chat 1 a 1
    const val NODE_GROUP_PRIVATE_DM = "group_dm" // Chat privado dentro de un grupo

    object ChatKeys {
        const val CONTENT = "content"  // String message
        const val DATE = "date"        // String DD/MM/YYYY HH:MM:SS
        const val SENDER_UID = "senderUid"    // String uid
        const val TYPE = "type"        // Int MSG_TEXT, MSG_PHOTO, MSG_AUDIO...
        const val SEEN = "seen"        // Int MSG_DELIVERED, MSG_RECEIVED, MSG_SEEN
    }

    // ==============================
    // Groups
    // ==============================
    const val NODE_GROUPS_USERS = "Users"
    const val NODE_GROUPS_CHAT = "Chat"
    const val NODE_GROUPS_DATA = "Data"

    object GroupUserKeys {
        const val TYPE = "type"             // Int ANONYMOUS_USER o PUBLIC_USER
        const val USER_ID = "userId"        // String uid
        const val USER_NAME = "userName"    // String userName
    }

    object GroupChatKeys {
        const val CONTENT = "content"           // String message (legacy)
        const val DATE = "date"                 // String DD/MM/YYYY HH:MM:SS
        const val NAME_USER = "nameUser"
        const val SENDER_UID = "senderUid"      // String uid sender
        const val TYPE = "type"             // Int MSG_TEXT, MSG_PHOTO...
        const val USER_TYPE = "userType"       // Int
    }

    object GroupDataKeys {
        const val NAME = "name"        // String groupName
        const val DESCRIPTION = "description" // String
        const val CREATOR_UID = "creatorUid" // String uid
        const val TYPE = "type"    // Int PUBLIC_GROUP o PRIVATE_GROUP
        const val USERS = "users"      // Int count
        const val CREATED_AT = "createdAt" // String DD/MM/YYYY HH:MM
    }

    // ==============================
    // Users
    // ==============================
    const val NODE_USERS_ACCOUNTS = "Accounts"
    const val NODE_USERS_DATA = "Data"

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

    // ==============================
    // Users/Data
    // ==============================
    const val NODE_FAVORITE_LIST = "FavoriteList"
    const val NODE_CHATLIST = "ChatList"
    const val NODE_STATUS = "Status"

    object ChatListKeys {
        const val ACTIVE_CHAT = "Actual"
        const val READ_GROUP_MESSAGES = "readGroupMessages"
        const val UNREAD_GROUP_COUNT = "unreadGroupCount"
    }

    object StatusKeys {
        const val STATUS = "status"
        const val LAST_SEEN_MS = "lastSeenMs"
    }

    // Threads metadata under /Users/Data/{uid}/{NODE_DM or NODE_GROUP_PRIVATE_DM}/{otherId}
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


    // ==============================
    // Sessions
    // ==============================
    object SessionKeys {
        const val FCM_TOKEN = "fcmToken"
        const val ACTIVE_INSTALL_ID = "activeInstallId"
    }

    // ==============================
    // FCM payload contract (data)
    // ==============================
    const val PAYLOAD_TYPE_DM = NODE_DM // "dm"
    // Para grupos: data["type"] = groupName (recomendado)















    const val PATH_AUDIOS = "audios"
    const val PATH_PHOTOS = "photos"

    const val PATH_PROFILE_PHOTOS = "profile_photos"

    const val PROFILE_PHOTO = "profile.jpg"


    const val EXTENSION_AUDIO = ".m4a"
    const val EXTENSION_IMAGE = ".jpg"



//    const val NODE_PUBLIC_GROUP_CHAT = ""



    const val AUTH = "key=AAAAhT_yccE:APA91bEJ26YPwH4F1a_ZQojK2jSmbTiA_v_-8j5EIDCiyuWFRJZtktMp3jr-5JB4YTcKbkVNdQN3t1U0C3UKp1XpxAZDR3DsW4nAlaTjfGVPE_BpD_sh0N8SH_eWdrcAhRPa6SW9W2Me"


    const val CHAT_STATE_BLOQ = "bloq"
    const val CHAT_STATE_SILENT = "silent"
    const val CHAT_STATE_HIDE = "hide"


//    const val KEY_DELIVERY_STATUS = "wVisto"
//    const val KEY_RECEIVED_UNREAD = "noVisto"
//    const val KEY_SEEN_STATUS = "visto"


    const val MSG_DELIVERED = 1
    const val MSG_RECEIVED = 2
    const val MSG_SEEN = 3



    const val EMPTY = "Empty"
    const val CALLING = "Calling"
    const val RINGING = "Ringing"

    val storage: FirebaseStorage = FirebaseStorage.getInstance()
    val storageReference: StorageReference = storage.reference


}