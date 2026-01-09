package com.zibete.proyecto1.data

import android.location.Location
import com.google.firebase.auth.FirebaseUser
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.core.constants.Constants.AccountsKeys.LATITUDE
import com.zibete.proyecto1.core.constants.Constants.AccountsKeys.LONGITUDE
import com.zibete.proyecto1.core.constants.USER_PROVIDER_ERR_EXCEPTION
import com.zibete.proyecto1.data.auth.AuthSessionProvider
import kotlinx.coroutines.tasks.await
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Singleton
class LocationRepository @Inject constructor(
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val authSessionProvider: AuthSessionProvider
) {

    val firebaseUser: FirebaseUser
        get() = checkNotNull(authSessionProvider.currentUser) {
            USER_PROVIDER_ERR_EXCEPTION
        }

    val myUid: String
        get() = firebaseUser.uid


    // ============================================================
    // LOCATION
    // ============================================================

    var latitude: Double = 0.0
    var longitude: Double = 0.0

//    val latitude: Double get() = latitude
//    val longitude: Double get() = longitude

    suspend fun updateLocation(location: Location) {
        updateUserFields(
            mapOf(
                LATITUDE to location.latitude,
                LONGITUDE to location.longitude
            )
        )
    }
    private fun accountRef(uid: String = myUid) =
        firebaseRefsContainer.refAccounts.child(uid)

    suspend fun updateUserFields(fields: Map<String, Any?>) {
        val clean = fields.filterValues { it != null }
        if (clean.isEmpty()) return
        accountRef().updateChildren(clean).await()
    }
    private suspend fun getLocation(uid : String = myUid): Pair<Double, Double> {
        val snapshot = firebaseRefsContainer.refAccounts.child(uid).get().await()
        val user = snapshot.getValue(Users::class.java)
            ?: throw Exception("User not found")
        return user.latitude to user.longitude
    }

    suspend fun getDistanceToUser(userId: String): String {
        val (myLat, myLng) = getLocation()
        val (otherLat, otherLng) = getLocation(userId)
        val distance = getDistanceMeters(myLat, myLng, otherLat, otherLng)
        return formatDistance(distance)
    }

    fun getDistanceMeters(lat1: Double, lon1: Double,lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (earthRadius * c * 1000) // metros
    }

    fun formatDistance(distanceMeters: Double): String {
        return when {
            distanceMeters < 50 -> "Aquí"
            distanceMeters < 1000 -> {
                val metros = distanceMeters.roundToInt()
                "A $metros metros"
            }
            distanceMeters < 10000 -> {
                val km = distanceMeters / 1000
                val rounded = BigDecimal(km).setScale(1, RoundingMode.HALF_UP)
                "A $rounded km"
            }
            else -> {
                val km = distanceMeters / 1000
                val rounded = BigDecimal(km).setScale(0, RoundingMode.HALF_UP)
                "A $rounded km"
            }
        }
    }
}
