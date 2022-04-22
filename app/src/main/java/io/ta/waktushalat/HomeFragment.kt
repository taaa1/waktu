package io.ta.waktushalat

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.SharedPreferences
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.preference.PreferenceManager
import com.batoulapps.adhan.*
import com.batoulapps.adhan.data.DateComponents
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.DateFormat
import java.util.*

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    lateinit var shar: SharedPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            requireView().findViewById<Button>(R.id.tangg).visibility = View.GONE
        }

        requireView().findViewById<Button>(R.id.tangg).setOnClickListener { change() }
        requireView().findViewById<Button>(R.id.editloc).setOnClickListener { locate() }

        shar = PreferenceManager.getDefaultSharedPreferences(requireContext())

        getL()
    }

    private fun calculate(it: Location) {
        try {
            val cor = Coordinates(it.latitude, it.longitude)
            requireView().findViewById<TextView>(R.id.loc).text =
                getString(R.string.lat_lon, it.latitude, it.longitude)
            println(shar.getString("calcmethod", "MUSLIM_WORLD_LEAGUE")!!)
            val par = CalculationMethod.valueOf(
                shar.getString(
                    "calc_method",
                    "MUSLIM_WORLD_LEAGUE"
                )!!
            ).parameters
            par.adjustments = PrayerAdjustments(2, 0, 2, 2, 2, 2)
            par.madhab = Madhab.SHAFI
            val tim = PrayerTimes(cor, DateComponents.from(dat.time), par)
            val form = DateFormat.getTimeInstance(DateFormat.SHORT)
            form.timeZone = TimeZone.getDefault()
            val dt =
                arrayOf(
                    tim.fajr,
                    tim.sunrise,
                    tim.dhuhr,
                    tim.asr,
                    tim.maghrib,
                    tim.isha
                ).map { form.format(it) }
            val bundle = bundleOf("data" to dt)
            requireActivity().supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<TiFragment>(R.id.main_frag, args = bundle)
            }
            requireView().findViewById<TextView>(R.id.date).text =
                DateFormat.getDateInstance(DateFormat.FULL).format(dat.time)
            err = false
        } catch (it: Exception) {
            println(it.message)
            err = true
        }
        showErr()
    }

    private fun showErr() {
        if (err) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.failed))
                .setMessage(getString(R.string.error_load_times))
                .setIcon(R.drawable.ic_baseline_error_24)
                .setNeutralButton(getString(R.string.edit_location)) { d, _ ->
                    d.dismiss()
                    locate()
                }
                .setPositiveButton(getString(R.string.retry)) { d, _ ->
                    d.dismiss()
                    getL()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun locate() {
        val m =
            requireContext().getSharedPreferences("Waktu Shalat", AppCompatActivity.MODE_PRIVATE)
        val s = LayoutInflater.from(requireContext())
            .inflate(R.layout.coord, requireView().findViewById(R.id.top), false)
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
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.location))
            .setView(s)
            .setIcon(R.drawable.ic_baseline_location_on_24)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val y = m.edit()
                if (c.isChecked) {
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
        val dp = DatePickerDialog(requireContext())
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
        val s =
            requireContext().getSharedPreferences("Waktu Shalat", AppCompatActivity.MODE_PRIVATE)
        if (s.getBoolean("auto", true)) {
            LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation.addOnSuccessListener {
                calculate(it)
            }
        } else {
            val l = Location("")
            l.latitude = s.getString("lat", "0.0")!!.toDouble()
            l.longitude = s.getString("lon", "0.0")!!.toDouble()
            calculate(l)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            HomeFragment().apply {}
    }
}