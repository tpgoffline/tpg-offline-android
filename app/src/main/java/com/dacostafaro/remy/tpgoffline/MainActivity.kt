package com.dacostafaro.remy.tpgoffline

import android.content.Context
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.dacostafaro.remy.tpgoffline.db.database
import com.github.kittinunf.fuel.Fuel
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.annotations.PolylineOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.db.insert
import org.jetbrains.anko.doAsync
import timber.log.Timber
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(
            this,
            "pk.eyJ1IjoicmVteWRjZiIsImEiOiJjamtubzJrcXYxYm9qM3JtZ3FibHA5MmZxIn0.zObvUnRHHWelQAPLFQpduw"
        );
        setContentView(R.layout.activity_main)
        mapView.onCreate(savedInstanceState)
        //mapView.setStyleUrl(getString(R.string.mapbox_light_theme))
        //mapView.getMapAsync {}

        NavigationUI.setupActionBarWithNavController(this, NavHostFragment.findNavController(nav_host_fragment))
    }

    public override fun onStart() {
        super.onStart()
        mapView.onStart()

        AppLocalisationManager.shared.localisationMananger = getSystemService(LOCATION_SERVICE) as LocationManager

        var jsonString = application.assets.open("stops.json").bufferedReader().use {
            it.readText()
        }
        var moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()


        val stopsMoshiAdapter = moshi.adapter<Array<Stop>>(Array<Stop>::class.java)
        App.stops = ArrayList(stopsMoshiAdapter.fromJson(jsonString)!!.toList().sortedBy { it.name })

        jsonString = application.assets.open("linesColors.json").bufferedReader().use {
            it.readText()
        }
        moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val linesColorsMoshiAdapter = moshi.adapter<Array<LineColor>>(Array<LineColor>::class.java)
        App.linesColors = ArrayList(linesColorsMoshiAdapter.fromJson(jsonString)!!.toList().sortedBy { it.line })

        val sharedPref = this.getSharedPreferences(getString(R.string.shared_preferences), Context.MODE_PRIVATE) ?: return

        /*doAsync {
            val days = listOf("Monday", "Friday", "Saturday", "Sunday")
            for (day in days) {
                Fuel.get("https://raw.githubusercontent.com/tpgoffline/tpgoffline-data/master/$day.timetables.md5")
                    .responseString { _, _, result ->
                        result.fold({ responseStringMD5 ->
                            if (responseStringMD5 != sharedPref.getString(
                                    getString(R.string.offline_departures_md5) + "_$day",
                                    ""
                                )
                            ) {
                                Fuel.get("https://raw.githubusercontent.com/tpgoffline/tpgoffline-data/master/$day.timetables")
                                    .responseString { _, _, result ->
                                        result.fold({ responseString ->
                                            val departures = responseString.split("\n")
                                            for (departure in departures) {
                                                val departureComponent = departure.split(" ")
                                                applicationContext.database.use {
                                                    insert(
                                                        "OfflineDepartures",
                                                        "departure_stop" to departureComponent[0],
                                                        "arrival_stop" to departureComponent[1],
                                                        "departure_time" to departureComponent[2],
                                                        "arrival_time" to departureComponent[3],
                                                        "line" to departureComponent[4],
                                                        "trip_id" to departureComponent[6],
                                                        "destination_stop" to departureComponent[5],
                                                        "day" to App.dayStringToId(day)
                                                    )
                                                }
                                            }
                                            with(sharedPref.edit()) {
                                                putString(
                                                    getString(R.string.offline_departures_md5) + "_$day",
                                                    responseStringMD5
                                                )
                                                apply()
                                            }
                                        }, {
                                            Timber.tag("Offline departures").w("Can't load ${day} offline departures")
                                        })
                                    }
                            } else {
                                Timber.tag("Offline departures").i("${day} offline departures are up to date")
                            }
                        }, {
                            Timber.tag("Offline departures").w("Can't load ${day} offline departures")
                        })
                    }
            }
        }*/
    }

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    public override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onSupportNavigateUp(): Boolean = NavHostFragment.findNavController(nav_host_fragment).navigateUp()

    fun drawLineOnMap(points: List<Marker>, line: String) {
        val lineColor = "#" + (App.linesColors.firstOrNull { it.line == line }?.background ?: "")

        mapView.getMapAsync {
            it.clear()

            for (point in points) {
                it.addMarker(
                    MarkerOptions()
                        .position(LatLng(point.location))
                        .title(point.title)
                )
            }

            it.addPolyline(
                PolylineOptions()
                    .addAll(points.map { it.location })
                    .color(Color.parseColor(lineColor))
                    .width(2F)
            )
        }
    }

    val favoritesStops: MutableList<Int>
        get() {
            val sharedPref = this.getSharedPreferences(getString(R.string.shared_preferences), Context.MODE_PRIVATE)
                ?: return mutableListOf()
            val favoritesStops = sharedPref.getString(getString(R.string.key_favorite), "")
            return if (favoritesStops.isBlank()) {
                mutableListOf()
            } else {
                favoritesStops.split(",").map { it.toInt() }.toMutableList()
            }
        }
}

data class Marker(
    val location: LatLng,
    val title: String,
    val busIcon: Boolean
)