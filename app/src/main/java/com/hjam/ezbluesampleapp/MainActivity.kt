package com.hjam.ezbluesampleapp

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.hjam.ezbluelib.EzBlue

//Todo: Add Unit tests!
@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity(), EzBlue.BlueCallback {

    companion object {
        private const val mTag = "EZBlueSample_LOG"
        private const val BLUETOOTH_PERMISSION_CODE = 101
    }

    private lateinit var mLblText: TextView
    private lateinit var mBtnSend: Button
    private lateinit var mBtnConnect: Button
    private lateinit var mBtnDisconnect: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mLblText = findViewById(R.id.lbl_text)
        mBtnSend = findViewById(R.id.btn_send)
        mBtnConnect = findViewById(R.id.btn_connect)
        mBtnDisconnect = findViewById(R.id.btn_disconnect)
        mBtnDisconnect.isEnabled = false
        mBtnSend.isEnabled = false
        if (checkPermission(Manifest.permission.BLUETOOTH_CONNECT, BLUETOOTH_PERMISSION_CODE)) {
            startTheApp()
        } else {
            mBtnConnect.isEnabled = false
            mLblText.text = getString(R.string.no_permission)
        }
    }

    private fun startTheApp() {
        setListeners()
    }

    private fun showDevList() {
        val mmDevList = EzBlue.getBondedDevices()
        showDeviceListDialog(mmDevList.toTypedArray())
    }

    private fun connectToDev(dev: BluetoothDevice) {
        EzBlue.init(dev, true, this)
        EzBlue.start()
    }

    var mCounter: Int = 0
    private val mBytes: ByteArray = ByteArray(1)

    private fun setListeners() {
        mBtnSend.setOnClickListener {
            mCounter++
            if (mCounter > 255) {
                mCounter = 1
            }
            // a byte array showcase:
            mBytes[0] = mCounter.toByte()
            EzBlue.write(mBytes)
            // or just use the above line for single byte transfer:
            //EzBlue.write(counterd)
        }
        mBtnConnect.isEnabled = true
        mBtnConnect.setOnClickListener {
            showDevList()
        }
        mBtnDisconnect.setOnClickListener {
            EzBlue.stop()
        }
    }

    override fun dataRec(inp: Int) {
        mLblText.text = inp.toString()
    }

    override fun connected() {
        Log.d(mTag, "connected!")
        setText("Connected!")
        mBtnDisconnect.isEnabled = true
        mBtnSend.isEnabled = true
    }

    override fun disconnected() {
        Log.d(mTag, "connectionFailed!")
        setText("Disconnected!")
        mBtnConnect.isEnabled = true
        mBtnDisconnect.isEnabled = false
        mBtnSend.isEnabled = false
    }

    override fun onDestroy() {
        EzBlue.stop()
        super.onDestroy()
    }

    private fun setText(str: String) {
        mLblText.text = str
    }

    private fun showDeviceListDialog(devices: Array<BluetoothDevice>) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose an animal")
        val mmListData: Array<String> = devices.map { it.name }.toTypedArray()
        builder.setItems(mmListData) { _, which ->
            connectToDevFromDialog(devices[which])
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun connectToDevFromDialog(dev: BluetoothDevice) {
        Log.e(mTag, dev.name + ":" + dev.address)
        connectToDev(dev)
        setText("Connecting...")
        mBtnConnect.isEnabled = false
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
                startTheApp()
            } else {
                Log.e(mTag, "Permission Denied!")
            }
        }
    }

}
