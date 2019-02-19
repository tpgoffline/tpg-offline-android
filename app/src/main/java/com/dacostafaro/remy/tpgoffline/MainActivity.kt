package com.dacostafaro.remy.tpgoffline

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.LineManager
import com.mapbox.mapboxsdk.plugins.annotation.LineOptions
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    var symbolManager: SymbolManager? = null
    var lineManager: LineManager? = null
    private var locationMananger: LocationManager? = null
    var currentLocation: Location? = null
    public var executeOnLocationChange: (() -> Unit)? = null

    val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            currentLocation = location
            executeOnLocationChange?.invoke()
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    public fun nearestStops(numberOfStops: Int = 2): List<Stop> {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(
            this,
            "pk.eyJ1IjoicmVteWRjZiIsImEiOiJjamtubzJrcXYxYm9qM3JtZ3FibHA5MmZxIn0.zObvUnRHHWelQAPLFQpduw"
        );
        setContentView(R.layout.activity_main)
        mapView.onCreate(savedInstanceState)

        NavigationUI.setupActionBarWithNavController(this, NavHostFragment.findNavController(nav_host_fragment))

        locationMananger = getSystemService(LOCATION_SERVICE) as LocationManager?
        try {
            locationMananger?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, this.locationListener)
        } catch (e: SecurityException) {
            Timber.tag("Location").w("Can't get location")
        }
    }

    public override fun onStart() {
        super.onStart()
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

        var linesColorsMoshiAdapter = moshi.adapter<Array<LineColor>>(Array<LineColor>::class.java)
        App.linesColors = ArrayList(linesColorsMoshiAdapter.fromJson(jsonString)!!.toList().sortedBy { it.line })

        mapView.onStart()

        mapView.getMapAsync { mapboxMap ->
            mapboxMap.setStyle(Style.Builder().fromUrl("mapbox://styles/remydcf/cjkl5fqlw616i2rmwvlv75j98")) { style ->
                symbolManager = SymbolManager(mapView, mapboxMap, style)
                symbolManager!!.iconAllowOverlap = true
                symbolManager!!.textAllowOverlap = true

                lineManager = LineManager(mapView, mapboxMap, style)
            }
        }
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

        for (point in points) {
            val icon = if (point.busIcon) {
                "bus-15"
            } else {
                "marker-15"
            }
            val options = SymbolOptions()
                .withLatLng(point.location)
                .withIconImage(icon)
                .withIconColor(lineColor)
            symbolManager!!.create(options)
        }

        val lineOptions = LineOptions()
            .withLineWidth(2F)
            .withLatLngs(points.map { it.location })
            .withLineColor(lineColor)
        lineManager!!.create(lineOptions)
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