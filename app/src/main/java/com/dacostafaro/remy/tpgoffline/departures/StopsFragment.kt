package com.dacostafaro.remy.tpgoffline.departures

import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.*
import com.dacostafaro.remy.tpgoffline.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_stops.*
import kotlinx.android.synthetic.main.fragment_stops_cell.view.*
import java.util.ArrayList


class StopsFragment : Fragment() {
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: StopRecyclerAdapter
    var filterText = ""

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater!!.inflate(R.layout.fragment_stops, container, false)

    }

    override fun onStart() {
        super.onStart()

        linearLayoutManager = LinearLayoutManager(this.activity)
        recycler_view.layoutManager = linearLayoutManager

        refresh(filterText)
        if (App.darkMode) {
            recycler_view.setBackgroundColor(Color.parseColor("#262626"))
        }
    }

    fun refresh(filterText: String) {
        this.filterText = filterText
        var stops = App.stops
        if (filterText != "") {
            stops = ArrayList(stops.filter { it.name.escaped.contains(filterText.escaped) || it.code.escaped == filterText.escaped })
        }
        adapter = StopRecyclerAdapter(stops)

        //This is the code to provide a sectioned list
        val sections = ArrayList<SimpleSectionedRecyclerViewAdapter.Section>()

        var keys = stops.map { it.name[0] }.distinct()

        var position = 0
        keys.forEach {
            sections.add(SimpleSectionedRecyclerViewAdapter.Section(position, "$it"))
            position += stops.filter { y -> y.name[0] == it}.count()
        }

        //Add your adapter to the sectionAdapter
        val mSectionedAdapter = SimpleSectionedRecyclerViewAdapter(this.activity, R.layout.section, R.id.section_text, adapter)
        mSectionedAdapter.setSections(sections.toTypedArray())

        //Apply this adapter to the RecyclerView

        recycler_view.adapter = mSectionedAdapter
    }

    companion object {
        fun newInstance(): StopsFragment {
            val fragment = StopsFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}

class StopRecyclerAdapter(private val stops: ArrayList<Stop>) : RecyclerView.Adapter<StopHolder>() {
    override fun onBindViewHolder(holder: StopHolder, position: Int) {
        val stop = stops[position]
        holder.bindStop(stop)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopHolder {
        val inflatedView = parent.inflate(R.layout.fragment_stops_cell, false)
        return StopHolder(inflatedView)
    }

    override fun getItemCount() = stops.size
}

class StopHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
    private var view: View = v
    private var stop: Stop? = null

    init {
        v.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val context = view.context
        val intent = Intent(context, DeparturesActivity::class.java)
        intent.putExtra("stop", stop?.code)
        context.startActivity(intent)
    }

    fun bindStop(stop: Stop) {
        this.stop = stop
        view.stopTitleTextView.text = stop.title
        view.stopSubtitleTextView.text = stop.subTitle
        if (stop.subTitle.isEmpty()) {
            view.stopSubtitleTextView.layoutParams.height = 0
            if (App.darkMode) {
                view.stopTitleTextView.setTextColor(Color.parseColor("#DEFFFFFF"))
            } else {
                view.stopTitleTextView.setTextColor(Color.parseColor("#DE000000"))
            }
            view.layoutParams.height = dpToPx(view.context,40)
        } else {
            view.stopSubtitleTextView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            if (App.darkMode) {
                view.stopSubtitleTextView.setTextColor(Color.parseColor("#DEFFFFFF"))
                view.stopTitleTextView.setTextColor(Color.parseColor("#B3FFFFFF"))
            } else {
                view.stopSubtitleTextView.setTextColor(Color.parseColor("#DE000000"))
                view.stopTitleTextView.setTextColor(Color.parseColor("#B3000000"))
            }
            view.layoutParams.height = dpToPx(view.context, 60)
        }
    }
}

fun dpToPx(context: Context, dp: Int): Int {
    val density = context.resources.displayMetrics.density
    return Math.round(dp.toFloat() * density)
}

data class Stop(
        val name: String,
        val title: String,
        val subTitle: String,
        val appId: Int,
        var code: String)