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

    const val REQUEST_LOCATION = 0
    const val FRAGMENT_ID_CHATLIST = 1
    const val FRAGMENT_ID_CHATGROUPLIST = 2

    const val CAMERA_SELECTED = 22
    const val PHOTO_SELECTED = 33
    const val MIC_SELECTED = 44
    const val PERMISSIONS_EDIT_PROFILE = 11

    const val MAXCHATSIZE = 10000

    const val INFO = 111

    const val MSG = 100
    const val MSG_SENDER_DLT = 101
    const val MSG_RECEIVER_DLT = 102

    const val PHOTO = 200
    const val PHOTO_SENDER_DLT = 201
    const val PHOTO_RECEIVER_DLT = 202

    const val AUDIO = 300
    const val AUDIO_SENDER_DLT = 301
    const val AUDIO_RECEIVER_DLT = 302

    const val PUBLIC_GROUP = 1
    const val PRIVATE_GROUP = 2

    const val CHAT = "Chats"
    const val UNKNOWN = "Unknown"
    const val CHATWITH = "ChatWith"
    const val CHATWITHUNKNOWN = "ChatWithUnknown"
    const val EMPTY = "Empty"
    const val CALLING = "Calling"
    const val RINGING = "Ringing"

    val storage: FirebaseStorage = FirebaseStorage.getInstance()
    val storageReference: StorageReference = storage.reference


}