package com.hjam.ezblue

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

object EzBlue {

    private const val mTag = "BLECOM_LOG"

    interface BlueCallback {
        fun dataRec(inp: Int)
        fun connected()
        fun connectionFailed()
    }

    @SuppressLint("MissingPermission")
    class BtConnectThread(private var dataCallback: BlueCallback) : Thread() {
        var mmDevice: BluetoothDevice? = null
        var mmSocket: BluetoothSocket? = null
        var mSocketType: String? = null
        var mEnable = true
        private var mmOutStream: OutputStream? = null
        fun init(device: BluetoothDevice, secure: Boolean) {
            mmDevice = device
            var tmp: BluetoothSocket? = null
            mSocketType = if (secure) "Secure" else "Insecure"

            // SPP Devices
            val mSPPUUID = UUID
                .fromString("00001101-0000-1000-8000-00805F9B34FB")

            // Get the BluetoothSocket of the given BluetoothDevice.
            try {
                tmp = if (secure) {
                    device.createRfcommSocketToServiceRecord(mSPPUUID)
                } else {
                    device.createInsecureRfcommSocketToServiceRecord(mSPPUUID)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            mmSocket = tmp
            logSocketUuids(mmSocket!!, "BT socket")
            Log.d(mTag, "Socket Type: " + mSocketType + "create() failed")
        }

        /**
         * Log supported UUIDs of the specified BluetoothSocket
         * @param socket Socket to log
         * @param msg Message to prepend the UUIDs
         */
        private fun logSocketUuids(socket: BluetoothSocket, msg: String) {
            val message = StringBuilder(msg)
            message.append(" - UUIDs:")
            val uuids = socket.remoteDevice.uuids
            if (uuids != null) {
                for (uuid in uuids) {
                    message.append(uuid.uuid.toString()).append(",")
                }
            } else {
                message.append("NONE (Invalid BT implementation)")
            }
            Log.d(mTag, "logSocketUuids: [$message]")
        }

        override fun run() {
            Log.d(mTag, "BEGIN mBtConnectThread SocketType:$mSocketType")
            // try to connect to the BluetoothSocket.
            try {
                Log.d(mTag, "Connect BT socket")
                mmSocket!!.connect()
            } catch (e: IOException) {
                Log.d(mTag, e.message.toString())
                passFailed()
                cancel()
                return
            } catch (e2: Exception) {
                e2.printStackTrace()
                Log.e(mTag, e2.message.toString())
                passFailed()
                return
            }
            // Start the connected thread
            Log.d(mTag, "Connected BT socket")
            connected(mmSocket, mmDevice, mSocketType)
        }

        private fun connected(
            socket: BluetoothSocket?,
            device: BluetoothDevice?,
            socketType: String?
        ) {
            Log.d(mTag, "connected, Socket Type:$socketType")
            // Cancel the thread that completed the connection
            Log.d(mTag, "Get the BluetoothSocket input and output streams: $socketType")
            val mmSocket: BluetoothSocket? = socket
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket!!.inputStream
                tmpOut = socket!!.outputStream
            } catch (e: IOException) {
                Log.e(mTag, "temp sockets not created")
                e.printStackTrace()
            }
            val mmInStream: InputStream? = tmpIn
            mmOutStream = tmpOut
            sleep(100)
            if (mmInStream != null) {
                Log.d(mTag, "Reading to BT socket!")
                var chr: Int
                while (mEnable) {
                    if (mmInStream.available() > 0) {
                        if (mmInStream.read().also { chr = it } > 0) {
                            passByte(chr)
                        } else {
                            Log.d(mTag, "End of the string")
                            // stream finished - break loop
                            break
                        }
                    } else {
                        sleep(0, 100)
                    }
                }
            }
            try {
                Log.d(mTag, "Closing BT socket")
                mmSocket!!.close()
            } catch (e: IOException) {
                Log.d(mTag, "Closing BT socket Exception!")
                e.printStackTrace()
            }
            Log.d(mTag, "BT socket Finished!")
        }

        //  @Synchronized
        fun write(buffer: ByteArray) {
            //  Log.d (mTag, "data write in ${Thread.currentThread().name}:${Thread.currentThread().id}")
            if (mmOutStream != null) {
                mmOutStream?.write(buffer)
            }
        }

        private fun passByte(input: Int) {
            Handler(Looper.getMainLooper()).post {
                dataCallback.dataRec(input)
            }
            yield()
        }

        private fun passFailed() {
            Handler(Looper.getMainLooper()).post {
                dataCallback.connectionFailed()
            }
            yield()
        }


        @Synchronized
        fun cancel() {
            try {
                //  log.log(Level.INFO, "Closing BT socket")
                mmSocket!!.close()
            } catch (e: IOException) {
                //   log.log(Level.SEVERE, e.message)
            }
        }

    }


}