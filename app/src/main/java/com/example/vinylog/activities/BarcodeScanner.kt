package com.example.vinylog.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

class BarcodeScanner : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC)
            .build()
        // [END set_detector_options]

        // [START get_detector]
        val scanner = BarcodeScanning.getClient()

    }
}