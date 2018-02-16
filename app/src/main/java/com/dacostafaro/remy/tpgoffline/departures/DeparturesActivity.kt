package com.dacostafaro.remy.tpgoffline.departures

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.dacostafaro.remy.tpgoffline.App
import com.dacostafaro.remy.tpgoffline.R
import com.dacostafaro.remy.tpgoffline.inflate
import com.dacostafaro.remy.tpgoffline.json.*
import com.github.kittinunf.fuel.Fuel
import com.squareup.moshi.*
import kotlinx.android.synthetic.main.activity_departures.*
import kotlinx.android.synthetic.main.activity_departures_cell.view.*
import kotlinx.android.synthetic.main.activity_departures_cell_listcell.view.*
import java.util.*


class DeparturesActivity : AppCompatActivity() {

    private lateinit var stop: Stop
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_departures)

        val stop = App.stops.firstOrNull { it.appId == intent.getIntExtra("stop", 0) }
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

    fun reload() {
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
        val line = departures.map { it.line.code }.distinct().sorted()[position]
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
        view.lineTextView.text = "Line $line"
        view.lineTextView.setTextColor(App.textForLine(line, "FF"))
        view.lineBackground.setBackgroundColor(App.backgroundForLine(line,"FF"))
        val linearLayoutManager = LinearLayoutManager(view.context)
        view.departuresRecyclerView.layoutManager = linearLayoutManager
        val adapter = ListCellDeparturesRecyclerAdapter(expanded, java.util.ArrayList(departures))
        view.departuresRecyclerView.adapter = adapter
        view.showMoreButton.setOnClickListener {
            expanded = !expanded
            view.showMoreButton.text = if (expanded) {
                "Show less"
            } else {
                "Show more"
            }
            val adapter = ListCellDeparturesRecyclerAdapter(expanded, java.util.ArrayList(departures))
            view.departuresRecyclerView.adapter = adapter
        }
        if (departures.size <= 5) (view.showMoreButton.parent as ViewGroup).removeView(view.showMoreButton)
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

    init {
        v.setOnClickListener(this)
    }

    override fun onClick(v: View) {

    }

    fun bindDeparture(departure: Departure) {
        this.departure = departure
        view.destinationTextView.text = departure.line.destination
        view.leftTimeTextView.text = "${departure.leftTime}'"
    }
}