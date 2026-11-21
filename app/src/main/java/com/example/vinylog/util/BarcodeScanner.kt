package com.example.vinylog.util

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class BarcodeScanner(private val activity: ComponentActivity) {
    interface ScanResultListener {
        fun onScanResult(barcode: String)
        fun onScanCancelled()
        fun onPermissionDenied()
    }

    private var scanResultListener: ScanResultListener? = null

    private val barcodeLauncher: ActivityResultLauncher<ScanOptions> = activity.registerForActivityResult(
        ScanContract()
    ) { result ->
        val contents = result.contents
        if (!contents.isNullOrBlank()) {
            scanResultListener?.onScanResult(contents)
        } else {
            scanResultListener?.onScanCancelled()
        }
    }

    private val permissionLauncher: ActivityResultLauncher<String> = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startScanning()
        } else {
            scanResultListener?.onPermissionDenied()
            Toast.makeText(activity, "Camera permission is needed to scan a barcode", Toast.LENGTH_SHORT).show()
        }
    }

    fun setScanResultListener(listener: ScanResultListener?) {
        this.scanResultListener = listener
    }

    fun scanBarcode() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startScanning()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startScanning() {
        val options = ScanOptions().apply {
            setPrompt("Line the barcode up inside the frame")
            setBeepEnabled(true)
            // Returning the camera bitmap with the result crashes or drops the scan on some devices.
            setBarcodeImageEnabled(false)
            setOrientationLocked(false)
            setDesiredBarcodeFormats(
                listOf(
                    ScanOptions.UPC_A,
                    ScanOptions.UPC_E,
                    ScanOptions.EAN_13,
                    ScanOptions.EAN_8,
                    ScanOptions.CODE_128,
                    ScanOptions.QR_CODE,
                )
            )
        }
        barcodeLauncher.launch(options)
    }
}
