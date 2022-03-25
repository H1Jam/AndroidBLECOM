package com.hjam.blecom

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat



class MainActivity : AppCompatActivity() {

    companion object {
        private const val mTag = "BLECOM_LOG"
        private const val BLUETOOTH_PERMISSION_CODE = 101
    }
    lateinit var lbltext: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        lbltext = findViewById(R.id.lbltext01)
        if (checkPermission(Manifest.permission.BLUETOOTH_CONNECT, BLUETOOTH_PERMISSION_CODE)){
            startTheApp()
        }else{
            lbltext.text = getString(R.string.no_permission)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startTheApp(){
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        val pairedDevices: Collection<BluetoothDevice> = mBluetoothAdapter.bondedDevices
        var strDeviceList = ""
        for (device in pairedDevices) {
            strDeviceList += "Name: [${device.name}]  MAC: [${device.address}]\n"
            Log.d(mTag, "Name: [${device.name}]  MAC: [${device.address}]")
        }
        lbltext.text = strDeviceList
    }

    // Function to check and request permission.
    private fun checkPermission(permission: String, requestCode: Int): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                permission
            ) == PackageManager.PERMISSION_DENIED
        ) {
            // Requesting the permission
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BLUETOOTH_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(mTag, "PERMISSION GRANTED")
            } else {
                Log.e(mTag, "Permission Denied!")
            }
        }
    }
}
