package com.dacostafaro.remy.tpgoffline.disruptions

import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dacostafaro.remy.tpgoffline.App
import com.dacostafaro.remy.tpgoffline.R
import com.github.kittinunf.fuel.Fuel
import com.squareup.moshi.Json
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import kotlinx.android.synthetic.main.fragment_disruptions.*
import com.squareup.moshi.Rfc3339DateJsonAdapter
import java.util.Date
import java.util.*
import android.app.Fragment
import android.util.Log
import com.dacostafaro.remy.tpgoffline.inflate
import com.dacostafaro.remy.tpgoffline.sortedWithInt
import kotlinx.android.synthetic.main.fragment_disruptions_cell.view.*

class DisruptionsFragment : Fragment() {
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: RecyclerAdapter

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater!!.inflate(R.layout.fragment_disruptions, container, false)
    }

    override fun onStart() {
        super.onStart()

        linearLayoutManager = LinearLayoutManager(this.activity)
        recycler_view.layoutManager = linearLayoutManager

        adapter = RecyclerAdapter(ArrayList())

        recycler_view.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener {
            refresh()
        }

        refresh()
    }


    private fun refresh() {
        Log.d("Disruptions", "Request")

        var url = if (false) {
            "https://asmartcode.com/disruptions.json"
        } else {
            "http://prod.ivtr-od.tpg.ch/v1/GetDisruptions.json"
        }
        Fuel.get(url, listOf("key" to App.tpgApiKey)).responseString { _, _, result ->
            result.fold({ responseString ->
                val moshi = Moshi.Builder()
                        .add(KotlinJsonAdapterFactory())
                        .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
                        .build()

                val moshiAdapter = moshi.adapter<DisruptionsGroup>(DisruptionsGroup::class.java)

                val disruptions = java.util.ArrayList(moshiAdapter.fromJson(responseString)!!.disruptions.sortedBy { it.line })

                val adapter = RecyclerAdapter(disruptions)

                recycler_view.adapter = adapter

                swipeRefreshLayout.isRefreshing = false
            }, {
                swipeRefreshLayout.isRefreshing = false
            })
        }
    }

    companion object {
        fun newInstance(): DisruptionsFragment {
            val fragment = DisruptionsFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}

class RecyclerAdapter(private val disruptions: ArrayList<Disruption>) : RecyclerView.Adapter<DisruptionHolder>() {
    override fun onBindViewHolder(holder: DisruptionHolder, position: Int) {
        val stop = disruptions[position]
        holder.bindDisruption(stop)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisruptionHolder {
        val inflatedView = parent.inflate(R.layout.fragment_disruptions_cell, false)
        return DisruptionHolder(inflatedView)
    }

    override fun getItemCount() = disruptions.size
}

class DisruptionHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
    private var view: View = v
    private var disruption: Disruption? = null

    init {
        v.setOnClickListener(this)
    }

    override fun onClick(v: View) {

    }

    fun bindDisruption(disruption: Disruption) {
        this.disruption = disruption
        view.lineTextView.text = "Line ${disruption.line}"
        var a = disruption.nature
        if (disruption.place != "") {
            a += " - ${disruption.place}"
        }
        view.titleTextView.text = a
        view.descriptionTextView.text = disruption.consequence

        view.lineTextView.setTextColor(App.textForLine(disruption.line, "FF"))
        view.titleTextView.setTextColor(App.textForLine(disruption.line, "DE"))
        view.lineBackground.setBackgroundColor(App.backgroundForLine(disruption.line,"FF"))
    }
}

data class DisruptionsGroup(
        val disruptions: List<Disruption>,
        val timestamp: Date
)

data class Disruption(
        val place: String,
        val nature: String,
        val consequence: String,
        @Json(name = "lineCode") val line: String)