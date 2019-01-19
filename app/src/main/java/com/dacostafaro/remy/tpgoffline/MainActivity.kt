package com.dacostafaro.remy.tpgoffline

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.mapbox.mapboxsdk.Mapbox
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.android.synthetic.main.activity_main.*



class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, "pk.eyJ1IjoicmVteWRjZiIsImEiOiJjamtubzJrcXYxYm9qM3JtZ3FibHA5MmZxIn0.zObvUnRHHWelQAPLFQpduw");
        setContentView(R.layout.activity_main)
        mapView.onCreate(savedInstanceState)
        NavigationUI.setupActionBarWithNavController(this, NavHostFragment.findNavController(nav_host_fragment))
    }

    public override fun onStart() {
        super.onStart()
        var jsonString = application.assets.open("stops.json").bufferedReader().use{
            it.readText()
        }
        var moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()


        var stopsMoshiAdapter = moshi.adapter<Array<Stop>>(Array<Stop>::class.java)
        App.stops = ArrayList(stopsMoshiAdapter.fromJson(jsonString)!!.toList().sortedBy { it.name })

        jsonString = application.assets.open("linesColors.json").bufferedReader().use{
            it.readText()
        }
        moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        var linesColorsMoshiAdapter = moshi.adapter<Array<LineColor>>(Array<LineColor>::class.java)
        App.linesColors = ArrayList(linesColorsMoshiAdapter.fromJson(jsonString)!!.toList().sortedBy { it.line })

        mapView.onStart()
    }

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    public override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onSupportNavigateUp(): Boolean = NavHostFragment.findNavController(nav_host_fragment).navigateUp()
}
