package com.example.bleheartrate

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    private val viewModel: BluetoothViewModel by viewModels()

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            viewModel.startBleScan()
        } else {
            // Handle the case where permissions are not granted
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BLEHeartRateApp(viewModel)
        }

        if (!hasPermissions()) {
            requestPermissions()
        } else {
            viewModel.startBleScan()
        }
    }

    private fun hasPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return permissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        requestPermissionsLauncher.launch(permissions)
    }
}

@Composable
fun BLEHeartRateApp(viewModel: BluetoothViewModel = viewModel()) {
    val bpm by viewModel.bpmLiveData.observeAsState(initial = 0)
    val isConnected by viewModel.isConnected.observeAsState(initial = false)
    val devices by viewModel.scannedDevices.observeAsState(initial = emptyList())

    HeartRateScreen(
        bpm = bpm,
        onScanClick = { viewModel.startBleScan() },
        isConnected = isConnected,
        scannedDevices = devices
    )
}

@Composable
fun HeartRateScreen(bpm: Int, onScanClick: () -> Unit, isConnected: Boolean, scannedDevices: List<ScannedDevice>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Start Scanning Button
        Button(
            onClick = { onScanClick() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(text = "Start Scanning", fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Connected Status
        Text(
            text = if (isConnected) "Connected" else "Not Connected",
            fontSize = 16.sp,
            color = if (isConnected) Color.Green else Color.Red,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Display Heart Rate
        Text(
            text = "$bpm bpm",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // List of scanned devices
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(scannedDevices) { device ->
                Text(
                    text = "${device.address} ${device.name} ${device.rssi}dBm",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}