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