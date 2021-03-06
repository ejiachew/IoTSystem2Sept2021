package com.example.iotsystem

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.iotsystem.databinding.ActivityMachineMonitorBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MachineMonitor : AppCompatActivity() {

    private lateinit var binding : ActivityMachineMonitorBinding

    private lateinit var repeater : Job

    private lateinit var database: DatabaseReference
    @RequiresApi(VERSION_CODES.O)
    private val dtf = DateTimeFormatter.ofPattern("yyyyMMdd")
    @RequiresApi(VERSION_CODES.O)
    private val hourDTF = DateTimeFormatter.ofPattern("HH")
    @RequiresApi(VERSION_CODES.O)
    private val minuteDTF = DateTimeFormatter.ofPattern("mm")
    @RequiresApi(VERSION_CODES.O)
    private val secondDTF = DateTimeFormatter.ofPattern("ss")

    private lateinit var mStorageReference: StorageReference

    var isMachineOn = true
    var overTemperature = false

    private val channel_ID = "channel_id_example_01"
    private val notificationID = 109

    var smartControlEnable = false
    var currentTempValue = 0.00
    var isNullDetected = false


    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_machine_monitor)

        database = Firebase.database.reference
        val database = Firebase.database
        createNotificationChannel()

        val relay1Ref = database.getReference("PI_13__CONTROL").child("relay1")
        val cameraRef = database.getReference("PI_13__CONTROL").child("camera")
        val cameraFeedbackRef = database.getReference("PI_13__CONTROL").child("camera_feedback")

        binding.imgBtnBack.setOnClickListener(){
            val intent = Intent(this@MachineMonitor, MainActivity::class.java)
            finish()
            startActivity(intent)
        }

        binding.swMachine.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked){
                if(overTemperature){
                    SweetAlertDialog(this,SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("Machine Over Temperature")
                        .setContentText("It will turn on automatically once the temperature is back to normal")
                        .show()
                    binding.swMachine.isChecked = false
                }else{
                    relay1Ref.setValue("1")
                }
            }else{
                relay1Ref.setValue("0")
            }
        }

        binding.swTempControl.setOnCheckedChangeListener { _, isChecked ->
            smartControlEnable = isChecked
        }

        // get instantly update
        relay1Ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                if(dataSnapshot.getValue<String>() == "1"){
                    binding.tvMachineStatus.text = "On"
                    binding.swMachine.isChecked = true
                    isMachineOn = true
                }else{
                    binding.tvMachineStatus.text = "Off"
                    binding.swMachine.isChecked = false
                    isMachineOn = false
                }
                Log.i("infoTag",dataSnapshot.getValue<String>().toString())
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.i("infoTag", "Failed to read value.", error.toException())
            }
        })

        binding.btnCamera.setOnClickListener(){
            cameraRef.setValue("1")
            SweetAlertDialog(this,SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("Image Capturing")
                .setContentText("Please wait for few seconds")
                .show()
        }

        cameraRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                if(dataSnapshot.getValue<String>() == "1"){
                    binding.tvCameraStatus.text = "Capturing Image..."
                }else{
                    binding.tvCameraStatus.text = "Idle"
                }
                Log.i("infoTag",dataSnapshot.getValue<String>().toString())
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.i("infoTag", "Failed to read value.", error.toException())
            }
        })

        cameraFeedbackRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                var cameraFeedback = dataSnapshot.getValue<String>()
                if(cameraFeedback != "None"){
                    var filePath = cameraFeedback?.substring(9,31)
                    if (filePath != null) {

                        val progressDialog = ProgressDialog(this@MachineMonitor)
                        progressDialog.setMessage("Fetching Image...")
                        progressDialog.setCancelable(false)
                        progressDialog.show()

                        mStorageReference = FirebaseStorage.getInstance().reference.child("PI_13__CONTROL/${filePath}")

                        val localFile = File.createTempFile("tempImage","jpg")
                        mStorageReference.getFile(localFile).addOnSuccessListener {

                            if(progressDialog.isShowing)
                                progressDialog.dismiss()


                            val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                            binding.imgCaptured.setImageBitmap(bitmap)
                            binding.imgCaptured.visibility = View.VISIBLE

                        }.addOnFailureListener(){
                            if(progressDialog.isShowing)
                                progressDialog.dismiss()
                        }

                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.i("infoTag", "Failed to read value.", error.toException())
            }
        })

    }

    @RequiresApi(VERSION_CODES.O)
    override fun onResume() {
        super.onResume()

        repeater = repeatFun()
    }

    override fun onStop() {
        super.onStop()
        Log.i("newTag","Stop Getting Data form Database")
        repeater.cancel()
    }

    @RequiresApi(VERSION_CODES.O)
    private fun repeatFun(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            while(isActive) {
                getData()
                delay(1000)
            }
        }
    }


    var isAlertDisplayError = false
    var isAlertDisplayWarning = false

    @RequiresApi(VERSION_CODES.O)
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
                    //binding.tvMachineTemp.text = snap.child("rand2").getValue<String>() + "??C"
                    binding.tvMachineTemp.textSize = 20F

                    val lcdRef = database.getReference("PI_13__CONTROL").child("lcdtxt")
                    val relay1Ref = database.getReference("PI_13__CONTROL").child("relay1")

                    var tempValue = snap.child("rand2").getValue<String>()?.toDouble()

                    if(tempValue != null){
                        binding.tvMachineTemp.text = snap.child("rand2").getValue<String>() + "??C"
                    }else{
                        isNullDetected = true
                        Log.i("newTag","Null Detected : $tempValue")

                        if(isNullDetected){
                            Log.i("newTag","Close and turn on the repeater again")
                            repeater.cancel()
                            isNullDetected = false
                            repeater = repeatFun()
                        }
                    }


                    if(smartControlEnable){
                        if (tempValue != null) {
                            if(tempValue > 750){
                                binding.tvAlertRemark.text = "Over Temperature"
                                binding.imageViewAlert.setImageResource(R.drawable.alert)
                                lcdRef.setValue("Over Temperature")

                                relay1Ref.setValue("0")

                                if(!isAlertDisplayError && isMachineOn){
                                    SweetAlertDialog(this,SweetAlertDialog.ERROR_TYPE)
                                        .setTitleText("Machine Shutdown")
                                        .setContentText("It will be turn on again once the temperature is back to normal")
                                        .show()
                                    isMachineOn = false
                                    isAlertDisplayError = true
                                    overTemperature = true
                                    sendNotification("Machine temperature is too high, so the machine is turn off now")
                                }

                            }else if(tempValue > 500 && tempValue <= 750){
                                binding.tvAlertRemark.text = "High Temperature"
                                binding.imageViewAlert.setImageResource(R.drawable.warning)
                                lcdRef.setValue("High Temperature")


                                if(!isAlertDisplayWarning && isMachineOn){
                                    SweetAlertDialog(this,SweetAlertDialog.WARNING_TYPE)
                                        .setTitleText("High Temperature")
                                        .setContentText("It will be turn off once the temperature is over")
                                        .show()
                                    isAlertDisplayWarning = true
                                    sendNotification("Machine temperature is quite high, it will be turn off once the temperature is over")
                                }
                            }else{
                                binding.tvAlertRemark.text = "Normal Temperature"
                                binding.imageViewAlert.setImageResource(R.drawable.bell)
                                lcdRef.setValue("==Normal Temp!==")

                                if(!isMachineOn){

                                    relay1Ref.setValue("1")

                                    SweetAlertDialog(this,SweetAlertDialog.SUCCESS_TYPE)
                                        .setTitleText("Machine Turn On")
                                        .setContentText("The machine temperature is back to normal")
                                        .show()
                                    isMachineOn = true
                                    overTemperature = false
                                }
                            }
                        }
                    }


                }
        }else{
            Log.i("newTag",currentSec.toString())
        }
    }


    private fun createNotificationChannel(){
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            val name ="Machine Temperature Detection"
            val descriptionText="Machine temperature is too high, so the machine is turn off now"
            val importance= NotificationManager.IMPORTANCE_DEFAULT
            val channel= NotificationChannel(channel_ID,name,importance).apply {
                description=descriptionText
            }

            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(contentText : String){
        val builder = NotificationCompat.Builder(this, channel_ID)
            .setSmallIcon(R.drawable.alert)
            .setContentTitle("Machine Temperature Detection")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)){
            notify(notificationID,builder.build())
        }
    }


























}