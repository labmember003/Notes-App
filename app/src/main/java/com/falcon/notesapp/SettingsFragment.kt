package com.falcon.notesapp

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.falcon.notesapp.utils.TokenManager
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var tokenManager: TokenManager
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        val preferenceContact = preferenceManager.findPreference<Preference>("contact")
        preferenceContact?.setOnPreferenceClickListener {
            composeEmail("Regarding App " + getString(R.string.app_name))
            true
        }
        val preferenceBugReport = preferenceManager.findPreference<Preference>("bug")
        preferenceBugReport?.setOnPreferenceClickListener {
            composeEmail("Bug Report For " + getString(R.string.app_name))
            true
        }
        val preference = preferenceManager.findPreference<Preference>("libraries")
        preference?.setOnPreferenceClickListener {
            startActivity(Intent(context, OssLicensesMenuActivity::class.java))
            true
        }
        val preferenceLogout = preferenceManager.findPreference<Preference>("logout")
        preferenceLogout?.setOnPreferenceClickListener {
            tokenManager.deleteToken()
            findNavController().navigate(R.id.action_settingsFragment2_to_firstFragment)
//            findNavController().popBackStack()
//            findNavController().popBackStack()
            true
        }
    }
    private fun composeEmail(subject: String) {
        val a = arrayOf("usarcompanion@gmail.com")
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // only email apps should handle this
            putExtra(Intent.EXTRA_EMAIL, a)
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No Mail App Found", Toast.LENGTH_SHORT).show()
        }
    }

}