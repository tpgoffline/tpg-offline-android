package com.dacostafaro.remy.tpgoffline.departures

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.dacostafaro.remy.tpgoffline.App
import com.dacostafaro.remy.tpgoffline.R
import com.dacostafaro.remy.tpgoffline.TransitionsObjects
import com.dacostafaro.remy.tpgoffline.inflate
import com.dacostafaro.remy.tpgoffline.json.BusRoute
import com.dacostafaro.remy.tpgoffline.json.BusRouteGroup
import com.dacostafaro.remy.tpgoffline.json.Departure
import com.github.kittinunf.fuel.Fuel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.Rfc3339DateJsonAdapter
import kotlinx.android.synthetic.main.activity_bus_route.*
import kotlinx.android.synthetic.main.activity_bus_route_cell.view.*
import java.util.*
import com.google.android.gms.maps.model.BitmapDescriptor




class BusRouteActivity : AppCompatActivity() , OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    private lateinit var departure: Departure
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var mapLoaded: Boolean = false
    private var locations: MutableList<MarkerOptions> = mutableListOf()
    private var disabledPolyline = PolylineOptions().color(Color.GRAY)
    private var passPolyline = PolylineOptions()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bus_route)

        val mapFragment = supportFragmentManager
                .findFragmentById(map.id) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val departure = TransitionsObjects.departure ?: return
        this.departure = departure
        linearLayoutManager = LinearLayoutManager(this)
        busRouteRecyclerView.layoutManager = linearLayoutManager

        swipeRefreshLayout.setOnRefreshListener {
            reload()
        }

        reload()
    }

    private fun reload() {
        Fuel.get("http://prod.ivtr-od.tpg.ch/v1/GetThermometerPhysicalStops.json?key=${App.tpgApiKey}&departureCode=${departure.code}").responseString { _, _, result ->
            result.fold({ responseString ->
                val moshi = Moshi.Builder()
                        .add(KotlinJsonAdapterFactory())
                        .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
                        .build()

                val moshiAdapter = moshi.adapter<BusRouteGroup>(BusRouteGroup::class.java)

                val json = moshiAdapter.fromJson(responseString)!!
                val steps = java.util.ArrayList(json.steps)
                passPolyline = PolylineOptions().color(App.backgroundForLine(json.lineCode, "FF"))

                val adapter = BusRouteRecyclerAdapter(steps, line = json.lineCode)

                busRouteRecyclerView.adapter = adapter

                swipeRefreshLayout.isRefreshing = false
                linearLayoutManager.scrollToPosition(steps.filter { it.arrivalTime == "" }.size)

                for (step in steps) {
                    val coordinates = LatLng(step.physicalStop.coordinates.latitude, step.physicalStop.coordinates.longitude)
                    val alpha = when {
                        step.arrivalTime == "" -> 0.9f
                        else -> 1.0f
                    }
                    val icon = when {
                        step.arrivalTime != "" -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                        else -> BitmapDescriptorFactory.defaultMarker(0.0f)
                    }
                    locations.add(MarkerOptions().position(coordinates).alpha(alpha).icon(icon))
                    when {
                        step.arrivalTime == "" -> disabledPolyline.add(coordinates)
                        else -> passPolyline.add(coordinates)
                    }
                }
                disabledPolyline.add(locations.filter { it.alpha != 0.9f }[0].position)
                if (mapLoaded) {
                    pinpointLocations()
                }
            }, {
                swipeRefreshLayout.isRefreshing = false
            })
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mapLoaded = true
        if (locations.isNotEmpty()) {
            pinpointLocations()
        }
    }

    private fun pinpointLocations() {
        for (location in locations) {
            mMap.addMarker(location)
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locations.filter { it.alpha != 0.9f }[0].position, 15f))
        mMap.addPolyline(disabledPolyline)
        mMap.addPolyline(passPolyline)
    }
}

class BusRouteRecyclerAdapter(private val steps: ArrayList<BusRoute>, private val line: String) : RecyclerView.Adapter<BusRouteHolder>() {
    override fun onBindViewHolder(holder: BusRouteHolder, position: Int) {
        holder.bindStep(step = steps[position], first = position == 0, last = position == steps.size - 1, line = line)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusRouteHolder {
        val inflatedView = parent.inflate(R.layout.activity_bus_route_cell, false)
        return BusRouteHolder(inflatedView)
    }

    override fun getItemCount() = steps.size
}

class BusRouteHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
    private var view: View = v
    private var step: BusRoute? = null

    init {
        v.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val context = view.context
        val intent = Intent(context, DeparturesActivity::class.java)
        intent.putExtra("stop", this.step?.stop?.code ?: "31DC")
        context.startActivity(intent)
    }

    fun bindStep(step: BusRoute, first: Boolean, last: Boolean, line: String) {
        this.step = step
        view.trackView.isStart = first
        view.trackView.isEnd = last
        view.stopTitleTextView.text = step.stop.name
        when {
            step.arrivalTime == "" -> {
                view.arrivalIcon.visibility = View.GONE
                view.leftTimeTextView.visibility = View.GONE
                view.trackView.color = Color.GRAY
                view.stopTitleTextView.setTextColor(Color.GRAY)
            }
            step.arrivalTime?.toInt() ?: -1 == 0 -> {
                view.arrivalIcon.visibility = View.VISIBLE
                view.leftTimeTextView.visibility = View.GONE
                view.trackView.color = App.backgroundForLine(line = line, alpha = "FF")
                view.stopTitleTextView.setTextColor(App.backgroundForLine(line = line, alpha = "FF"))
            }
            else -> {
                val leftTimeString = view.context.getString(R.string.leftTime, step.arrivalTime)
                view.leftTimeTextView.text = leftTimeString
                view.arrivalIcon.visibility = View.GONE
                view.leftTimeTextView.visibility = View.VISIBLE
                view.trackView.color = App.backgroundForLine(line = line, alpha = "FF")
                view.stopTitleTextView.setTextColor(App.backgroundForLine(line = line, alpha = "FF"))
            }
        }
    }
}