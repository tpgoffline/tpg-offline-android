package com.dacostafaro.remy.tpgoffline

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.Navigation
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
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home_stop_card.view.*
import kotlinx.android.synthetic.main.fragment_home_stop_card_listcell.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.fixedRateTimer
import android.graphics.drawable.GradientDrawable

class HomeFragment : Fragment(), LocalisationManagerInterface {
    override fun didNearestStopChanged() {
        val adapter = HomeRecyclerAdapter(activity!!, AppLocalisationManager.shared.nearestStops(1))
        recyclerView.adapter = adapter
    }

    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onStart() {
        super.onStart()

        AppLocalisationManager.shared.add(this)

        search_stops_button.text = getString(R.string.going_somewhere)
        search_stops_button.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.action_home_to_stopsFragment, null))

        linearLayoutManager = LinearLayoutManager(this.context)
        recyclerView.layoutManager = linearLayoutManager
        val adapter = HomeRecyclerAdapter(activity!!, AppLocalisationManager.shared.nearestStops(1))
        recyclerView.adapter = adapter
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            HomeFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}

enum class HomeCardType {
    LOCATION, FAVORITE, DISRUPTIONS, LINES
}

class HomeRecyclerAdapter(val activity: FragmentActivity, val nearestStops: List<Stop>) : RecyclerView.Adapter<HomeCardHolder>() {
    override fun onBindViewHolder(holder: HomeCardHolder, position: Int) {
        var max = 0
        max += nearestStops.count()
        if (position < max) {
            holder.bindStop(nearestStops[position].appId, HomeCardType.LOCATION)
            return
        }
        max += (activity as MainActivity).favoritesStops.count()
        if (position < max) {
            val index = position - (nearestStops.count())
            holder.bindStop((activity as MainActivity).favoritesStops[index], HomeCardType.FAVORITE)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeCardHolder {
        val inflatedView = parent.inflate(R.layout.fragment_home_stop_card, false)
        return HomeCardHolder(inflatedView)
    }

    override fun getItemCount() = nearestStops.count() + (activity as MainActivity).favoritesStops.count()
}

class HomeCardHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
    private var view: View = v
    var stopCode = ""
    var timer: Timer? = null

    private lateinit var linearLayoutManager: LinearLayoutManager

    init {
        v.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        // TODO: Push DepartureFragment with appId
        timer?.cancel()
        val action = HomeFragmentDirections.actionHomeToDepartureFragment(stopCode)
        view.findNavController().navigate(action)
    }

    fun reload() {
        view.loadingProgressView.visibility = View.VISIBLE
        Fuel.get("https://api.tpgoffline.com/departures/${this.stopCode}?key=d95be980-0830-11e5-a039-0002a5d5c51b").responseString { _, _, result ->
            result.fold({ responseString ->
                val moshi = Moshi.Builder()
                    .add(DepartureAdapter())
                    .add(KotlinJsonAdapterFactory())
                    .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
                    .build()

                val moshiAdapter = moshi.adapter<DeparturesGroup>(DeparturesGroup::class.java)

                val departures = java.util.ArrayList(moshiAdapter.fromJson(responseString)!!.departures)

                val adapter = StopListCellHomeRecyclerAdapter(departures, stopCode)

                view.departuresRecyclerView.adapter = adapter
                view.loadingProgressView.visibility = View.GONE
            }, {
                view.loadingProgressView.visibility = View.GONE
            })
        }
    }

    fun bindStop(appId: Int, cardType: HomeCardType) {
        val stop = App.stops.firstOrNull { it.appId == appId }
        requireNotNull(stop) { "appId is invalid: ${appId}" }
        view.cardTitleView.text = stop.name
        view.imageView.setImageResource(when (cardType) {
            HomeCardType.LOCATION -> R.drawable.ic_near_me_black_24dp
            else -> R.drawable.ic_star_black_24dp
        })
        linearLayoutManager = object:LinearLayoutManager(view.context) {
            override fun canScrollVertically(): Boolean {
                return false
            }
        }
        view.departuresRecyclerView.layoutManager = linearLayoutManager
        this.stopCode = stop.code
        timer = fixedRateTimer(name = "reload-timer", initialDelay = 0, period = 30000) {
            reload()
        }
    }
}

class StopListCellHomeRecyclerAdapter(private val departures: ArrayList<Departure>, val stopCode: String) : RecyclerView.Adapter<StopListCellHomeCardHolder>() {
    override fun onBindViewHolder(holder: StopListCellHomeCardHolder, position: Int) {
        holder.bindDeparture(departures[position], stopCode)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopListCellHomeCardHolder {
        val inflatedView = parent.inflate(R.layout.fragment_home_stop_card_listcell, false)
        return StopListCellHomeCardHolder(inflatedView)
    }

    override fun getItemCount() = if (departures.size > 3) {
        3
    } else {
        departures.size
    }
}

class StopListCellHomeCardHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
    private var view: View = v
    private var departure: Departure? = null
    var stopCode = ""

    init {
        v.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val action = HomeFragmentDirections.actionHomeToDepartureFragment(stopCode)
        view.findNavController().navigate(action)
    }

    @SuppressLint("SimpleDateFormat") // Linter should'nt give a warning, since the pattern is not designed for the user, but for the API.
    fun bindDeparture(departure: Departure, stopCode: String) {
        this.departure = departure
        this.stopCode = stopCode
        view.destinationTextView.text = departure.line.destination
        view.lineTextView.text = departure.line.code
        view.lineTextView.setTextColor(App.textForLine(departure.line.code, alpha = "FF"))
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.setColor(App.backgroundForLine(departure.line.code, "FF"))
        shape.cornerRadius = 75f
        view.lineTextView.background = shape

        when {
            departure.leftTime == "0" -> {
                view.leftTimeTextView.text = ""
                view.leftTimeTextView.visibility = View.GONE
                view.arrivalIcon.visibility = View.VISIBLE
                view.noMoreIcon.visibility = View.GONE
            }
            departure.leftTime == "&gt;1h" -> {
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ").parse(departure.timestamp)
                view.leftTimeTextView.text = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(date)
                view.leftTimeTextView.visibility = View.VISIBLE
                view.noMoreIcon.visibility = View.GONE
                view.arrivalIcon.visibility = View.GONE
            }
            departure.leftTime == "no more" -> {
                view.noMoreIcon.visibility = View.VISIBLE
                view.leftTimeTextView.visibility = View.GONE
                view.arrivalIcon.visibility = View.GONE
            }
            else -> {
                view.leftTimeTextView.visibility = View.VISIBLE
                view.arrivalIcon.visibility = View.GONE
                view.noMoreIcon.visibility = View.GONE
                view.leftTimeTextView.text = view.context.getString(R.string.leftTime, departure.leftTime)
            }
        }
        view.wifiIcon.visibility = if (departure.wifi) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}