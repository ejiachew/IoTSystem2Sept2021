package com.example.iotsystem

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.TimePicker
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import cn.pedant.SweetAlert.SweetAlertDialog
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
import java.util.*


class CorridorLightControl : AppCompatActivity(), TimePickerDialog.OnTimeSetListener {

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

    var hour = 0
    var minute = 0

    var hourOn = -1
    var hourOn24 = 0
    var hourOff = 0
    var hourOff24 = 0
    var minuteOn = -1
    var minuteOff = 0

    var timeConfigureEnable = false
    var onTimeConfiguration = false

    var smartControlEnable = false
    var currentLightValuve = 0.0
    var lightValueLimit = 100.0

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


//        binding.swSimulation.setOnCheckedChangeListener{ _, isChecked ->
//            if(isChecked){ // set to auto light control by system
//
//                binding.swFrontDoorLight.isEnabled = false
//                binding.swBackDoorLight.isEnabled = false
//
//                binding.seekbarSimulator.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
//                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
//                        var value = p1.toString()
//
//                        when (value) {
//                            "0" -> {
//                                binding.tvDay.text = getString(R.string.morning)
//                                binding.imgSimulator.setImageResource(R.drawable.morning)
//                                ledRef.setValue("180")
//                            }
//                            "1" -> {
//                                binding.tvDay.text = getString(R.string.sunnyDay)
//                                binding.imgSimulator.setImageResource(R.drawable.sunny_day)
//                                ledRef.setValue("250")
//                            }
//                            "2" -> {
//                                binding.tvDay.text = getString(R.string.rainyDay)
//                                binding.imgSimulator.setImageResource(R.drawable.rainy_day)
//                                ledRef.setValue("70")
//                            }
//                            else -> {
//                                binding.tvDay.text = getString(R.string.night)
//                                binding.imgSimulator.setImageResource(R.drawable.night)
//                                ledRef.setValue("0")
//                            }
//                        }
//                    }
//
//                    override fun onStartTrackingTouch(p0: SeekBar?) {}
//                    override fun onStopTrackingTouch(p0: SeekBar?) {}
//
//                })
//
//            }else{
//                binding.swFrontDoorLight.isEnabled = true
//                binding.swBackDoorLight.isEnabled = true
//                ledRef.setValue("0")
//            }
//        }

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
//                            Log.i("sunTag","Current Time: $currentTime")
//                            Log.i("sunTag","Sunrise Time: $sunriseFormatted")
//                            Log.i("sunTag","Sunset Time: $sunsetFormatted")
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
                lefRef.setValue("0")
            }
        }


        binding.swTimerControl.setOnCheckedChangeListener{ _, isChecked ->

             if (isChecked){
                 if(hourOn == -1){
                     getTimer()
                     TimePickerDialog(this,this,hour,minute,true).show()
                     TimePickerDialog(this,this,hour,minute,true).show()
                 }
                binding.swSun.isChecked = false
                binding.swSun.isEnabled = false
                binding.swFrontDoorLight.isEnabled = false
                binding.swBackDoorLight.isEnabled = false
                 timeConfigureEnable =true
            }else{
                binding.swSun.isEnabled = true
                binding.swFrontDoorLight.isEnabled = true
                binding.swBackDoorLight.isEnabled = true
                 timeConfigureEnable =false
            }
        }

        binding.btnTimerConfigure.setOnClickListener(){
            getTimer()
            TimePickerDialog(this,this,hour,minute,true).show()
            TimePickerDialog(this,this,hour,minute,true).show()
        }

        binding.swSmartControl.setOnCheckedChangeListener { _, isChecked ->

            smartControlEnable = isChecked
            if(isChecked){
                binding.swFrontDoorLight.isEnabled = false
                binding.swBackDoorLight.isEnabled = false
                if(currentLightValuve <= lightValueLimit){ // if sense no light intensity then turn on the light
                    Log.i("newTag",currentLightValuve.toString())
                    relay1Ref.setValue("1")
                    relay2Ref.setValue("1")
                }else{                 // if sense got light intensity then turn off the light
                    relay1Ref.setValue("0")
                    relay2Ref.setValue("0")
                }
            }else{
                binding.swFrontDoorLight.isEnabled = true
                binding.swBackDoorLight.isEnabled = true
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
                    //binding.tvLightLevel.text = snap.child("rand1").getValue<String>() + " lux"

                    var lightValue = snap.child("rand1").getValue<String>().toString().toDouble()
                    binding.tvLightLevel.text = (lightValue * 3).toString() + " lux"
                    currentLightValuve = lightValue * 3

                    val relay1Ref = database.getReference("PI_13__CONTROL").child("relay1")
                    val relay2Ref = database.getReference("PI_13__CONTROL").child("relay2")

                    if(timeConfigureEnable){
                        checkTimeConfigured()
                    }else if(smartControlEnable){ // Automation Light Control
                        if((lightValue * 3) <= lightValueLimit){ // if sense no light intensity then turn on the light
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

    private fun getTimer(){
        val cal = Calendar.getInstance()
        hour = cal.get(Calendar.HOUR_OF_DAY)
        minute = cal.get(Calendar.MINUTE)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        var am_pm = "AM"
        if(!onTimeConfiguration){
            hourOn = hourOfDay
            hourOn24 = hourOfDay
            minuteOn = minute
            if(hourOn > 12){
                hourOn -= 12
                am_pm = "PM"
            }
            binding.tvTurnOnTime.text = String.format("%02d:%02d %s",hourOn,minuteOn,am_pm)
            onTimeConfiguration = true
        }else{
            hourOff = hourOfDay
            hourOff24 = hourOfDay
            minuteOff = minute
            if(hourOff > 12)
            {
                hourOff -= 12
                am_pm = "PM"
            }
            binding.tvTurnOffTime.text = String.format("%02d:%02d %s",hourOff,minuteOff,am_pm)
            onTimeConfiguration = false

            SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("Configuration Done")
                .show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkTimeConfigured(){

        database = Firebase.database.reference

        val database = Firebase.database
        val relay1RefTime = database.getReference("PI_13__CONTROL").child("relay1")
        val relay2RefTime = database.getReference("PI_13__CONTROL").child("relay2")

        val now2 = LocalDateTime.now()
        var currentHour24 = hourDTF.format(now2).toString().toInt() // 22
        var currentMin24 = minuteDTF.format(now2).toString().toInt()
        if(hourOn24 > hourOff24){
            if(currentHour24 >= hourOn24 && currentMin24 >= minuteOn){
                relay1RefTime.setValue("1")
                relay2RefTime.setValue("1")
            }else{
                relay1RefTime.setValue("0")
                relay2RefTime.setValue("0")
            }
        }else if(hourOff24 > hourOn24){
            if(currentHour24 >= hourOn24 && currentMin24 >= minuteOn && currentHour24 <= hourOff24 && currentMin24 <= minuteOff){
                relay1RefTime.setValue("1")
                relay2RefTime.setValue("1")
            }else{
                relay1RefTime.setValue("0")
                relay2RefTime.setValue("0")
            }
        }else{
            if(currentMin24 >= minuteOff || currentMin24 < minuteOn){
                relay1RefTime.setValue("0")
                relay2RefTime.setValue("0")
            }else if(currentMin24 in minuteOn until minuteOff){
                relay1RefTime.setValue("1")
                relay2RefTime.setValue("1")
            }
        }
    }
}
