package com.zibete.proyecto1.model

import androidx.annotation.Keep
import com.google.firebase.database.Exclude
import java.io.Serializable
import java.util.Objects

@Keep
data class Users(
    // Idealmente este mismo valor es el key del nodo en Firebase (/Cuentas/<uid>)
    var id: String = "",

    var name: String = "",
    var birthDay: String = "",
    var createdAt: String = "",      // si querés, después lo pasás a Long
    var age: Int = 0,

    var email: String = "",
    var photoUrl: String = "",
    var isOnline: Boolean = false,

    // calculado localmente
    @get:Exclude
    var distanceMeters: Double = 0.0,

    var description: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0
) : Comparable<Users>, Serializable {

    // Nearest first
    override fun compareTo(other: Users): Int =
        distanceMeters.compareTo(other.distanceMeters)

    // Identidad estable: el uid (id)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Users) return false
        return id == other.id
    }

    override fun hashCode(): Int = Objects.hash(id)

//    companion object Keys {
//        const val ID = "id"
//        const val NAME = "name"
//        const val BIRTHDAY = "birthDay"
//        const val CREATED_AT = "createdAt"
//        const val AGE = "age"
//        const val EMAIL = "email"
//        const val PHOTO_URL = "photoUrl"
//        const val IS_ONLINE = "isOnline"
//        const val DESCRIPTION = "description"
//        const val LATITUDE = "latitude"
//        const val LONGITUDE = "longitude"
//    }
}
