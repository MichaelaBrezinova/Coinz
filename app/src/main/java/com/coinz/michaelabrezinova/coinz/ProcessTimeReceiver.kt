package com.coinz.michaelabrezinova.coinz

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

//Receiver called every 10 seconds
class ProcessTimeReceiver : BroadcastReceiver() {

    private val tag = "ProcessTimeReceiver"

    //Checks if the date and hour are up to date, if not restarts corresponding activity
    override fun onReceive(context: Context, intent: Intent) {

        val sdf = SimpleDateFormat("yyyy/M/dd hh:mm:ss", Locale.US)
        val date = sdf.format(Date()).substring(0,10)
        val hour = sdf.format(Date()).substring(11,13)

        Timber.tag(tag).d( "check of the date and hour")
        println("process time receiver check ")

        when {
            date!=MapsActivity.currentDate -> {
                Timber.tag(tag).d( "date has changed, Main Activity restarted")
                val mainIntent = Intent(context, MainActivity::class.java)
                context.startActivity(mainIntent)
            }
            hour>MapsActivity.currentHour -> {
                Timber.tag(tag).d( "part of day has changed, Maps Activity restarted")
                val mapsIntent = Intent(context, MapsActivity::class.java)
                context.startActivity(mapsIntent)
            }
            else -> {
                Timber.tag(tag).d( "Everything up to date")
                println("process time receiver everything ok ")
            }
        }
    }
}