package com.hjam.ezblue

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

//Todo: separate packages and add more documents.
class MainActivity : AppCompatActivity(), EzBlue.BlueCallback {
    companion object {
        private const val mTag = "BLECOM_LOG"
        private const val BLUETOOTH_PERMISSION_CODE = 101
    }


    lateinit var mLbltext: TextView
    lateinit var mBtnSend: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mLbltext = findViewById(R.id.lbl_text)
        mBtnSend = findViewById(R.id.btn_send)
        if (checkPermission(Manifest.permission.BLUETOOTH_CONNECT, BLUETOOTH_PERMISSION_CODE)) {
            startTheApp()
        } else {
            mLbltext.text = getString(R.string.no_permission)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startTheApp() {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        val pairedDevices: Collection<BluetoothDevice> = mBluetoothAdapter.bondedDevices
        var mmDevice: BluetoothDevice? = null
        for (device in pairedDevices) {
            if (device.name.equals("ESP32testB")) {
                mmDevice = device
            }
            Log.d(mTag, "Name: [${device.name}]  MAC: [${device.address}]")
        }
        if (mmDevice != null) {
            EzBlue.init (mmDevice, true,this)
            EzBlue.start()
        }
        setListeners()
    }

    var mCounter: Int = 0
    private fun setListeners() {
        mBtnSend.setOnClickListener {
            mCounter++
            if (mCounter > 255) {
                mCounter = 1
            }
            // a byte array showcase:
            byts[0] = mCounter.toByte()
            EzBlue.write(byts)
            // or just use the above line for single byte transfer:
            //EzBlue.write(counterd)
        }
    }

    val byts: ByteArray = ByteArray(1)
    override fun dataRec(inp: Int) {
        // byts[0] =inp.toByte()
        // mBtConnectThread.write(byts)
        mLbltext.text = inp.toString()
    }

    override fun connected() {
        setText("Connected!")
    }

    override fun connectionFailed() {
        setText("Disconnected!")
    }

    override fun onDestroy() {
        EzBlue.stop()
        super.onDestroy()
    }

    private fun setText(str: String) {
        mLbltext.text = str
    }

    class Test1(
        private val tv: TextView,
        private var mHandler: Handler,
        private var dataCallback: EzBlue.BlueCallback
    ) : Thread() {

        override fun run() {
            currentThread().name = "Test1Thread"
            repeat(250000) {
               // dataCallback.dataRec(it)
                sleep(2000)
                yield()
            }
        }
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
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
