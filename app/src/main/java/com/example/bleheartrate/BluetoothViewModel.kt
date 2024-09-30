package com.example.bleheartrate

import android.Manifest
import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

// Data class to hold information about scanned devices
data class ScannedDevice(val address: String, val name: String?, val rssi: Int)

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    val bpmLiveData = MutableLiveData<Int>(0)
    val isConnected = MutableLiveData<Boolean>(false)
    val scannedDevices = MutableLiveData<List<ScannedDevice>>(emptyList())

    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothLeScanner: BluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null

    private val discoveredDevices = mutableListOf<ScannedDevice>()

    // Callback for scanning BLE devices
    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val rssi = result.rssi
            if (ActivityCompat.checkSelfPermission(
                    getApplication<Application>(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            Log.d("BLE", "Device found: ${device.name} - ${device.address} - $rssi dBm")

            // Filter devices by name
            if (device.name == "BLEScanner") { // Replace with the desired device name
                val scannedDevice = ScannedDevice(device.address, device.name ?: "Unknown", rssi)
                discoveredDevices.add(scannedDevice)
                scannedDevices.postValue(discoveredDevices.toList())

                // Connect to the device
                bluetoothLeScanner.stopScan(this)
                bluetoothGatt = device.connectGatt(getApplication(), false, gattCallback)
                Log.d("BLE", "Connecting to BLEScanner")
            }
        }
    }

    // Callback for GATT connection and services discovery
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BLE", "Connected to GATT server")
                isConnected.postValue(true)
                if (ActivityCompat.checkSelfPermission(
                        getApplication<Application>(),
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BLE", "Disconnected from GATT server")
                isConnected.postValue(false)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val heartRateService =
                    gatt.getService(UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"))
                val heartRateCharacteristic =
                    heartRateService?.getCharacteristic(UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"))
                if (ActivityCompat.checkSelfPermission(
                        getApplication<Application>(),
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                gatt.setCharacteristicNotification(heartRateCharacteristic, true)

                val descriptor =
                    heartRateCharacteristic?.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val bpm = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1)
            Log.d("BLE", "Heart Rate: $bpm")
            bpmLiveData.postValue(bpm)
        }
    }

    fun startBleScan() {
        viewModelScope.launch(Dispatchers.IO) {
            discoveredDevices.clear()
            scannedDevices.postValue(emptyList())
            if (ActivityCompat.checkSelfPermission(
                    getApplication<Application>(),
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@launch
            }
            bluetoothLeScanner.startScan(leScanCallback)
        }
    }
}