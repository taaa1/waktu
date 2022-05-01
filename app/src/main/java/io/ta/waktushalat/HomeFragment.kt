package io.ta.waktushalat

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.icu.text.DateFormat.FULL
import android.icu.util.IslamicCalendar
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.preference.PreferenceManager
import com.batoulapps.adhan.*
import com.batoulapps.adhan.data.DateComponents
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.google.android.gms.location.LocationServices
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import okhttp3.*
import java.io.IOException
import java.net.URLEncoder
import java.text.DateFormat
import java.util.*


var dat: Calendar = Calendar.getInstance()
var err = false

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    lateinit var shar: SharedPreferences
    var p = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(requireView()) {
            this.findViewById<Button>(R.id.tangg).setOnClickListener { change() }
            this.findViewById<Button>(R.id.editloc).setOnClickListener { locate() }
            val d = this.findViewById<TextView>(R.id.date)
            d.setOnClickListener {
                p = !p
                if (p) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val s = android.icu.text.DateFormat.getDateInstance(FULL)
                        s.calendar = IslamicCalendar()
                        (it as TextView).text = s.format(IslamicCalendar(dat.time))
                    }
                } else {
                    (it as TextView).text =
                        DateFormat.getDateInstance(DateFormat.FULL).format(dat.time)
                }
            }
            d.text = DateFormat.getDateInstance(DateFormat.FULL).format(dat.time)
        }

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
                    "calcmethod",
                    "MUSLIM_WORLD_LEAGUE"
                )!!
            ).parameters
            val ppp = d.map { shar.getString("adj_$it", "0")!!.toInt() }
            par.adjustments = PrayerAdjustments(ppp[0], ppp[1], ppp[2], ppp[3], ppp[4], ppp[5])
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
            err = false
        } catch (it: Exception) {
            println(it.message)
            err = true
        }
        showErr()
    }

    private fun showErr() {
        if (err) {
            context?.let {
                MaterialAlertDialogBuilder(it)
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
    }

    private val client = OkHttpClient()
    private var sel = false
    private var search = ""

    private fun locate() {
        val m = shar
        val s = LayoutInflater.from(requireContext())
            .inflate(R.layout.coord, requireView().findViewById(R.id.top), false)
        val a = s.findViewById<TextInputEditText>(R.id.latitude)
        val b = s.findViewById<TextInputEditText>(R.id.longitude)
        val c = s.findViewById<SwitchMaterial>(R.id.otomatis)
        val d = s.findViewById<AutoCompleteTextView>(R.id.city_search)
        c.isChecked = m.getBoolean("auto", true)
        a.isEnabled = !c.isChecked
        b.isEnabled = !c.isChecked
        d.isEnabled = !c.isChecked
        c.setOnCheckedChangeListener { _, it ->
            a.isEnabled = !it
            b.isEnabled = !it
            d.isEnabled = !it
        }
        a.setText(m.getString("lat", "0.0"))
        b.setText(m.getString("lon", "0.0"))

        d.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                search = s.toString()
                tph(d, a, b)
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.location))
            .setView(s)
            .setIcon(R.drawable.ic_baseline_location_on_24)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val y = m.edit()
                if (c.isChecked) {
                    y.putBoolean("auto", true)
                } else {
                    if (a.text!!.isNotEmpty() && b.text!!.isNotEmpty()) {
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

    private fun tph(
        d: AutoCompleteTextView,
        a: TextInputEditText,
        b: TextInputEditText
    ) {
        if (!sel) {
            val req = Request.Builder()
                .url("https://photon.komoot.io/api/?q=${URLEncoder.encode(search, "utf-8")}")
                .build()

            client.newCall(req).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        requireActivity().runOnUiThread {
                            if (search == d.text.toString()) {
                                val q =
                                    Klaxon().parse<Map<String, List<JsonObject>>>(response.body!!.string())!!["features"]!!

                                val p = q.map {
                                    val o = it.obj("properties")
                                    "${o?.string("name")}, ${o?.string("country")}"
                                }
                                d.setAdapter(ArrayAdapter(requireContext(), R.layout.city_item, p))
                                d.showDropDown()
                                d.setOnItemClickListener { _, _, i, _ ->
                                    sel = true
                                    val r = q[i].obj("geometry")!!.array<Float>("coordinates")!!
                                    a.setText(r[1].toString())
                                    b.setText(r[0].toString())
                                }
                            }
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {}
            })
        } else {
            sel = false
        }
    }

    private fun change() {
        val dp = MaterialDatePicker.Builder.datePicker().setSelection(dat.timeInMillis).build()
        dp.addOnPositiveButtonClickListener { dat.timeInMillis = it; getL() }
        dp.show(requireActivity().supportFragmentManager, "date")
    }

    @SuppressLint("MissingPermission")
    fun getL() {
        if (shar.getBoolean("auto", true)) {
            LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation.addOnSuccessListener {
                calculate(it)
            }
        } else {
            val l = Location("")
            l.latitude = shar.getString("lat", "0.0")!!.toDouble()
            l.longitude = shar.getString("lon", "0.0")!!.toDouble()
            calculate(l)
        }
    }
}