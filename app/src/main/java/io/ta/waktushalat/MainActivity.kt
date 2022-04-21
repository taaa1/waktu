package io.ta.waktushalat

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.add
import androidx.fragment.app.commit
import com.batoulapps.adhan.*
import com.batoulapps.adhan.data.DateComponents
import com.google.android.gms.location.LocationServices
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.*

var dat: Calendar = Calendar.getInstance()
var err = false

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyIfAvailable(this)

        setContentView(R.layout.activity_main)
        title = getString(R.string.app_name)

        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (!isGranted) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_DENIED
                    ) {
                        MaterialAlertDialogBuilder(this)
                            .setTitle(getString(R.string.perm_denied))
                            .setMessage(getString(R.string.perm_denied_desc))
                            .setPositiveButton(getString(R.string.settings)) { _, _ ->
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
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                }
            }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            findViewById<FloatingActionButton>(R.id.tangg).hide()
        }

        findViewById<Button>(R.id.tangg).setOnClickListener { change() }
        findViewById<Button>(R.id.editloc).setOnClickListener { locate() }
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
            var dt =
                arrayOf(tim.fajr, tim.dhuhr, tim.asr, tim.maghrib, tim.isha).map { form.format(it) }
            val bundle = bundleOf("data" to dt)
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<TiFragment>(R.id.main_frag, args = bundle)
            }
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
                .setTitle(getString(R.string.failed))
                .setMessage(getString(R.string.error_load_times))
                .setIcon(R.drawable.ic_baseline_error_24)
                .setNeutralButton(getString(R.string.edit_location)) {d, _ ->
                    d.dismiss()
                    locate()
                }
                .setPositiveButton(getString(R.string.retry)) {d, _ ->
                    d.dismiss()
                    getL()
                }
                .setCancelable(false)
                .show()
        }
    }

    fun locate() {
        val m = getSharedPreferences("Waktu Shalat", MODE_PRIVATE)
        val s = LayoutInflater.from(this).inflate(R.layout.coord, findViewById(R.id.top), false)
        val a = s.findViewById<EditText>(R.id.latitude)
        val b = s.findViewById<EditText>(R.id.Longitude)
        val c = s.findViewById<SwitchMaterial>(R.id.otomatis)
        c.isChecked = m.getBoolean("auto", true)
        a.isEnabled = !c.isChecked
        b.isEnabled = !c.isChecked
        c.setOnCheckedChangeListener { _, d ->
            a.isEnabled = !d
            b.isEnabled = !d
        }
        a.setText(m.getString("lat", "0.0"))
        b.setText(m.getString("lon", "0.0"))
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.location))
            .setView(s)
            .setIcon(R.drawable.ic_baseline_location_on_24)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val y = m.edit()
                if(c.isChecked) {
                    y.putBoolean("auto", true)
                } else {
                    if (a.text.isNotEmpty() && b.text.isNotEmpty()) {
                        y.putBoolean("auto", false)
                        y.putString("lat", a.text.toString())
                        y.putString("lon", b.text.toString())
                    }
                }
                y.apply()
                getL()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    @SuppressLint("NewApi")
    fun change() {
        val dp = DatePickerDialog(this)
        dp.updateDate(
            dat.get(Calendar.YEAR),
            dat.get(Calendar.MONTH),
            dat.get(Calendar.DAY_OF_MONTH)
        )
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

