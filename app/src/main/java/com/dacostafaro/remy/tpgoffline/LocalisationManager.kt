package com.dacostafaro.remy.tpgoffline

import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import timber.log.Timber
import kotlin.math.pow

class AppLocalisationManager {
    companion object {
        val shared = AppLocalisationManager()
    }

    var interfaces = mutableListOf<LocalisationManagerInterface>()

    fun add(_interface: LocalisationManagerInterface) {
        interfaces.add(_interface)
    }

    fun remove(_interface: LocalisationManagerInterface){
        interfaces.remove(_interface)
    }

    var localisationMananger: android.location.LocationManager? = null
        set(value: android.location.LocationManager?) {
            if (value != null) {
                field = value
                try {
                    localisationMananger?.requestLocationUpdates(android.location.LocationManager.NETWORK_PROVIDER, 0L, 0f, this.locationListener)
                } catch (e: SecurityException) {
                    Timber.tag("Location").w("Can't get location")
                }
            }
        }
    var currentLocation: Location? = null

    val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            currentLocation = location
            nearestStop = nearestStops(1).first()
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    var nearestStop: Stop? = null
        set(value: Stop?) {
            if (field != value) {
                field = value
                interfaces.forEach { it.didNearestStopChanged() }
            }
        }

    fun nearestStops(numberOfStops: Int = 2): List<Stop> {
        if (currentLocation == null) {
            return listOf()
        }
        val stops = App.stops.sortedWith(object : Comparator<Stop> {
            override fun compare(p1: Stop, p2: Stop): Int = when {
                ((currentLocation!!.latitude - p1.latitude).pow(2) + (currentLocation!!.longitude - p1.longitude).pow(2)) > ((currentLocation!!.latitude - p2.latitude).pow(
                    2
                ) + (currentLocation!!.longitude - p2.longitude).pow(2)) -> 1
                ((currentLocation!!.latitude - p1.latitude).pow(2) + (currentLocation!!.longitude - p1.longitude).pow(2)) == ((currentLocation!!.latitude - p2.latitude).pow(
                    2
                ) + (currentLocation!!.longitude - p2.longitude).pow(2)) -> 0
                else -> -1
            }
        })
        return stops.take(numberOfStops)
    }
}

interface LocalisationManagerInterface {
    fun didNearestStopChanged()
}