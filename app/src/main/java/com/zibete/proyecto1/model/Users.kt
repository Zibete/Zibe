package com.zibete.proyecto1.model
import androidx.annotation.Keep
import com.google.firebase.database.Exclude
import com.google.firebase.database.PropertyName
import java.io.Serializable
import java.util.Objects

@Keep
data class Users(
    var id: String = "",

    @get:PropertyName("nombre")
    @set:PropertyName("nombre")
    var name: String = "",

    var birthDay: String = "",
    var date: String = "",
    var age: Int = 0,
    var mail: String = "",

    @get:PropertyName("foto")
    @set:PropertyName("foto")
    var profilePhoto: String = "",

    @get:PropertyName("estado")
    @set:PropertyName("estado")
    var state: Boolean = false,

    var token: String = "",

    // NO viene de Firebase, la calculamos nosotros
    @get:Exclude
    var distanceMeters: Double = 0.0,

    @get:PropertyName("descripcion")
    @set:PropertyName("descripcion")
    var description: String = "",

    @get:PropertyName("latitud")
    @set:PropertyName("latitud")
    var latitude: Double = 0.0,

    @get:PropertyName("longitud")
    @set:PropertyName("longitud")
    var longitude: Double = 0.0
)
 : Comparable<Users>, Serializable {

        override fun compareTo(other: Users): Int {
            if (this.distanceMeters < other.distanceMeters) return 1
            else if (other.distanceMeters < this.distanceMeters) return -1
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