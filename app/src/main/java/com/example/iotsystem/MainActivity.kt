package com.example.iotsystem

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.example.iotsystem.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.cvTempHumid.setOnClickListener(){
            val intent = Intent(this@MainActivity, Temperature_Humidity::class.java)
            startActivity(intent)
        }

        binding.cvIntruder.setOnClickListener(){
            val intent = Intent(this@MainActivity, IntruderDetection::class.java)
            startActivity(intent)
        }

        binding.cvFire.setOnClickListener(){
            val intent = Intent(this@MainActivity, MainFireDetection::class.java)
            startActivity(intent)
        }

        binding.cvLight.setOnClickListener(){
            val intent = Intent(this@MainActivity, CorridorLightControl::class.java)
            startActivity(intent)
        }

        binding.cvMachine.setOnClickListener(){
            val intent = Intent(this@MainActivity, MachineMonitor::class.java)
            startActivity(intent)
        }

    }
}