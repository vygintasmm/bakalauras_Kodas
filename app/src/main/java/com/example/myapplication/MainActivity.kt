package com.example.myapplication

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var pairedDevices: Set<BluetoothDevice>

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.list_view)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter != null) {
            pairedDevices = bluetoothAdapter.bondedDevices
            val deviceList = ArrayList<String>()

            if (pairedDevices.isNotEmpty()) {
                for (device in pairedDevices) {
                    deviceList.add("${device.name} - ${device.address}")
                }
            }

            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceList)
            listView.adapter = adapter

            listView.setOnItemClickListener { _, _, position, _ ->
                val device = pairedDevices.elementAt(position)
                val intent = Intent(this, VoiceRecognitionActivity::class.java)
                intent.putExtra("device", device)
                startActivity(intent)
            }
        }
    }


}