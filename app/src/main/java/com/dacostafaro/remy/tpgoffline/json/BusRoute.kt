package com.dacostafaro.remy.tpgoffline.json

import com.squareup.moshi.Json

/**
 * Created by remy on 04/03/2018.
 */

data class BusRouteGroup(
        val steps: List<BusRoute>,
        val lineCode: String,
        @Json(name = "destinationName") val destination: String
)

data class BusRoute(
    val stop: Stop,
    val physicalStop: PhysicalStop,
    val timestamp: String,
    val arrivalTime: String? = "",
    //var first: Boolean,
    //var last: Boolean,
    var reliability: Reliability
) {
    data class Stop(
            @Json(name = "stopCode") val code: String,
            @Json(name = "stopName") val name: String//,
            //val connections: Any
    )
    data class PhysicalStop(
            val coordinates: Coordinates
    ) {
        data class Coordinates(
                val latitude: Double,
                val longitude: Double
        )
    }
    enum class Reliability { F, T }
}