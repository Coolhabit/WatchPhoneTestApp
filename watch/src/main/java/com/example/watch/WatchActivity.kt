package com.example.watch

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.nio.ByteBuffer

class WatchActivity : ComponentActivity(), SensorEventListener {

    companion object {
        private const val BODY_SENSOR_PERMISSION_REQUEST_CODE = 1
        const val COMBINED_SENSOR_PATH = "combinedDataPath"
        private var counter = 0
    }

    private lateinit var sensorManager: SensorManager
    private val isTransmitting = mutableStateOf(false)
    private var currentChannel: ChannelClient.Channel? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        checkAndRequestPermissions()

        setContent {
            WearOSApp(
                onStartStopClicked = { toggleTransmission() },
                isTransmitting = isTransmitting.value
            )
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            BODY_SENSOR_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                } else {
                    Toast.makeText(this, "No permission", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isTransmitting.value) return

        counter++
        if (counter >= 5) {
            counter = 0
            lifecycleScope.launch {
                when (event.sensor.type) {
                    Sensor.TYPE_GYROSCOPE -> {
                        sendData(Clock.System.now().toEpochMilliseconds().longToByteArray())
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}


    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BODY_SENSORS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.BODY_SENSORS),
                BODY_SENSOR_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun toggleTransmission() {
        isTransmitting.value = !isTransmitting.value
        if (isTransmitting.value) {
            registerSensors()
            openChannel()
        } else {
            sensorManager.unregisterListener(this)
            closeChannel()
        }
    }

    private fun registerSensors() {
        sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)?.also { heartRateSensor ->
            sensorManager.registerListener(this, heartRateSensor, 19000, 0)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also { gyroscopeSensor ->
            sensorManager.registerListener(this, gyroscopeSensor, 19000, 0)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometerSensor ->
            sensorManager.registerListener(this, accelerometerSensor, 19000, 0)
        }
    }

    private fun openChannel() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val nodes = Tasks.await(Wearable.getNodeClient(applicationContext).connectedNodes)
                val node = nodes.firstOrNull()
                node?.let { targetNode ->
                    val channelClient = Wearable.getChannelClient(applicationContext)
                    currentChannel = Tasks.await(channelClient.openChannel(targetNode.id, COMBINED_SENSOR_PATH))
                    Log.d("WATCH", "Channel opened")
                }
            } catch (e: Exception) {
                Log.e("WATCH", "Error opening channel: ${e.message}")
            }
        }
    }

    private fun closeChannel() {
        currentChannel?.let { channel ->
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    Wearable.getChannelClient(applicationContext).close(channel)
                    Log.d("WATCH", "Channel closed")
                } catch (e: Exception) {
                    Log.e("WATCH", "Error closing channel: ${e.message}")
                } finally {
                    currentChannel = null
                }
            }
        }
    }

    private fun sendData(data: ByteArray) {
        currentChannel?.let { channel ->
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val outputStream = Tasks.await(Wearable.getChannelClient(applicationContext).getOutputStream(channel))
                    outputStream.use {
                        it.write(data)
                        it.flush()
                    }
                    Log.d("WATCH", "Data sent")
                } catch (e: Exception) {
                    Log.e("WATCH", "Error sending data: ${e.message}")
                }
            }
        }
    }

    private fun Long.longToByteArray(): ByteArray {
        return ByteBuffer.allocate(8).putLong(this).array() // 8 bytes for a long
    }

    @Composable
    fun WearOSApp(onStartStopClicked: () -> Unit, isTransmitting: Boolean) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Button(onClick = { onStartStopClicked() }) {
                    Text(text = if (isTransmitting) "Stop" else "Start")
                }
            }
        }
    }
}