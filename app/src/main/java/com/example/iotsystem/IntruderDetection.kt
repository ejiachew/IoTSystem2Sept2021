package com.example.iotsystem

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.DataBindingUtil
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.iotsystem.databinding.ActivityIntruderDetectionBinding
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class IntruderDetection : AppCompatActivity() {
    private lateinit var binding : ActivityIntruderDetectionBinding
    private lateinit var database: DatabaseReference
    private lateinit var repeater : Job
    private val channel_ID = "channel_id_example_01"
    private val notificationID = 101
    private var switchEnable:Boolean = false

    @RequiresApi(Build.VERSION_CODES.O)
    private val dtf = DateTimeFormatter.ofPattern("yyyyMMdd")
    @RequiresApi(Build.VERSION_CODES.O)
    private val hourDTF = DateTimeFormatter.ofPattern("HH")
    @RequiresApi(Build.VERSION_CODES.O)
    private val minuteDTF = DateTimeFormatter.ofPattern("mm")
    @RequiresApi(Build.VERSION_CODES.O)
    private val secondDTF = DateTimeFormatter.ofPattern("ss")

    var isNullDetected = false


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_intruder_detection)
        database = Firebase.database.reference
        val database = Firebase.database
        val buzzerRef = database.getReference("PI_13__CONTROL").child("buzzer")
        val lcdTextRef = database.getReference("PI_13__CONTROL").child("lcdtxt")
        //val relay1Ref = database.getReference("PI_13__CONTROL").child("relay1")
        val cameraRef = database.getReference("PI_13__CONTROL").child("camera")
        val cameraImgRef = database.getReference("PI_13__CONTROL").child("camera_feedback")
        binding.imgBtnBack.setOnClickListener(){
            val intent = Intent(this@IntruderDetection, MainActivity::class.java)
            finish()
            startActivity(intent)
        }

        binding.btnCamera.setOnClickListener(){
            cameraRef.setValue("1")
            SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
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

        cameraImgRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.

                var cameraFeedback = dataSnapshot.getValue<String>()
                if(cameraFeedback != "None"){

                    var currentString = cameraFeedback.toString()
                    var word  = currentString.split(" ")

                    Log.i("image", cameraFeedback.toString())
                    if(!word[1].isNullOrEmpty()){
                        val progressDialog = ProgressDialog(this@IntruderDetection)
                        progressDialog.setMessage("Fetching Image...")
                        progressDialog.setCancelable(false)
                        progressDialog.show()

                        var storage: FirebaseStorage = FirebaseStorage.getInstance()
                        var imageRef: StorageReference = storage.getReference()
                            .child("PI_13__CONTROL/")
                            .child(word[1])
                        val ONE_MEGABYTE: Long = 1024 * 1024
                        imageRef.getBytes(ONE_MEGABYTE)
                            .addOnSuccessListener(){
                                    bytes ->
                                if(progressDialog.isShowing)
                                    progressDialog.dismiss()
                                var bmp : Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                binding.imgCaptured.setImageBitmap(bmp)
                                binding.imgCaptured.visibility = View.VISIBLE
                            }.addOnFailureListener(){
                                if(progressDialog.isShowing)
                                    progressDialog.dismiss()
                            }
                    }
                }else{
                    binding.imgCaptured.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.i("infoTag", "Failed to read value.", error.toException())
            }
        })

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                var value: Double = p1.toString().toDouble() / 100
                binding.tvDistanceValue.text =  value.toString()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        binding.swIntruderDetection.setOnCheckedChangeListener{ _, isChecked ->
            if(isChecked){
                binding.tvSwitchStatus.text = "On"
                switchEnable = true

            }else{
                switchEnable = false
                lcdTextRef.setValue("=App is running=")
                cameraRef.setValue("0")
                //relay1Ref.setValue("0")
                cameraImgRef.setValue("None")
                buzzerRef.setValue("0")
                binding.tvSwitchStatus.text = "Off"
            }
        }
        createNotificationChannel()




    }


    private fun createNotificationChannel(){
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            val name = "Illegal Intruder Alert"
            val descText = "There is illegal intruder founded"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channel_ID, name, importance).apply {
                description = descText
            }

            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(){
        val builder = NotificationCompat.Builder(this, channel_ID)
            .setSmallIcon(R.drawable.ic_alert)
            .setContentTitle("Illegal Intruder Alert")
            .setContentText("There is illegal intruder founded")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)){
            notify(notificationID, builder.build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        repeater = repeatFun()
    }


    override fun onStop(){
        super.onStop()
        Log.i("Stop", "Here")
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

        Log.i("local data time", todayDate + " " +currentHour + " " + currentMin)

        if(currentSec % 10 == 0) {
            database = Firebase.database.reference

            Log.i("newTag",currentSec.toString())

            val database = Firebase.database

            val buzzerRef = database.getReference("PI_13__CONTROL").child("buzzer")
            val lcdTextRef = database.getReference("PI_13__CONTROL").child("lcdtxt")
            val cameraRef = database.getReference("PI_13__CONTROL").child("camera")
            //val relay1Ref = database.getReference("PI_13__CONTROL").child("relay1")
            val cameraImgRef = database.getReference("PI_13__CONTROL").child("camera_feedback")


            database.getReference("PI_13__${todayDate}").child(currentHour)
                .child(currentMin + String.format("%02d",currentSec)).get().addOnSuccessListener { snap ->

                    //binding.tvDistanceValue.text = (snap.child("rand1").getValue<String>().toString().toDouble()/100).toString()


//                    var valueSound =  snap.child("rand1").getValue<String>().toString().toDouble()
//                    var valueDistance = binding.tvDistanceValue.text.toString().toDouble()

                    var valueSound =  snap.child("rand1").getValue<String>()?.toDouble()
                    var valueDistance = binding.tvDistanceValue.text.toString().toDouble()

                    if(valueSound != null){
                        binding.tvSoundValue.text = snap.child("rand1").getValue<String>()
                    }else{
                        isNullDetected = true
                        Log.i("newTag","Null Detected : $valueSound")

                        if(isNullDetected){
                            Log.i("newTag","Close and turn on the repeater again")
                            repeater.cancel()
                            isNullDetected = false
                            repeater = repeatFun()
                        }
                    }


                    if(switchEnable){
                        if (valueSound != null) {
                            if(valueSound > 70.00 || valueDistance < 0.5){
                                buzzerRef.setValue("1")
                                lcdTextRef.setValue("==Has Intruder==")
                                //cameraRef.setValue("1")
                                //relay1Ref.setValue("1")
                                sendNotification()
                            }else{
                                lcdTextRef.setValue("=App is running=")
                                //cameraRef.setValue("0")
                                //relay1Ref.setValue("0")
                                buzzerRef.setValue("0")
                                //cameraImgRef.setValue("None")
                            }
                        }
                    }

                    database.getReference("PI_13__CONTROL").get().addOnSuccessListener { snap ->
                        var alarmValue = snap.child("buzzer").getValue<String>().toString().toInt()
                        var messageValue = snap.child("lcdtxt").getValue<String>().toString()
                        if(alarmValue == 1){
                            binding.tvAlarmValue.text = "On"
                        }else{
                            binding.tvAlarmValue.text = "Off"
                        }

                        if(messageValue == "=App is running="){
                            binding.tvMessageValue.text = "None"
                        }else{
                            binding.tvMessageValue.text = messageValue
                        }
                    }
                }

        }else{
            Log.i("newTag",currentSec.toString())

        }


    }
}