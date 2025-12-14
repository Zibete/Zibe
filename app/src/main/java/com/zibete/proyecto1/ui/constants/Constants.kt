package com.zibete.proyecto1.ui.constants

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

object Constants {

    const val DEFAULT_PROFILE_PHOTO_URL =
        "https://firebasestorage.googleapis.com/v0/b/zproyecto1.appspot.com/o/Users%2FimgPerfil%2Fuser%403x.png?alt=media&token=7a04b75f-abea-4670-928f-bc897524e8e8"

    object PrefKeys {
        const val ONBOARDING_DONE = "onBoarding"
        const val FIRST_LOGIN_DONE = "flag_Splash"
    }



    const val PAYLOAD_DISTANCE_METERS = "payload_distance_meters"
    const val PAYLOAD_AGE = "payload_age"
    const val PAYLOAD_NAME = "payload_name"
    const val PAYLOAD_PHOTO_URL = "payload_photo_url"
    const val PAYLOAD_DESCRIPTION = "payload_description"
    const val PAYLOAD_ONLINE = "payload_online"










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

    const val EXTRA_USER_ID = "EXTRA_USER_ID"
    const val EXTRA_USER_IDS = "EXTRA_USER_IDS"
    const val EXTRA_START_INDEX = "EXTRA_START_INDEX"

    const val EXTRA_CHAT_ID = "userId"
    const val EXTRA_CHAT_NODE = "nodeType"



    const val NODE_FAVORITE_LIST = "FavoriteList"
    const val NODE_CHAT_MESSAGE = "ChatMessage"
    const val NODE_CURRENT_CHAT = "ChatWith" // Chat entre perfiles públicos -->
    const val NODE_CHATLIST = "ChatList"

    const val NODE_CHATS = "Chats"
    const val NODE_GROUP_CHAT = "Unknown" // Chat dentro de un grupo -> pero tambien puede ser dos perfiles publicos -- si el


    const val NODE_ACTIVE_CHAT = "Actual"

    const val PATH_AUDIOS = "audios"
    const val PATH_PHOTOS = "photos"

    const val EXTENSION_AUDIO = ".m4a"
    const val EXTENSION_IMAGE = ".jpg"



//    const val NODE_PUBLIC_GROUP_CHAT = ""



    const val AUTH = "key=AAAAhT_yccE:APA91bEJ26YPwH4F1a_ZQojK2jSmbTiA_v_-8j5EIDCiyuWFRJZtktMp3jr-5JB4YTcKbkVNdQN3t1U0C3UKp1XpxAZDR3DsW4nAlaTjfGVPE_BpD_sh0N8SH_eWdrcAhRPa6SW9W2Me"


    const val CHAT_STATE_BLOQ = "bloq"
    const val CHAT_STATE_SILENT = "silent"
    const val CHAT_STATE_HIDE = "delete"


    const val MSG_DELIVERY_STATUS = "wVisto"
    const val MSG_RECEIVED_UNREAD = "noVisto"
    const val MSG_SEEN_STATUS = "visto"


    const val MSG_DELIVERED = 1
    const val MSG_RECEIVED = 2
    const val MSG_SEEN = 3



    const val EMPTY = "Empty"
    const val CALLING = "Calling"
    const val RINGING = "Ringing"

    val storage: FirebaseStorage = FirebaseStorage.getInstance()
    val storageReference: StorageReference = storage.reference


}