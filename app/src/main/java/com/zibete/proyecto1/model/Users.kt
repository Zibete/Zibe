package com.zibete.proyecto1.model

import androidx.annotation.Keep
import com.google.firebase.database.Exclude
import com.google.firebase.database.PropertyName
import java.io.Serializable
import java.util.Objects

@Keep
data class Users(
    var id: String = "",
    var name: String = "",
    var birthDate: String = "", // yyyy-MM-dd
    var createdAt: Long = 0L,  // epoch millis
    var age: Int = 0,
    var email: String = "",
    var photoUrl: String = "",
    @get:PropertyName("isOnline") @set:PropertyName("isOnline")
    var online: Boolean = false,
    @get:Exclude
    var distanceMeters: Double = 0.0, // calculado localmente
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

}
