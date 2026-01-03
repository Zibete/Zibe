package com.zibete.proyecto1.testing

import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.model.Users

object TestData {
    const val EMAIL = "a@b.com"
    const val PASSWORD = "123"
    const val NAME = "Zibe"
    const val BIRTH = "2000-01-01"
    const val DESC = "desc"
    const val UID = "uid_123"
    const val INSTALL_ID = "install_1"
    const val TOKEN = "token_1"
    const val PHOTO_URL = "photoUrl"
    const val RUNTIME_EXCEPTION = "boom"
    const val CHAT_STATE = NODE_DM
    val USER = Users(
        id = UID,
        name = NAME,
        birthDate = BIRTH,
        description = DESC,
        photoUrl = PHOTO_URL
    )

}