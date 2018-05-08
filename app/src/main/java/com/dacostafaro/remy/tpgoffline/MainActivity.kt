package com.dacostafaro.remy.tpgoffline

import android.app.AlertDialog
import android.app.Fragment
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.dacostafaro.remy.tpgoffline.departures.Stop
import com.dacostafaro.remy.tpgoffline.departures.StopsFragment
import com.dacostafaro.remy.tpgoffline.disruptions.DisruptionsFragment
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import java.util.ArrayList
import android.content.Context.SEARCH_SERVICE
import android.app.SearchManager
import android.content.Context
import android.graphics.Color
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuInflater
import android.widget.Toast
import android.content.ClipData.Item
import android.graphics.drawable.ColorDrawable
import com.dacostafaro.remy.tpgoffline.maps.MapsListFragment


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    var currentFragment: String = ""

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search, menu)

        val myActionMenuItem = menu.findItem(R.id.action_search)

        if (currentFragment != "stops") {
            myActionMenuItem.isVisible = false
            return false
        }

        myActionMenuItem.isVisible = true
        val searchView = myActionMenuItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                // Toast like print
                if (!searchView.isIconified) {
                    searchView.isIconified = true
                }
                myActionMenuItem.collapseActionView()
                return false
            }

            override fun onQueryTextChange(s: String): Boolean {
                if (fragmentManager.findFragmentByTag(currentFragment) is StopsFragment) {
                    var fragment = fragmentManager.findFragmentByTag(currentFragment) as StopsFragment
                    fragment.refresh(s)
                }

                // UserFeedback.show( "SearchOnQueryTextChanged: " + s);
                return false
            }
        })
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

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

        val fragment = StopsFragment()
        currentFragment = "stops"

        val ft = fragmentManager.beginTransaction()
        ft.replace(R.id.frameContainer, fragment, currentFragment)
        ft.commit()
        supportActionBar?.title = this.baseContext.getString(R.string.departures)
        if (App.darkMode) {
            supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        var fragment: Fragment? = null
        when (item.itemId) {
            R.id.nav_departures -> {
                fragment = StopsFragment()
                currentFragment = "stops"
                supportActionBar?.title = this.baseContext.getString(R.string.departures)
            }
            R.id.nav_disruptions -> {
                fragment = DisruptionsFragment()
                currentFragment = "disruptions"
                supportActionBar?.title = this.baseContext.getString(R.string.disruptions)
            }
            R.id.nav_maps -> {
                fragment = MapsListFragment()
                currentFragment = "maps"
                supportActionBar?.title = this.baseContext.getString(R.string.maps)
            }
            else -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("WIP")
                builder.setMessage("Sorry, this part is currently undeveloped.")
                builder.setPositiveButton("OK") {_, _ ->  }
                builder.create().show()
            }
        }

        if(null != fragment) {
            val ft = fragmentManager.beginTransaction()
            ft.replace(R.id.frameContainer, fragment, currentFragment)
            ft.commit()
        }

        invalidateOptionsMenu()

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}