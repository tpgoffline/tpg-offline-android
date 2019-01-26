package com.dacostafaro.remy.tpgoffline

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dacostafaro.remy.tpgoffline.json.BusRoute
import com.dacostafaro.remy.tpgoffline.json.BusRouteGroup
import com.dacostafaro.remy.tpgoffline.json.Departure
import com.github.kittinunf.fuel.Fuel
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.android.synthetic.main.fragment_bus_route.*
import kotlinx.android.synthetic.main.fragment_bus_route_cell.view.*
import java.util.*


class BusRouteFragment : Fragment() {

    private lateinit var departure: Departure
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bus_route, container, false)
    }

    override fun onStart() {
        super.onStart()
        val departure = TransitionsObjects.departure ?: return
        this.departure = departure
        linearLayoutManager = LinearLayoutManager(this.context)
        busRouteRecyclerView.layoutManager = linearLayoutManager

        reload()
    }

    private fun reload() {
        Fuel.get("https://prod.ivtr-od.tpg.ch/v1/GetThermometerPhysicalStops.json?key=${App.tpgApiKey}&departureCode=${departure.code}").responseString { _, _, result ->
            result.fold({ responseString ->
                val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
                    .build()

                val moshiAdapter = moshi.adapter<BusRouteGroup>(BusRouteGroup::class.java)

                val json = moshiAdapter.fromJson(responseString)!!
                val steps = ArrayList(json.steps)

                val adapter = BusRouteRecyclerAdapter(steps, line = json.lineCode)

                busRouteRecyclerView.adapter = adapter

                linearLayoutManager.scrollToPosition(steps.filter { it.arrivalTime == "" }.size)
            }, {
            })
        }
    }
}

class BusRouteRecyclerAdapter(private val steps: ArrayList<BusRoute>, private val line: String) : RecyclerView.Adapter<BusRouteHolder>() {
    override fun onBindViewHolder(holder: BusRouteHolder, position: Int) {
        holder.bindStep(step = steps[position], first = position == 0, last = position == steps.size - 1, line = line)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusRouteHolder {
        val inflatedView = parent.inflate(R.layout.fragment_bus_route_cell, false)
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
        val action= BusRouteFragmentDirections.actionBusRouteToDepartureFragment(this.step?.stop?.code ?: "31DC")
        v.findNavController().navigate(action)
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
