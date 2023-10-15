package com.example.remoteserviceconsumer

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.remoteserviceconsumer.ui.theme.RemoteServiceConsumerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    val serviceViewModel: ServiceViewModel by viewModels()
    private var serviceConnection: ServiceConnection? = null
    private var isServiceBound: Boolean = false
    private var requestMessenger: Messenger? = null
    private var receivingMessenger: Messenger? = null
    private val serviceIntent: Intent =
        Intent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RemoteServiceConsumerTheme {

                serviceIntent.component = ComponentName(
                    "com.example.servicedemo",
                    "com.example.servicedemo.remote_binding.RemoteBindingService"
                )
                serviceViewModel._serviceBind.observe(this) { value ->
                    if (value.not()) return@observe
                    establishConnection(serviceIntent)
                }
                serviceViewModel._serviceUnBind.observe(this) { value ->
                    if (value.not()) return@observe
                    removeConnection()
                }
                serviceViewModel._getNumbers.observe(this) { value ->
                    if (value.not()) return@observe
                    CoroutineScope(Dispatchers.Default).launch {
                        while (isActive && isServiceBound) {
                            delay(1000L)
                            val requestNumber =
                                Message.obtain(null, REMOTE_SERVICE_CONSUMER).apply {
                                    replyTo = receivingMessenger
                                }
                            kotlin.runCatching {
                                requestMessenger?.send(requestNumber)
                            }.onFailure {
                                Log.d("remote_exception", it.localizedMessage ?: "")
                            }
                        }
                    }
                }
                ShowCount(serviceViewModel = serviceViewModel)
            }
        }
    }

    private fun isMyServiceRunning(
        context: Context,
        serviceClassName: String,
    ): Boolean {
        val manager =
            context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == serviceClassName }
    }


    private fun establishConnection(serviceIntent: Intent) {
        if (serviceConnection == null) {
            serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(p0: ComponentName?, mBinder: IBinder?) {
                    isServiceBound = true
                    requestMessenger = Messenger(mBinder)
                    receivingMessenger = Messenger(CommunicationReceivingHandler())
                }

                override fun onServiceDisconnected(p0: ComponentName?) {
                    isServiceBound = false
                    receivingMessenger = null
                    requestMessenger = null
                }

            }
        }

        // binding to our service intent.
        bindService(
            serviceIntent,
            serviceConnection!!,
            BIND_AUTO_CREATE
        )


    }

    private fun removeConnection() {
        if (isServiceBound) {
            serviceConnection?.let {
                unbindService(it)
            }
            stopService(serviceIntent)
            serviceViewModel.setRandomValue(0)
            isServiceBound = false
            serviceConnection = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceConnection = null
    }

    private val REMOTE_SERVICE_CONSUMER = 100001


    @SuppressLint("HandlerLeak")
    inner class CommunicationReceivingHandler() : Handler() {

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                REMOTE_SERVICE_CONSUMER -> {
                    val number = msg.arg1
                    if (isServiceBound.not()) return
                    serviceViewModel.setRandomValue(number)
                }
            }
            super.handleMessage(msg)
        }
    }
}


@Composable
private fun ShowCount(serviceViewModel: ServiceViewModel) {
    RemoteServiceConsumerTheme {
        val randomNumber by serviceViewModel.currentRandomValue.observeAsState(initial = 0)
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            val bindService = {
                serviceViewModel.setServiceBind()
            }

            val unBindService = {
                serviceViewModel.setServiceUnBind()
            }


            Button(onClick = { bindService.invoke() }) {
                Text(text = "Bind Service")
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = { unBindService.invoke() }) {
                Text(text = "Un Bind Service")
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Remote Bound Service", color = Color.Green)
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Count is  : $randomNumber")
            Button(onClick = { serviceViewModel.setRandomNumberFetching() }) {
                Text(text = "Fetch Data from other App")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}