package com.falcon.notesapp

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.falcon.notesapp.dao.NoteDatabase
import com.falcon.notesapp.utils.TokenManager
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var noteDatabase: NoteDatabase

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
            logoutUser()
            true
        }
    }

    private fun logoutUser() {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setMessage("Log out of your account ?")
        dialogBuilder.setTitle("Logout")
        dialogBuilder.setPositiveButton("Yes") { dialog, which ->
            CoroutineScope(Dispatchers.IO).launch {
                noteDatabase.clearAllTables()
            }
            tokenManager.deleteToken()
            findNavController().navigate(R.id.action_settingsFragment2_to_firstFragment)
        }
        dialogBuilder.setNegativeButton("Cancel") { dialog, which ->
            dialog.dismiss()
        }
        val dialog = dialogBuilder.create()
        dialog.show()
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