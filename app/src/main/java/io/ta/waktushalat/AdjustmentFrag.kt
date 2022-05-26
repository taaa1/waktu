package io.ta.waktushalat

import android.os.Bundle
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat

class AdjustmentFrag : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.adjustment, rootKey)

        d.forEach { s ->
            findPreference<EditTextPreference>("adj_$s")?.setOnBindEditTextListener {
                it.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            }
            findPreference<EditTextPreference>("adj_$s")?.setOnPreferenceChangeListener { a, b ->
                if (b.toString().isBlank()) (a as EditTextPreference).text = "0"
                return@setOnPreferenceChangeListener b.toString().toIntOrNull() != null
            }
        }
    }
}