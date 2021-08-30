package com.example.iotsystem

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.iotsystem.databinding.ActivityCorridorLightControlBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import java.net.URL
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


class CorridorLightControl : AppCompatActivity() {

    private lateinit var binding : ActivityCorridorLightControlBinding

    private lateinit var sunriseFormatted : LocalTime
    private lateinit var sunsetFormatted : LocalTime

    private lateinit var repeater : Job

    private lateinit var database: DatabaseReference
    @RequiresApi(Build.VERSION_CODES.O)
    private val dtf = DateTimeFormatter.ofPattern("yyyyMMdd")
    @RequiresApi(Build.VERSION_CODES.O)
    private val hourDTF = DateTimeFormatter.ofPattern("HH")
    @RequiresApi(Build.VERSION_CODES.O)
    private val minuteDTF = DateTimeFormatter.ofPattern("mm")
    @RequiresApi(Build.VERSION_CODES.O)
    private val secondDTF = DateTimeFormatter.ofPattern("ss")

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_corridor_light_control)

        database = Firebase.database.reference

        val database = Firebase.database


        val ledRef = database.getReference("PI_13__CONTROL").child("ledlgt")
        val relay1Ref = database.getReference("PI_13__CONTROL").child("relay1")
        val relay2Ref = database.getReference("PI_13__CONTROL").child("relay2")


        binding.imgBtnBack.setOnClickListener{
            val intent = Intent(this@CorridorLightControl, MainActivity::class.java)
            finish()
            startActivity(intent)
        }

        // get instantly update
        relay1Ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                if(dataSnapshot.getValue<String>() == "1"){
                    binding.tvFrontStatus.text = "On"
                    binding.swFrontDoorLight.isChecked = true
                }else{
                    binding.tvFrontStatus.text = "Off"
                    binding.swFrontDoorLight.isChecked = false
                }
                Log.i("infoTag",dataSnapshot.getValue<String>().toString())
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.i("infoTag", "Failed to read value.", error.toException())
            }
        })

        relay2Ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                if(dataSnapshot.getValue<String>() == "1"){
                    binding.tvBackStatus.text = "On"
                    binding.swBackDoorLight.isChecked = true
                }else{
                    binding.tvBackStatus.text = "Off"
                    binding.swBackDoorLight.isChecked = false
                }
                Log.i("infoTag",dataSnapshot.getValue<String>().toString())
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.i("infoTag", "Failed to read value.", error.toException())
            }
        })

        binding.swFrontDoorLight.setOnCheckedChangeListener { _, isChecked ->
                if(isChecked){
                    relay1Ref.setValue("1")
                }else{
                    relay1Ref.setValue("0")
                }
        }

        binding.swBackDoorLight.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked){
                relay2Ref.setValue("1")
            }else{
                relay2Ref.setValue("0")
            }
        }


        binding.swSimulation.setOnCheckedChangeListener{ _, isChecked ->
            if(isChecked){ // set to auto light control by system

                binding.swFrontDoorLight.isEnabled = false
                binding.swBackDoorLight.isEnabled = false

                binding.seekbarSimulator.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        var value = p1.toString()

                        when (value) {
                            "0" -> {
                                binding.tvDay.text = getString(R.string.morning)
                                binding.imgSimulator.setImageResource(R.drawable.morning)
                                ledRef.setValue("180")
                            }
                            "1" -> {
                                binding.tvDay.text = getString(R.string.sunnyDay)
                                binding.imgSimulator.setImageResource(R.drawable.sunny_day)
                                ledRef.setValue("250")
                            }
                            "2" -> {
                                binding.tvDay.text = getString(R.string.rainyDay)
                                binding.imgSimulator.setImageResource(R.drawable.rainy_day)
                                ledRef.setValue("70")
                            }
                            else -> {
                                binding.tvDay.text = getString(R.string.night)
                                binding.imgSimulator.setImageResource(R.drawable.night)
                                ledRef.setValue("0")
                            }
                        }
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {}
                    override fun onStopTrackingTouch(p0: SeekBar?) {}

                })

            }else{
                binding.swFrontDoorLight.isEnabled = true
                binding.swBackDoorLight.isEnabled = true
                ledRef.setValue("0")
            }
        }

        binding.swSun.setOnCheckedChangeListener{ _, isChecked ->

            val relay1Ref = database.getReference("PI_13__CONTROL").child("relay1")
            val relay2Ref = database.getReference("PI_13__CONTROL").child("relay2")
            val lefRef = database.getReference("PI_13__CONTROL").child("ledlgt")

            if(isChecked){ // set to auto light control by system

                binding.swFrontDoorLight.isEnabled = false
                binding.swBackDoorLight.isEnabled = false

                // Call API to get Sunrise and Sunset
                doAsync {
                    runRequest()
                    uiThread{
                        //toast("Request Performed")
                        binding.tvSunriseTime.text = sunriseFormatted.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
                        binding.tvSunsetTime.text = sunsetFormatted.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))



                        var currentTime = LocalTime.now()
                        if(currentTime.compareTo(sunriseFormatted) == -1 || currentTime.compareTo(sunsetFormatted) == 1){
                            Log.i("newTag","Current Time: $currentTime")
                            Log.i("newTag","Sunrise Time: $sunriseFormatted")
                            Log.i("newTag","Sunset Time: $sunsetFormatted")
                            // still before sunrise   AND  still night time
                            // in this both situation, I need to turn on the light
                            // open light
                            relay1Ref.setValue("1")
                            relay2Ref.setValue("1")
                            lefRef.setValue("0")
                        }else if(currentTime.compareTo(sunriseFormatted) == 1 && currentTime.compareTo(sunsetFormatted) == -1) {
                            // day time but before sunset
                            // In this situation, I need to turn off the light
                            // close light
                            relay1Ref.setValue("0")
                            relay2Ref.setValue("0")
                            lefRef.setValue("250")
                        }

                    }
                }

            }else{
                binding.swFrontDoorLight.isEnabled = true
                binding.swBackDoorLight.isEnabled = true
//                relay1Ref.setValue("0")
//                relay2Ref.setValue("0")
                lefRef.setValue("0")
            }
        }





    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun runRequest(){

        val url = "http://api.openweathermap.org/data/2.5/weather?lat=3.2162&lon=101.7290&appid=8681f87b315ecb324917270ad5836014"

        val resultJson = URL(url).readText()
        val jsonObj = JSONObject(resultJson)
        val sys = jsonObj.getJSONObject("sys")
        val sunrise = sys.getString("sunrise")
        val sunset = sys.getString("sunset")

        sunriseFormatted = Instant.ofEpochSecond(sunrise.toLong())
            .atZone(ZoneId.systemDefault())
            .toLocalTime()

        sunsetFormatted = Instant.ofEpochSecond(sunset.toLong())
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()

        repeater = repeatFun()
    }

    override fun onStop() {
        super.onStop()
        Log.i("newTag","Stop Getting Data form Database")
        repeater.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun repeatFun(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            while(isActive) {
                getData()
                delay(1000)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getData(){
        val now = LocalDateTime.now()
        var todayDate = dtf.format(now)
        var currentHour = hourDTF.format(now).toString()
        var currentMin = minuteDTF.format(now).toString()
        var currentSec = secondDTF.format(now).toString().toInt()

        if(currentSec % 10 == 0) {
            database = Firebase.database.reference

            val database = Firebase.database
            database.getReference("PI_13__${todayDate}").child(currentHour)
                .child(currentMin + String.format("%02d",currentSec)).get().addOnSuccessListener { snap ->
                    binding.tvLightLevel.text = snap.child("light").getValue<String>()

                    var lightValue = snap.child("light").getValue<String>().toString().toInt()
                    val relay1Ref = database.getReference("PI_13__CONTROL").child("relay1")
                    val relay2Ref = database.getReference("PI_13__CONTROL").child("relay2")

                    if(binding.swSimulation.isChecked || binding.swSun.isChecked){ // Automation Light Control
                        if(lightValue == 0 || lightValue <= 100){ // if sense no light intensity then turn on the light
                            relay1Ref.setValue("1")
                            relay2Ref.setValue("1")
                        }else{                 // if sense got light intensity then turn off the light
                            relay1Ref.setValue("0")
                            relay2Ref.setValue("0")
                        }
                    }
                }
        }else{
            Log.i("newTag",currentSec.toString())
        }
    }
}
