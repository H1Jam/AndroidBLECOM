package com.hjam.ezbluelib

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

@SuppressLint("MissingPermission")
object EzBlue {

    private const val mTag = "EzBlue"
    private lateinit var mBtConnectThread: BtConnectThread

    interface BlueCallback {
        fun dataRec(inp: Int)
        fun connected()
        fun disconnected()
    }

    interface BlueParser {
        /**
         * @parseIt A byte receive callback. When a byte appears in the stream this method will be invoked.
         * The method runs on Bluetooth thread. Do not update UI here!
         * @param inp: an Int from input stream.
         * @return ArrayList<Byte> if the packed was parsed otherwise returns null.
         */
        fun parseIt(inp: Int): ArrayList<Byte>?

        /**
         * @bluePackReceived Packet receive callback. When your packed is ready this method will be invoked.
         * The method runs on the UI thread. It is safe to update UI here.
         * @param inp: an ArrayList containing the packed body parsed in the parseIt stage.
         * @return void
         */
        fun bluePackReceived(inp: ArrayList<Byte>?)
    }

    fun getBondedDevices(): Collection<BluetoothDevice> {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return mBluetoothAdapter.bondedDevices
    }

    fun init(device: BluetoothDevice, secure: Boolean, dataCallback: BlueCallback): Boolean {
        return init(device, secure, dataCallback, null)
    }

    fun init(
        device: BluetoothDevice, secure: Boolean,
        dataCallback: BlueCallback, parser: BlueParser?
    ): Boolean {
        if (this::mBtConnectThread.isInitialized) {
            mBtConnectThread.mEnable = false
        }
        mBtConnectThread = BtConnectThread(dataCallback, parser)
        return mBtConnectThread.init(device, secure)
    }

    fun start(): Boolean {
        if (!this::mBtConnectThread.isInitialized) {
            Log.e(mTag, "EzBlue hasn't been initialized!")
            return false
        }
        if (!mBtConnectThread.mLastInitSuccesses) {
            Log.e(mTag, "EzBlue hasn't been properly initialized!")
            return false
        }
        Log.d(mTag, "EzBlue Starts!")
        mBtConnectThread.start()
        return false
    }

    fun stop() {
        if (this::mBtConnectThread.isInitialized) {
            mBtConnectThread.mEnable = false
            Log.e(mTag, "EzBlue stopped!")
            return
        }
        Log.e(mTag, "Failed to Stop! EzBlue hasn't been initialized!")
    }

    /**
     * A method to write an array of byte to output stream. Good for sending an entire data frame.
     * @param buffer : ByteArray
     */
    @Synchronized
    fun write(buffer: ByteArray) {
        if (this::mBtConnectThread.isInitialized) {
            mBtConnectThread.write(buffer)
        }
    }

    /**
     * A method to write a single byte to output stream.
     * use this method when you just want to send a single byte since if trying to send a frame
     * another thread may interrupt and ruin the stream. Thus, try to use @write(buffer) method.
     * @param data : Integer (The first byte will be transferred [0-255])
     */
    @Synchronized
    fun write(data: Int) {
        if (this::mBtConnectThread.isInitialized) {
            mBtConnectThread.write(data)
        }
    }

    class BtConnectThread(
        private var dataCallback: BlueCallback,
        private var blueParser: BlueParser?
    ) : Thread() {
        var mmSocket: BluetoothSocket? = null
        var mSocketType: String? = null
        var mEnable = true
        var mLastInitSuccesses = false
        var mIsRunning = false
        private var mmOutStream: OutputStream? = null
        fun init(device: BluetoothDevice, secure: Boolean): Boolean {
            mLastInitSuccesses = false
            var tmp: BluetoothSocket?
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
                return false
            }
            mmSocket = tmp
            Log.d(mTag, "Socket Type: " + mSocketType + "create() failed")
            mLastInitSuccesses = true
            return true
        }

        override fun run() {
            currentThread().name = "BtConnectThread"
            Log.d(
                mTag, "BEGIN mBtConnectThread SocketType:$mSocketType " +
                        "on ${currentThread().name} ID:${currentThread().id}"
            )
            // try to connect to the BluetoothSocket.
            try {
                Log.d(mTag, "Connect BT socket")
                mmSocket!!.connect()
            } catch (e: IOException) {
                Log.d(mTag, e.message.toString())
                passDisconnected()
                cancel()
                return
            } catch (e2: Exception) {
                e2.printStackTrace()
                Log.e(mTag, e2.message.toString())
                passDisconnected()
                return
            }
            // Start the connected thread
            Log.d(mTag, "Connected BT socket")
            connected(mmSocket, mSocketType)
        }

        private fun connected(
            socket: BluetoothSocket?,
            socketType: String?
        ) {
            passConnected()
            Log.d(mTag, "connected, Socket Type:$socketType")
            // Cancel the thread that completed the connection
            Log.d(mTag, "Get the BluetoothSocket input and output streams: $socketType")
            val mmSocket: BluetoothSocket? = socket
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket!!.inputStream
                tmpOut = socket.outputStream
            } catch (e: IOException) {
                Log.e(mTag, "temp sockets not created")
                e.printStackTrace()
            }
            val mmInStream: InputStream? = tmpIn
            mmOutStream = tmpOut
            sleep(100)
            if (mmInStream != null) {
                Log.d(mTag, "Reading from BT socket!")
                var chr: Int
                mIsRunning = true
                while (mEnable) {
                    if (mmInStream.available() > 0) {
                        if (mmInStream.read().also { chr = it } >= 0) {
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
                mIsRunning = false
                passDisconnected()
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

        fun write(buffer: ByteArray) {
            //  Log.d (mTag, "data write in ${Thread.currentThread().name}:${Thread.currentThread().id}")
            if (mmSocket?.isConnected == true && mmOutStream != null) {
                try {
                    mmOutStream?.write(buffer)
                } catch (e: Exception) {
                    Log.e(mTag, e.message.toString())
                    mEnable = false
                }
            }

        }

        fun write(data: Int) {
            //  Log.d (mTag, "data write in ${Thread.currentThread().name}:${Thread.currentThread().id}")
            if (mmSocket?.isConnected == true && mmOutStream != null) {
                try {
                    mmOutStream?.write(data)
                } catch (e: Exception) {
                    Log.e(mTag, e.message.toString())
                    mEnable = false
                }
            }

        }

        private fun passByte(input: Int) {
            if (blueParser == null) {
                Handler(Looper.getMainLooper()).post {
                    dataCallback.dataRec(input)
                }
                yield()
            } else {
                val result = blueParser?.parseIt(input)
                if (result != null) {
                    Handler(Looper.getMainLooper()).post {
                        blueParser?.bluePackReceived(result)
                    }
                }
            }
        }

        private fun passConnected() {
            Handler(Looper.getMainLooper()).post {
                dataCallback.connected()
            }
            yield()
        }

        private fun passDisconnected() {
            Handler(Looper.getMainLooper()).post {
                dataCallback.disconnected()
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
