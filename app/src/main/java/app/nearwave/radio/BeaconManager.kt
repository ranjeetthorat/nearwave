package app.nearwave.radio

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import app.nearwave.core.hexToBytes
import app.nearwave.core.toHex
import java.util.UUID

/**
 * A tiny Bluetooth Low Energy "I'm here" signal, used only for the radar.
 *
 * Each phone broadcasts 8 bytes: the first 4 bytes of the channel fingerprint and its 4-byte
 * member id. Other phones on the same channel read the signal strength (RSSI) to estimate
 * how close it is. Nearby Connections does not expose RSSI, which is why this exists.
 */
@SuppressLint("MissingPermission") // Permissions are requested before the radio starts.
class BeaconManager(
    context: Context,
    private val onSighting: (memberId: String, rssi: Int) -> Unit,
) {
    private val bluetooth = context.getSystemService(BluetoothManager::class.java)

    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null
    private var channelPrefix = ByteArray(4)
    private var memberId = ""
    private var running = false
    private var fastScan = false

    val isBluetoothOn: Boolean
        get() = bluetooth?.adapter?.isEnabled == true

    fun start(channelHash: ByteArray, myMemberId: String) {
        stop()
        channelPrefix = channelHash.copyOf(4)
        memberId = myMemberId
        running = true
        startAdvertising()
        startScanning()
    }

    fun stop() {
        running = false
        runCatching { advertiser?.stopAdvertising(advertiseCallback) }
        runCatching { scanner?.stopScan(scanCallback) }
        advertiser = null
        scanner = null
    }

    /** Scan aggressively while the radar is on screen, gently otherwise. */
    fun setFastScan(fast: Boolean) {
        if (fast == fastScan) return
        fastScan = fast
        if (running) {
            runCatching { scanner?.stopScan(scanCallback) }
            startScanning()
        }
    }

    private fun startAdvertising() {
        val adapter = bluetooth?.adapter?.takeIf { it.isEnabled } ?: return
        val adv = adapter.bluetoothLeAdvertiser ?: return
        advertiser = adv
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(false)
            .setTimeout(0)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addServiceData(SERVICE_UUID, channelPrefix + memberId.hexToBytes())
            .build()
        runCatching { adv.startAdvertising(settings, data, advertiseCallback) }
            .onFailure { Log.w(TAG, "advertise failed", it) }
    }

    private fun startScanning() {
        val adapter = bluetooth?.adapter?.takeIf { it.isEnabled } ?: return
        val sc = adapter.bluetoothLeScanner ?: return
        scanner = sc
        val filter = ScanFilter.Builder()
            .setServiceData(
                SERVICE_UUID,
                channelPrefix + ByteArray(4),
                byteArrayOf(-1, -1, -1, -1, 0, 0, 0, 0),
            )
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(if (fastScan) ScanSettings.SCAN_MODE_LOW_LATENCY else ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
        runCatching { sc.startScan(listOf(filter), settings, scanCallback) }
            .onFailure { Log.w(TAG, "scan failed", it) }
    }

    private fun handle(result: ScanResult) {
        val data = result.scanRecord?.getServiceData(SERVICE_UUID) ?: return
        if (data.size < 8) return
        for (i in 0 until 4) if (data[i] != channelPrefix[i]) return
        val id = data.copyOfRange(4, 8).toHex()
        if (id != memberId) onSighting(id, result.rssi)
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            // Some phones allow only one BLE advertiser at a time. The radar then relies on
            // the other phone's beacon; talking and messages are unaffected.
            Log.w(TAG, "beacon advertise failed: $errorCode")
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) = handle(result)
        override fun onBatchScanResults(results: MutableList<ScanResult>) = results.forEach { handle(it) }
        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "beacon scan failed: $errorCode")
        }
    }

    companion object {
        private const val TAG = "BeaconManager"
        private val SERVICE_UUID: ParcelUuid =
            ParcelUuid(UUID.fromString("6e77a7e1-5a1b-4c0e-9d2a-4e3a7761b0c1"))
    }
}
