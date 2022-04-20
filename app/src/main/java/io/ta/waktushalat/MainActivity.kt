package io.ta.waktushalat

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.batoulapps.adhan.*
import com.batoulapps.adhan.data.DateComponents
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.Exception

var dat: Calendar = Calendar.getInstance()
var err = false

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "Waktu Shalat"
        setSupportActionBar(findViewById(R.id.topAppBar))

        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (!isGranted){
                    if(ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_DENIED) {
                        MaterialAlertDialogBuilder(this)
                            .setTitle("Izin tidak diberikan")
                            .setMessage("Berikan izin lokasi agar aplikasi berjalan dengan benar")
                            .setPositiveButton("Pengaturan") { _, _ ->
                                val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                i.data = Uri.fromParts("package", packageName, null)
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                                i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                                startActivity(i)
                            }
                            .setCancelable(false)
                            .show()

                        return@registerForActivityResult
                    }
                }
                val inte = intent
                finish()
                startActivity(inte)
            }

            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED -> {
                    dat = Calendar.getInstance()
                    getL()
                }
                else -> {
                    requestPermissionLauncher.launch(
                        Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            findViewById<Button>(R.id.tangg).visibility = View.GONE
        }
    }

    private fun calculate(it: Location) {
        try {
            val cor = Coordinates(it.latitude, it.longitude)
            findViewById<TextView>(R.id.loc).text =
                it.latitude.toString() + ", " + it.longitude.toString()
            val par = CalculationMethod.SINGAPORE.parameters
            par.adjustments = PrayerAdjustments(2, 0, 2, 2, 2, 2)
            par.madhab = Madhab.SHAFI
            val tim = PrayerTimes(cor, DateComponents.from(dat.time), par)
            val form = SimpleDateFormat("hh:mm a")
            form.timeZone = TimeZone.getDefault()
            findViewById<TextView>(R.id.Subuh).text = form.format(tim.fajr)
            findViewById<TextView>(R.id.Dzuhur).text = form.format(tim.dhuhr)
            findViewById<TextView>(R.id.Ashar).text = form.format(tim.asr)
            findViewById<TextView>(R.id.Maghrib).text = form.format(tim.maghrib)
            findViewById<TextView>(R.id.Isya).text = form.format(tim.isha)
            findViewById<TextView>(R.id.date).text =
                SimpleDateFormat("E, dd MMM y").format(dat.time)
            err = false
        } catch(it: Exception) {
            err = true
        }
        showErr()
    }

    fun showErr() {
        if(err) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Gagal")
                .setMessage("Terjadi kesalahan saat memuat jadwal shalat")
                .setIcon(R.drawable.ic_baseline_error_24)
                .setNeutralButton("Ubah lokasi") {d, _ ->
                    d.dismiss()
                    locate(findViewById(R.id.top))
                }
                .setPositiveButton("Coba lagi") {d, _ ->
                    d.dismiss()
                    getL()
                }
                .setCancelable(false)
                .show()
        }
    }

    fun locate(v: View) {
        val m = getSharedPreferences("Waktu Shalat", MODE_PRIVATE)
        val s = LayoutInflater.from(this).inflate(R.layout.coord, findViewById(R.id.top), false)
        val a = s.findViewById<EditText>(R.id.latitude)
        val b = s.findViewById<EditText>(R.id.Longitude)
        val c = s.findViewById<Switch>(R.id.otomatis)
        c.isChecked = m.getBoolean("auto", true)
        a.isEnabled = !c.isChecked
        b.isEnabled = !c.isChecked
        c.setOnCheckedChangeListener() { _, d ->
            a.isEnabled = !d
            b.isEnabled = !d
        }
        a.setText(m.getString("lat", "0.0"))
        b.setText(m.getString("lon", "0.0"))
        MaterialAlertDialogBuilder(this)
            .setTitle("Lokasi")
            .setView(s)
            .setIcon(R.drawable.ic_baseline_location_on_24)
            .setPositiveButton("Ubah") { _, _ ->
                val y = m.edit()
                if(c.isChecked) {
                    y.putBoolean("auto", true)
                } else {
                    if(a.text.isNotEmpty() && b.text.isNotEmpty()) {
                        y.putBoolean("auto", false)
                        y.putString("lat", a.text.toString())
                        y.putString("lon", b.text.toString())
                    }
                }
                y.apply()
                getL()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    @SuppressLint("NewApi")
    fun change(v: View) {
        val dp = DatePickerDialog(this)
        dp.updateDate(dat.get(Calendar.YEAR), dat.get(Calendar.MONTH), dat.get(Calendar.DAY_OF_MONTH))
        dp.show()
        dp.setOnDateSetListener(
        fun(_, y, m, d) {
            dat.set(y, m, d)
            getL()
        })
    }

    @SuppressLint("MissingPermission")
    fun getL() {
        val s = getSharedPreferences("Waktu Shalat", MODE_PRIVATE)
        if(s.getBoolean("auto", true)) {
            LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener {
                calculate(it)
            }
        } else {
            val l = Location("")
            l.latitude = s.getString("lat", "0.0")!!.toDouble()
            l.longitude = s.getString("lon", "0.0")!!.toDouble()
            calculate(l)
        }
    }
}

