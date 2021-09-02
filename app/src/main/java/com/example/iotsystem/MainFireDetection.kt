package com.example.iotsystem

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.DataBindingUtil
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.iotsystem.databinding.ActivityMainFireDetectionBinding
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

class MainFireDetection : AppCompatActivity() {

    private val channel_ID="channel_id_example_01"
    private val notificationID=101

    private lateinit var binding : ActivityMainFireDetectionBinding

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

    private lateinit var mStorageReference: StorageReference

    var isNullDetected = false

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_fire_detection)

        database = Firebase.database.reference
        val database = Firebase.database

        createNotificationChannel()
        val cameraRef = database.getReference("PI_13__CONTROL").child("camera")
        val cameraFeedbackRef = database.getReference("PI_13__CONTROL").child("camera_feedback")

        binding.imgBtnBack.setOnClickListener(){
            val intent = Intent(this@MainFireDetection, MainActivity::class.java)
            finish()
            startActivity(intent)
        }

        binding.imgBtnBack.setOnClickListener{
            val intent = Intent(this@MainFireDetection, MainActivity::class.java)
            finish()
            startActivity(intent)
        }

        binding.btnCamera.setOnClickListener(){
            cameraRef.setValue("1")
            SweetAlertDialog(this,SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("Capturing Image")
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

                        val progressDialog = ProgressDialog(this@MainFireDetection)
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
            val relay1Ref = database.getReference("PI_13__CONTROL").child("relay1")
            val buzzerRef = database.getReference("PI_13__CONTROL").child("buzzer")
            database.getReference("PI_13__${todayDate}").child(currentHour)
                .child(currentMin + String.format("%02d",currentSec)).get().addOnSuccessListener { snap ->
                    //binding.tvTemp.text = snap.child("rand1").getValue<String>() + "°C"
                    binding.tvTemp.textSize = 20F
                    val temp=binding.txtTriggerTemp.text.toString().toDouble()
                    val lcdRef = database.getReference("PI_13__CONTROL").child("lcdtxt")

                    //var tempValue = snap.child("rand1").getValue<String>().toString().toDouble()
                    var tempValue = snap.child("rand1").getValue<String>()?.toDouble()

                    if(tempValue != null){
                        binding.tvTemp.text = snap.child("rand1").getValue<String>() + "°C"
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


                    if (tempValue != null) {
                        if(tempValue > temp){
                            binding.tvAlertRemark.text = "Fire detected, fire alarm and sprinkles has been triggered"
                            binding.imageViewAlert.setImageResource(R.drawable.alert)
                            lcdRef.setValue("=Fire detected!=")
                            relay1Ref.setValue("1")
                            buzzerRef.setValue("1")
                            sendNotification()
                        }else{
                            binding.tvAlertRemark.text = "There is no fire detected."
                            binding.imageViewAlert.setImageResource(R.drawable.bell)
                            relay1Ref.setValue("0")
                            buzzerRef.setValue("0")
                            lcdRef.setValue("=Temperature Ok=")
                        }
                    }
                }
        }else{
            Log.i("newTag",currentSec.toString())
        }
    }

    private fun createNotificationChannel(){
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            val name ="Fire detection"
            val descriptionText="There is a fire detect in the warehouse!!!"
            val importance=NotificationManager.IMPORTANCE_DEFAULT
            val channel=NotificationChannel(channel_ID,name,importance).apply {
                description=descriptionText
            }

            val notificationManager: NotificationManager= getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

        }
    }

    private fun sendNotification(){
        val builder = NotificationCompat.Builder(this, channel_ID)
            .setSmallIcon(R.drawable.alert)
            .setContentTitle("Fire alert")
            .setContentText("There is a fire detect in the warehouse!!!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)){
            notify(notificationID,builder.build())
        }
    }
}