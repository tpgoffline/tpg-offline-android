package com.dacostafaro.remy.tpgoffline

import android.content.Context
import com.dacostafaro.remy.tpgoffline.db.DBConnection
import com.dacostafaro.remy.tpgoffline.db.database
import org.jetbrains.anko.db.classParser
import org.jetbrains.anko.db.parseList
import org.jetbrains.anko.db.select
import org.jetbrains.anko.doAsync
import java.util.*
import kotlin.math.min

val Context.tpgRoutes: TpgRoutes
    get() = TpgRoutes()

class TpgRoutes {
    private val maxStations = 8600000

    private class EarliestArrival(var time: Int, var line: String)
    private var earliestArrival = mutableListOf<EarliestArrival>()
    private var inConnection = mutableListOf<Int>()
    private val numberOfRoutes = 6
    var breakProcess = false
    var progress: Double = 0.0
    val context: Context? = null

    init {

    }

    private fun loop(arrivalStop: Int, departureTime: Int) {
        assert(context != null) { "Context should'nt be null" }
        var earliest = Int.MAX_VALUE

        val calendar = Calendar.getInstance()
        val day = App.dayNumberToId(calendar.get(Calendar.DAY_OF_WEEK))
        val connections = context!!.database.readableDatabase
            .select("OfflineDepartures", "*")
            .whereArgs("departure_time >= {departure_time} and departure_time < arrival_time and day = {day}", "departure_time" to departureTime, "day" to App.dayNumberToId(day))
            .exec {
                return@exec parseList(classParser<DBConnection>())
            }
        for (connection in connections) {
            var minimumConnectionTime = 120
            if ((earliestArrival[connection.departureStop].line.isEmpty()) ||
                (connection.line == earliestArrival[connection.departureStop].line)) {
                minimumConnectionTime = 0
            }
            if (connection.departureStop >= (earliestArrival[connection.departureStop].time + minimumConnectionTime) &&
                    connection.arrivalTime < earliestArrival[connection.arrivalStop].time) {
                earliestArrival[connection.arrivalStop].time = connection.arrivalTime
                earliestArrival[connection.arrivalStop].line = connection.line
                inConnection[connection.arrivalStop] = connection.id

                if (connection.arrivalStop == arrivalStop) {
                    earliest = min(earliest, connection.arrivalTime)
                }

            } else if (connection.arrivalTime > earliest) {
                return
            }
        }
    }

    fun compute(
        departureStop: Int,
        arrivalStop: Int,
        departureTime: Int,
        completion: (List<DBConnection>) -> Unit
    ) {
        breakProcess = false
        progress = 0.0
        doAsync {
            var routes : MutableList<List<DBConnection>> = mutableListOf()
            var lastRoute : List<DBConnection> = listOf()
            var i = 0
            while (routes.count() < 6 && !breakProcess) {
                i += 1
                if (i >= 50) {
                    break
                }
                inConnection = MutableList(maxStations) { Int.MAX_VALUE }
                earliestArrival = MutableList(maxStations) { EarliestArrival(Int.MAX_VALUE, "") }
                earliestArrival[departureStop].time = departureTime

                if (departureStop <= maxStations &&
                    arrivalStop <= maxStations) {
                    loop(arrivalStop, departureTime)
                }

                // Return results
                if (inConnection[arrivalStop] == Int.MAX_VALUE) {
                    routes.add(lastRoute)
                    progress = 1.0
                    completion(lastRoute)
                } else {
                    var route = mutableListOf<DBConnection>()

                    // We have to rebuild the route from the arrival station
                    var lastConnectionIndex = inConnection[arrivalStop]
                    while (lastConnectionIndex != Int.MAX_VALUE) {
                         var connection = context!!.database.readableDatabase
                            .select("OfflineDepartures", "*")
                             .whereArgs("id = {}", "id" to lastConnectionIndex)
                            .exec {
                                return@exec parseList(classParser<DBConnection>())
                            }.first()
                        lastConnectionIndex = inConnection[connection.departureStop]
                        route.reverse()
                        if (lastRoute.isEmpty()) {
                            lastRoute = route
                        } else if (lastRoute.last().arrivalTime == route.last().arrivalTime &&
                                route.first().departureTime > lastRoute.first().departureTime) {
                            lastRoute = route
                        } else if (lastRoute.last().arrivalTime != route.last().arrivalTime) {
                            routes.add(lastRoute)
                            completion(route)
                            lastRoute = route
                            progress += (1/6)
                            i = 0
                        }
                    }

                    progress = 1.0
                    breakProcess = false
                }
            }
        }
    }
}