package com.dacostafaro.remy.tpgoffline.departures

import android.annotation.SuppressLint
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.dacostafaro.remy.tpgoffline.*
import com.dacostafaro.remy.tpgoffline.json.*
import com.github.kittinunf.fuel.Fuel
import com.squareup.moshi.*
import kotlinx.android.synthetic.main.activity_departures.*
import kotlinx.android.synthetic.main.activity_departures_cell.view.*
import kotlinx.android.synthetic.main.activity_departures_cell_listcell.view.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


class DeparturesActivity : AppCompatActivity() {

    private lateinit var stop: Stop
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_departures)

        val stop = App.stops.firstOrNull { it.code == intent.getStringExtra("stop") }
        requireNotNull(stop) { "no stopCode provided in Intent extras" }
        this.stop = stop!!

        linearLayoutManager = LinearLayoutManager(this)
        departuresRecyclerView.layoutManager = linearLayoutManager

        toolbar.title = stop.name

        swipeRefreshLayout.setOnRefreshListener {
            reload()
        }

        reload()
    }

    private fun reload() {
        Fuel.get("http://tpgoffline-apns.alwaysdata.net/api/departures/${stop.code}").responseString { _, _, result ->
            result.fold({ responseString ->
                val moshi = Moshi.Builder()
                        .add(DepartureAdapter())
                        .add(KotlinJsonAdapterFactory())
                        .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
                        .build()

                val moshiAdapter = moshi.adapter<DeparturesGroup>(DeparturesGroup::class.java)

                val departures = java.util.ArrayList(moshiAdapter.fromJson(responseString)!!.departures)

                val adapter = DeparturesRecyclerAdapter(departures)

                departuresRecyclerView.adapter = adapter

                swipeRefreshLayout.isRefreshing = false
            }, {
                swipeRefreshLayout.isRefreshing = false
            })
        }
    }
}

class DeparturesRecyclerAdapter(private val departures: ArrayList<Departure>) : RecyclerView.Adapter<DepartureHolder>() {
    override fun onBindViewHolder(holder: DepartureHolder, position: Int) {
        val line = departures.map { it.line.code }.distinct().sortedWithInt()[position]
        val departures = departures.filter { it.line.code == line }
        holder.bindDepartures(line = line, departures = departures)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DepartureHolder {
        val inflatedView = parent.inflate(R.layout.activity_departures_cell, false)
        return DepartureHolder(inflatedView)
    }

    override fun getItemCount() = departures.map { it.line.code }.distinct().sorted().size
}

class DepartureHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
    private var view: View = v
    private var departures: List<Departure>? = null
    private var expanded: Boolean = false

    init {
        v.setOnClickListener(this)
    }

    override fun onClick(v: View) {

    }

    fun bindDepartures(line: String, departures: List<Departure>) {
        this.departures = departures
        view.lineTextView.text = view.context.getString(R.string.line_title, line)
        if (line.take(1).toUpperCase() == "N" && line.length == 2) {
            view.lineTextView.text = "Line Noctambus ${line.substring(1,2)}"
        }
        view.lineTextView.setTextColor(App.textForLine(line, "FF"))
        view.lineBackground.setBackgroundColor(App.backgroundForLine(line,"FF"))
        val linearLayoutManager = LinearLayoutManager(view.context)
        view.departuresRecyclerView.layoutManager = linearLayoutManager
        var adapter = ListCellDeparturesRecyclerAdapter(expanded, java.util.ArrayList(departures))
        view.departuresRecyclerView.adapter = adapter
        if (departures.size <= 5) view.showMoreButton.visibility = View.GONE
        else {
            view.showMoreButton.visibility = View.VISIBLE
            view.showMoreButton.setOnClickListener {
                expanded = !expanded
                view.showMoreButton.text = if (expanded) {
                    "Show less"
                } else {
                    "Show more"
                }
                adapter = ListCellDeparturesRecyclerAdapter(expanded, java.util.ArrayList(departures))
                view.departuresRecyclerView.adapter = adapter
            }
        }
    }
}

class ListCellDeparturesRecyclerAdapter(private val expanded: Boolean, private val departures: ArrayList<Departure>) : RecyclerView.Adapter<LictCellDepartureHolder>() {
    override fun onBindViewHolder(holder: LictCellDepartureHolder, position: Int) {
        holder.bindDeparture(departures[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LictCellDepartureHolder {
        val inflatedView = parent.inflate(R.layout.activity_departures_cell_listcell, false)
        return LictCellDepartureHolder(inflatedView)
    }

    override fun getItemCount() = if (!expanded && departures.size > 5) {
        5
    } else {
        departures.size
    }
}

class LictCellDepartureHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
    private var view: View = v
    private var departure: Departure? = null
    private var isClickable = true

    init {
        v.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        if (departure?.code ?: -1 != -1) {
            TransitionsObjects.departure = departure
            val context = view.context
            val intent = Intent(context, BusRouteActivity::class.java)
            context.startActivity(intent)
        }
    }

    fun bindDeparture(departure: Departure) {
        this.departure = departure
        view.destinationTextView.text = departure.line.destination
        when {
            departure.leftTime == "0" -> {
                view.leftTimeTextView.text = ""
                view.leftTimeTextView.visibility = View.GONE
                view.arrivalIcon.visibility = View.VISIBLE
                view.noMoreIcon.visibility = View.GONE
                view.chevronIcon.visibility = View.VISIBLE
                isClickable = true
            }
            departure.leftTime == "&gt;1h" -> {
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ").parse(departure.timestamp)
                view.leftTimeTextView.text = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(date)
                view.leftTimeTextView.visibility = View.VISIBLE
                view.noMoreIcon.visibility = View.GONE
                view.arrivalIcon.visibility = View.GONE
                view.chevronIcon.visibility = View.VISIBLE
                isClickable = true
            }
            departure.leftTime == "no more" -> {
                view.noMoreIcon.visibility = View.VISIBLE
                view.leftTimeTextView.visibility = View.GONE
                view.arrivalIcon.visibility = View.GONE
                view.chevronIcon.visibility = View.GONE
                isClickable = false
            }
            else -> {
                view.leftTimeTextView.visibility = View.VISIBLE
                view.arrivalIcon.visibility = View.GONE
                view.noMoreIcon.visibility = View.GONE
                view.chevronIcon.visibility = View.VISIBLE
                view.leftTimeTextView.text =  view.context.getString(R.string.leftTime, departure.leftTime)
                isClickable = true
            }
        }
        view.wifiIcon.visibility = if (departure.wifi) {
             View.VISIBLE
        } else {
            View.GONE
        }
        view.notPMRIcon.visibility = if (departure.reducedMobilityAccessibility == Departure.ReducedMobilityAccessibility.Inaccessible) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}