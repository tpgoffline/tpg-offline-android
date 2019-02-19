package com.dacostafaro.remy.tpgoffline

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import kotlinx.android.synthetic.main.fragment_stops.*
import kotlinx.android.synthetic.main.fragment_stops_cell.view.*
import java.util.*


class StopsFragment : Fragment() {
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: StopRecyclerAdapter
    var filterText = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stops, container, false)
    }

    override fun onStart() {
        super.onStart()

        linearLayoutManager = LinearLayoutManager(this.activity)
        recycler_view.layoutManager = linearLayoutManager

        searchText.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {
                refresh(searchText.text.toString())
            }
        })

        searchText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val behavior = BottomSheetBehavior.from(view?.parent as View)
                behavior.state = STATE_EXPANDED
            }
        }

        searchText.requestFocus()

        refresh(searchText.text.toString())
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

        val sections = ArrayList<SimpleSectionedRecyclerViewAdapter.Section>()

        var keys = stops.asSequence().map { it.name[0] }.distinct().toList()

        var position = 0
        keys.forEach {
            sections.add(SimpleSectionedRecyclerViewAdapter.Section(position, "$it"))
            position += stops.asSequence().filter { y -> y.name[0] == it}.count()
        }

        val mSectionedAdapter = SimpleSectionedRecyclerViewAdapter(this.activity, R.layout.section, R.id.section_text, adapter)
        mSectionedAdapter.setSections(sections.toTypedArray())

        recycler_view.adapter = mSectionedAdapter
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
        val action = StopsFragmentDirections.actionStopsFragmentToDepartureFragment(stop!!.code)
        v.findNavController().navigate(action)
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
