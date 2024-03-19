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
import com.example.watchphonetestapp.ui.theme.WatchPhoneTestAppTheme
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.nio.ByteBuffer

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {
    companion object {
        const val COMBINED_SENSOR_PATH = "combinedDataPath"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Wearable.getMessageClient(this).addListener(this)
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

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            COMBINED_SENSOR_PATH -> {
                val data = messageEvent.data
                val timestamp = byteArrayToTomestamp(data)
                Log.d("WATCH", "Data received with timestamp=${Instant.fromEpochMilliseconds(timestamp)}, current=${Clock.System.now()}")
            }
        }
    }
}

private fun byteArrayToTomestamp(data: ByteArray): Long {
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