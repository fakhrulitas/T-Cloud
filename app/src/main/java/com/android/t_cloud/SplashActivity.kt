package com.android.t_cloud

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

class SplashActivity : AppCompatActivity() {

    private lateinit var tvHello: TextView
    private lateinit var tvFlag: TextView
    private lateinit var tvLocationName: TextView
    private lateinit var centerContainer: LinearLayout

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            getRealLocation()
        } else {
            proceedWithLocale()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // UI Setup
        window.statusBarColor = android.graphics.Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        tvHello = findViewById(R.id.tvHello)
        tvFlag = findViewById(R.id.tvFlag)
        tvLocationName = findViewById(R.id.tvLocationName)
        centerContainer = findViewById(R.id.centerContainer)

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (coarse == PackageManager.PERMISSION_GRANTED) {
            getRealLocation()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun getRealLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val lat = location.latitude
                        val lng = location.longitude

                        val geocoder = Geocoder(this, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(lat, lng, 1)

                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            // Ambil nama kota atau kecamatan
                            val city = addr.locality ?: addr.subAdminArea ?: "Unknown Location"
                            val code = addr.countryCode ?: "ID"
                            showHelloAndNavigate(code, city)
                        } else {
                            // Jika geocoder gagal (biasanya butuh internet), tampilkan koordinat mentah
                            showHelloAndNavigate(Locale.getDefault().country, "Lat: $lat, Lng: $lng")
                        }
                    } else {
                        proceedWithLocale()
                    }
                }
                .addOnFailureListener { proceedWithLocale() }
        } catch (e: SecurityException) {
            proceedWithLocale()
        }
    }

    private fun proceedWithLocale() {
        showHelloAndNavigate(Locale.getDefault().country, "Location access denied")
    }

    private fun showHelloAndNavigate(countryCode: String, locationInfo: String) {
        runOnUiThread {
            tvFlag.text = countryToEmoji(countryCode)
            tvHello.text = getHelloByCountry(countryCode)
            tvLocationName.text = locationInfo
            centerContainer.visibility = View.VISIBLE
        }

        centerContainer.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 3000)
    }

    private fun countryToEmoji(countryCode: String): String {
        val code = countryCode.uppercase()
        if (code.length != 2) return "🌐"
        val firstLetter = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
        val secondLetter = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
    }

    private fun getHelloByCountry(code: String): String {
        return when (code.uppercase()) {
            "ID" -> "Halo "
            "JP" -> "Kon'nichiwa "
            "KR" -> "Annyeong "
            "SA" -> "Ahlan "
            "FR" -> "Bonjour "
            else -> "Hello "
        }
    }
}