package com.dacostafaro.remy.tpgoffline.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*

class DatabaseOpenHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, "OfflineDepartures", null, 1) {
    companion object {
        private var instance: DatabaseOpenHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): DatabaseOpenHelper {
            if (instance == null) {
                instance = DatabaseOpenHelper(ctx.getApplicationContext())
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Here you create tables
        db.createTable("OfflineDepartures", true,
            "id" to INTEGER + PRIMARY_KEY + UNIQUE,
            "departure_stop" to INTEGER,
            "arrival_stop" to INTEGER,
            "departure_time" to INTEGER,
            "arrival_time" to INTEGER,
            "line" to TEXT,
            "trip_id" to INTEGER,
            "destination_stop" to INTEGER,
            "day" to INTEGER
            )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Here you can upgrade tables, as usual
        db.dropTable("OfflineDepartures", true)
    }
}

class DBConnection(
    val id: Int,
    val departureStop: Int,
    val arrivalStop: Int,
    val departureTime: Int,
    val arrivalTime: Int,
    val line: String,
    val tripId: Int,
    val destinationStop: Int,
    val day: Int)

// Access property for Context
val Context.database: DatabaseOpenHelper
    get() = DatabaseOpenHelper.getInstance(applicationContext)