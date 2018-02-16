package com.dacostafaro.remy.tpgoffline.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.ToJson

/**
 * Created by remy on 11/02/2018.
 */

data class DeparturesGroup(
        val departures: List<Departure>
)

class DepartureAdapter {
    @FromJson
    fun departureFromJson(departureJson: DepartureJson): Departure {
        val wifi = (departureJson.vehiculeNo in 781..790) || (departureJson.vehiculeNo in 1601..1663)
        val reliability: Departure.Reliability = if (departureJson.reliability == "F") {
            Departure.Reliability.Reliable
        } else {
            Departure.Reliability.Theoretical
        }
        val reducedMobilityAccessible = if (departureJson.characteristics == "PMR") {
            Departure.ReducedMobilityAccessibility.Accessible
        } else {
            Departure.ReducedMobilityAccessibility.Inaccessible
        }
        return Departure(
                line = departureJson.line,
                code = departureJson.code,
                leftTime = departureJson.leftTime,
                timestamp = departureJson.timestamp,
                wifi = wifi,
                reliability = reliability,
                reducedMobilityAccessibility = reducedMobilityAccessible,
                platform = departureJson.platform)
    }

    @ToJson
    fun departureToJson(departure: Departure): DepartureJson {
        val reliability: String = if (departure.reliability == Departure.Reliability.Reliable) {
            "F"
        } else {
            "T"
        }
        val characteristics = if (departure.reducedMobilityAccessibility == Departure.ReducedMobilityAccessibility.Accessible) {
            "PMR"
        } else {
            ""
        }
        return DepartureJson(
                line = departure.line,
                code = departure.code,
                leftTime = departure.leftTime,
                timestamp = departure.timestamp,
                vehiculeNo = -1,
                reliability = reliability,
                characteristics = characteristics,
                platform = departure.platform)
    }
}

class OfflineDepartureAdapter {
    @FromJson
    fun departureFromJson(departureJson: DeparturesOfflineJson): Departure {
        return Departure(
                line = DepartureLine(
                        code = departureJson.line,
                        destination = departureJson.destination,
                        destinationCode = ""),
                code = -1,
                leftTime = departureJson.leftTime,
                timestamp = departureJson.timestamp,
                wifi = false,
                reliability = Departure.Reliability.Reliable,
                reducedMobilityAccessibility = Departure.ReducedMobilityAccessibility.Accessible,
                platform = null)
    }

    @ToJson
    fun departureToJson(departure: Departure): DepartureJson {
        val reliability: String = if (departure.reliability == Departure.Reliability.Reliable) {
            "F"
        } else {
            "T"
        }
        val characteristics = if (departure.reducedMobilityAccessibility == Departure.ReducedMobilityAccessibility.Accessible) {
            "PMR"
        } else {
            ""
        }
        return DepartureJson(
                line = departure.line,
                code = departure.code,
                leftTime = departure.leftTime,
                timestamp = departure.timestamp,
                vehiculeNo = -1,
                reliability = reliability,
                characteristics = characteristics,
                platform = departure.platform)
    }
}

data class Departure(
        var line: DepartureLine,
        var code: Int,
        var leftTime: String,
        var timestamp: String,
        var wifi: Boolean,
        var reliability: Reliability,
        var reducedMobilityAccessibility: ReducedMobilityAccessibility,
        var platform: String?) {
    enum class Reliability { Reliable, Theoretical }
    enum class ReducedMobilityAccessibility { Accessible, Inaccessible }
}

data class DepartureJson(
        val line: DepartureLine,
        @Json(name = "departureCode") val code: Int = -1,
        @Json(name = "waitingTime") val leftTime: String = "",
        val timestamp: String = "",
        val vehiculeNo: Int = -1,
        val reliability: String = "F",
        val characteristics: String = "PMR",
        val platform: String?)

data class DepartureLine(
        @Json(name = "lineCode") var code: String,
        @Json(name = "destinationName") var destination: String,
        var destinationCode: String
)

data class DeparturesOfflineJson(
        @Json(name = "ligne") val line: String,
        val destination: String,
        val leftTime: String = "",
        val timestamp: String = "")
