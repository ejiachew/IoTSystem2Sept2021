package com.example.iotsystem.extension

import android.util.Log
import java.net.URL

class Request (private val url : String) {

    fun run(){
        val forecastJson = URL(url).readText()
        Log.d(javaClass.simpleName, forecastJson)
    }
}