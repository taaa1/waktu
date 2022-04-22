package io.ta.waktushalat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationBarView
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
                    supportFragmentManager.beginTransaction().replace(R.id.mainfr, HomeFragment())
                        .commit() //getL()
                }
                else -> {
                    requestPermissionLauncher.launch(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                }
            }

        findViewById<NavigationBarView>(R.id.bottom_navigation).setOnItemSelectedListener { item ->
            val p = supportFragmentManager.beginTransaction()
            val frag = R.id.mainfr
            when (item.itemId) {
                R.id.homeb -> {
                    p.replace(frag, HomeFragment())
                }

                R.id.settingsb -> {
                    p.replace(frag, SettingsFragment())
                }
            }
            p.commit()

            return@setOnItemSelectedListener true
        }
    }
}