package com.dacostafaro.remy.tpgoffline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dacostafaro.remy.tpgoffline.json.Departure
import com.dacostafaro.remy.tpgoffline.json.DepartureAdapter
import com.dacostafaro.remy.tpgoffline.json.DeparturesGroup
import com.github.kittinunf.fuel.Fuel
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.android.synthetic.main.fragment_departure.*
import kotlinx.android.synthetic.main.fragment_departure_cell.view.*
import kotlinx.android.synthetic.main.fragment_departure_cell_listcell.view.*
import java.text.SimpleDateFormat
import java.util.*

class DepartureFragment : Fragment() {

    private lateinit var stop: Stop
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_departure, container, false)
    }

    override fun onStart() {
        super.onStart()

        activity!!.dismissKeyboard()

        val stop = App.stops.firstOrNull { it.code == DepartureFragmentArgs.fromBundle(this.arguments!!).stopId }
        requireNotNull(stop) { "no stopCode provided in Intent extras" }
        this.stop = stop

        stop_title.text = stop.title
        stop_subtitle.text = stop.subTitle

        if (stop.subTitle.isEmpty()) {
            stop_subtitle.visibility = View.GONE
        }

        linearLayoutManager = LinearLayoutManager(this.context)
        departuresRecyclerView.layoutManager = linearLayoutManager

        //swipeRefreshLayout.setOnRefreshListener {
        //    reload()
        //}

        reload()
    }

    fun reload() {
        Fuel.get("https://api.tpgoffline.com/departures/${stop.code}?key=d95be980-0830-11e5-a039-0002a5d5c51b").responseString { _, _, result ->
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

                //swipeRefreshLayout.isRefreshing = false
            }, {
                //swipeRefreshLayout.isRefreshing = false
            })
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            DepartureFragment().apply {
                arguments = Bundle().apply {
                }
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
        val inflatedView = parent.inflate(R.layout.fragment_departure_cell, false)
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

    override fun onClick(v: View?) {

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
        val inflatedView = parent.inflate(R.layout.fragment_departure_cell_listcell, false)
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
            val action = DepartureFragmentDirections.actionDepartureFragmentToBusRoute()
            v.findNavController().navigate(action)
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
                view.leftTimeTextView.text = view.context.getString(R.string.leftTime, departure.leftTime)
                isClickable = true
            }
        }
        view.wifiIcon.visibility = if (departure.wifi) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}