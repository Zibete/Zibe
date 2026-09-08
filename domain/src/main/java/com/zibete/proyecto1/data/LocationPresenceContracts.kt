package com.zibete.proyecto1.data

import com.zibete.proyecto1.core.utils.ZibeResult
import kotlinx.coroutines.flow.Flow

interface LocationRepositoryProvider {
    val latitude: Double
    val longitude: Double
    suspend fun getDistanceToUser(otherUid: String): ZibeResult<String>
    fun getDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double
    fun formatDistance(distanceMeters: Double): String
}

interface LocationRepositoryActions {
    suspend fun updateLocation(latitude: Double, longitude: Double)
}

interface PresenceRepositoryActions {
    suspend fun startPresence()
    fun stopPresence()
}
