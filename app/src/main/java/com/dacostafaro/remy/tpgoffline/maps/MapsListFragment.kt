package com.dacostafaro.remy.tpgoffline.maps

import android.os.Bundle
import android.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dacostafaro.remy.tpgoffline.R
import com.dacostafaro.remy.tpgoffline.inflate
import kotlinx.android.synthetic.main.fragment_maps_list.*
import kotlinx.android.synthetic.main.maps_list_card_view.view.*


class MapsListFragment : Fragment() {
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onStart() {
        super.onStart()

        linearLayoutManager = LinearLayoutManager(this.activity)
        mapsListRecyclerView.layoutManager = linearLayoutManager

        val maps = arrayListOf(
                TpgMap(getString(R.string.urbain_map), R.drawable.urbain_map),
                TpgMap(getString(R.string.regional_map), R.drawable.periurbain_map),
                TpgMap(getString(R.string.noctambus_urbain_map), R.drawable.noc_urbain_map),
                TpgMap(getString(R.string.noctambus_regional_map), R.drawable.noc_periurbain_map))
        val adapter = MapListRecyclerAdapter(maps)
        mapsListRecyclerView.adapter = adapter
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_maps_list, container, false)
    }
}

class TpgMap(val name: String, val image: Int)

class MapListRecyclerAdapter(private val maps: ArrayList<TpgMap>) : RecyclerView.Adapter<MapHolder>() {
    override fun onBindViewHolder(holder: MapHolder, position: Int) {
        val tpgMap = maps[position]
        holder.bindStop(tpgMap)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapHolder {
        val inflatedView = parent.inflate(R.layout.maps_list_card_view, false)
        return MapHolder(inflatedView)
    }

    override fun getItemCount() = maps.size
}

class MapHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
    private var view: View = v
    private var tpgMap: TpgMap? = null

    init {
        v.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        /*val context = view.context
        val intent = Intent(context, DeparturesActivity::class.java)
        intent.putExtra("stop", stop?.code)
        context.startActivity(intent)*/
    }

    fun bindStop(tpgMap: TpgMap) {
        this.tpgMap = tpgMap
        view.mapPreviewImageView.setImageResource(tpgMap.image)
        view.mapTitle.text = tpgMap.name
    }
}