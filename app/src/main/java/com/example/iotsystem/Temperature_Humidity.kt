package com.example.iotsystem

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import com.example.iotsystem.databinding.ActivityTemperatureHumidityBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.widget.CompoundButton
import kotlin.math.roundToInt


class Temperature_Humidity : AppCompatActivity() {

    private lateinit var binding: ActivityTemperatureHumidityBinding
    private lateinit var database: DatabaseReference

    private lateinit var repeater : Job
    var aircond_SmartControlEnabled : Boolean = false
    var dehumidifier_SmartControlEnabled : Boolean = false

    @RequiresApi(Build.VERSION_CODES.O)
    private val dtf = DateTimeFormatter.ofPattern("yyyyMMdd")

    @RequiresApi(Build.VERSION_CODES.O)
    private val hourDTF = DateTimeFormatter.ofPattern("HH")

    @RequiresApi(Build.VERSION_CODES.O)
    private val minuteDTF = DateTimeFormatter.ofPattern("mm")

    @RequiresApi(Build.VERSION_CODES.O)
    private val secondDTF = DateTimeFormatter.ofPattern("ss")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temperature_humidity)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_temperature_humidity)

        database = Firebase.database.reference
        val database = Firebase.database
        val relay1Ref = database.getReference("PI_13__CONTROL").child("relay1")
        val relay2Ref =database.getReference("PI_13__CONTROL").child("relay2")


        binding.imgBtnBack.setOnClickListener{
            val intent = Intent(this@Temperature_Humidity, MainActivity::class.java)
            finish()
            startActivity(intent)
        }

        binding.switchAircond.setOnCheckedChangeListener { _, isChecked ->
            var msg=""
            if(isChecked) {
                msg = "Air-conditioner turned On"
                relay1Ref.setValue("1")
            }
            else {
                msg = "Air-conditioner turned Off"
                relay1Ref.setValue("0")
            }

            val toast = Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT)
            toast.show()
        }

        binding.switchDehumidifier.setOnCheckedChangeListener { _, isChecked ->
            var msg=""
            if(isChecked) {
                msg = "Dehumidifier turned On"
                relay2Ref.setValue("1")
            }
            else {
                msg = "Dehumidifier turned Off"
                relay2Ref.setValue("0")
            }

            val toast = Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT)
            toast.show()
        }

        binding.switchAircondAuto.setOnCheckedChangeListener{_,isChecked ->
            val builder = AlertDialog.Builder(this)

            // set title
            builder.setTitle("Smart Control")

            //set content area
            builder.setMessage("Enable Smart Control to turn on the Air-Conditioner when temperature is higher or equal than the temperature you set " + "\n" +
                    "You can manually control the Air-Conditioner by turning off Smart Control")

            //set positive button
            builder.setPositiveButton(
                "Ok") { dialog, id ->
                // User clicked Update Now button

            }
            builder.show()
            binding.switchAircond.isEnabled = !isChecked
            aircond_SmartControlEnabled = isChecked





        }

        binding.switchDehumidifierAuto.setOnCheckedChangeListener{_,isChecked ->
            val builder = AlertDialog.Builder(this)

            // set title
            builder.setTitle("Smart Control")

            //set content area
            builder.setMessage("Enable Smart Control to turn on the Dehumidifier when humidity is higher or equal than the humidity you set. " + "\n"+
                    "You can manually control the Dehumidifier by turning off Smart Control")

            //set positive button
            builder.setPositiveButton(
                "Ok") { dialog, id ->
                // User clicked Update Now button

            }
            builder.show()

            binding.switchDehumidifier.isEnabled = !isChecked
            dehumidifier_SmartControlEnabled=isChecked

        }


    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()

        repeater=repeatFun()
    }

    override fun onStop() {
        super.onStop()
        repeater.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun repeatFun(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                getData()
                delay(1000)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getData() {
        val now = LocalDateTime.now()
        var todayDate = dtf.format(now)
        var currentHour = hourDTF.format(now).toString()
        var currentMin = minuteDTF.format(now).toString()
        var currentSec = secondDTF.format(now).toString().toInt()

        if (currentSec % 10 == 0) {
            database = Firebase.database.reference

            Log.i("newTag", currentSec.toString())


            val database = Firebase.database
            database.getReference("PI_13__${todayDate}").child(currentHour)
                .child(currentMin + String.format("%02d", currentSec)).get()
                .addOnSuccessListener { snap ->
                    val humid= snap.child("humid").getValue<String>()?.toDouble()?.times(10)
                    binding.tvHumid.text = humid?.roundToInt().toString()+"%"
                    binding.tvCelcius.text = snap.child("tempe").getValue<String>()+ 0x00B0.toChar()+"C"

                }
            var relay1 = 0
            var relay2 = 0
            database.getReference("PI_13__CONTROL").get().addOnSuccessListener { snap ->
                relay1 = snap.child("relay1").getValue<String>()?.toInt() ?: 0
                relay2 = snap.child("relay2").getValue<String>()?.toInt() ?: 0

                //turn on switch according to firebase value
                if (relay1 == 0) {
                    binding.tvAircondStatus.text = "Off"
                    binding.switchAircond.isChecked=false
                } else {
                    binding.tvAircondStatus.text = "On"
                    binding.switchAircond.isChecked=true
                }

                if (relay2 == 0) {
                    binding.tvDehumidifierStatus.text = "Off"
                    binding.switchDehumidifier.isChecked=false
                } else {
                    binding.tvDehumidifierStatus.text = "On"
                    binding.switchDehumidifier.isChecked=true
                }

                //check smart control
                if(aircond_SmartControlEnabled){ AirCond_SmartControl() }
                if(dehumidifier_SmartControlEnabled){Dehumidifier_SmartControl()}
            }


        } else {
            Log.i("newTag", currentSec.toString())
        }

    }

    fun AirCond_SmartControl(){

        database = Firebase.database.reference
        val database = Firebase.database

        var temp=binding.tvCelcius.text.toString().substring(0,4)
        var tempLimit=binding.txtTempLimit.text.toString().toDouble()

        if(temp.toDouble()>=tempLimit && !binding.switchAircond.isChecked){
            database.getReference("PI_13__CONTROL").child("relay1").setValue("1")
            binding.tvAircondStatus.text = "On"
            binding.switchAircond.isChecked=true
        }else if(temp.toDouble()<tempLimit && binding.switchAircond.isChecked){
            database.getReference("PI_13__CONTROL").child("relay1").setValue("0")
            binding.tvAircondStatus.text = "Off"
            binding.switchAircond.isChecked=false
        }

    }

    fun Dehumidifier_SmartControl(){

        database = Firebase.database.reference
        val database = Firebase.database
        var humidLimit=binding.txtHumidLimit.text.toString().toInt()

        var humid=binding.tvHumid.text.toString().substring(0,2)
        if(humid.toInt()>=humidLimit && !binding.switchDehumidifier.isChecked){
            database.getReference("PI_13__CONTROL").child("relay2").setValue("1")
            binding.tvDehumidifierStatus.text = "On"
            binding.switchDehumidifier.isChecked=true
        }else if(humid.toInt()<humidLimit && binding.switchDehumidifier.isChecked){
            database.getReference("PI_13__CONTROL").child("relay2").setValue("0")
            binding.tvDehumidifierStatus.text = "Off"
            binding.switchDehumidifier.isChecked=false
        }

    }


}