package com.example.iotsystem

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
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
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MachineMonitor : AppCompatActivity() {

    private lateinit var binding : ActivityMachineMonitorBinding

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

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_machine_monitor)

        database = Firebase.database.reference
        val database = Firebase.database


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
                relay1Ref.setValue("1")
            }else{
                relay1Ref.setValue("0")
            }
        }

        // get instantly update
        relay1Ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                if(dataSnapshot.getValue<String>() == "1"){
                    binding.tvMachineStatus.text = "On"
                    binding.swMachine.isChecked = true
                }else{
                    binding.tvMachineStatus.text = "Off"
                    binding.swMachine.isChecked = false
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
                    binding.tvMachineTemp.text = snap.child("tempe").getValue<String>() + "°C"
                    binding.tvMachineTemp.textSize = 28F

                    val lcdRef = database.getReference("PI_13__CONTROL").child("lcdtxt")

                    var tempValue = snap.child("tempe").getValue<String>().toString().toDouble()

                    if(tempValue > 500){
                        binding.tvAlertRemark.text = "Over Temperature"
                        binding.imageViewAlert.setImageResource(R.drawable.alert)
                        lcdRef.setValue("Over Temperature")
                    }else if(tempValue > 200 && tempValue <= 500){
                        binding.tvAlertRemark.text = "High Temperature"
                        binding.imageViewAlert.setImageResource(R.drawable.warning)
                        lcdRef.setValue("High Temperature")
                    }else{
                        binding.tvAlertRemark.text = "Normal Temperature"
                        binding.imageViewAlert.setImageResource(R.drawable.bell)
                        lcdRef.setValue("Temperature Ok")
                    }
                }
        }else{
            Log.i("newTag",currentSec.toString())
        }
    }
}