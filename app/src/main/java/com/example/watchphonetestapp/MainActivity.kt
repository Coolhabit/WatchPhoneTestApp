package com.example.watchphonetestapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.example.watchphonetestapp.ui.theme.WatchPhoneTestAppTheme
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.nio.ByteBuffer

class MainActivity : ComponentActivity() {
    companion object {
        const val COMBINED_SENSOR_PATH = "combinedDataPath"
    }

    private val channelCallback = object : ChannelClient.ChannelCallback() {
        override fun onChannelOpened(channel: ChannelClient.Channel) {
            Log.d("MainActivity", "Channel opened: ${channel.path}")
            if (channel.path == COMBINED_SENSOR_PATH) {
                readChannelData(channel)
            }
        }

        override fun onChannelClosed(
            channel: ChannelClient.Channel,
            closeReason: Int,
            appSpecificErrorCode: Int
        ) {
            Log.d("MainActivity", "Channel closed: ${channel.path}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Wearable.getChannelClient(this).registerChannelCallback(channelCallback)
        setContent {
            WatchPhoneTestAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Wearable.getChannelClient(this).unregisterChannelCallback(channelCallback)
    }

    private fun readChannelData(channel: ChannelClient.Channel) {
        val inputStreamClient = Wearable.getChannelClient(applicationContext)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = Tasks.await(inputStreamClient.getInputStream(channel))
                inputStream.use { it ->
                    val data = it.readBytes()
                    val timestamp = byteArrayToTimestamp(data)
                    Log.d("WATCH", "Data received with timestamp=${Instant.fromEpochMilliseconds(timestamp)}, current=${Clock.System.now()}")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error reading channel data: ${e.message}")
            }
        }
    }
}

private fun byteArrayToTimestamp(data: ByteArray): Long {
    val buffer = ByteBuffer.wrap(data)
    return buffer.long
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WatchPhoneTestAppTheme {
        Greeting("Android")
    }
}