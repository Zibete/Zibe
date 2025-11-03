package com.zibete.proyecto1.model
import java.io.Serializable
import java.util.Objects

data class Users(
    var id: String = "",
    var name: String? = null,
    var birthDay: String? = null,
    var date: String? = null,
    var age: Int? = null,
    var mail: String? = null,
    var profilePhoto: String? = null,
    var status: Boolean = false,
    var token: String? = null,
    var distance: Double = 0.0,
    var description: String? = null,
    var latitude: Double = 0.0,     // no-null con default
    var longitude: Double = 0.0
) : Comparable<Users>, Serializable {

        override fun compareTo(other: Users): Int {
            if (this.distance < other.distance) return 1
            else if (other.distance < this.distance) return -1
            return 0
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val user = other as Users
            return name == user.name &&
                    id == user.id &&
                    token == user.token &&
                    mail == user.mail
        }

        override fun hashCode(): Int {
            return Objects.hash(id, name, mail)
        }
}