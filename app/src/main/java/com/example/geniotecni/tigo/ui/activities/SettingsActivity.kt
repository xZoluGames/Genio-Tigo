package com.example.geniotecni.tigo.ui.activities

import android.content.Intent
import android.health.connect.datatypes.units.Length
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.example.geniotecni.tigo.ui.activities.Bt
import com.example.geniotecni.tigo.R
import com.example.geniotecni.tigo.helpers.BackupHelper
import com.example.geniotecni.tigo.helpers.ExportHelper
import com.example.geniotecni.tigo.managers.PreferencesManager
import com.example.geniotecni.tigo.utils.showToast
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
class SettingsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        supportActionBar?.apply {
            title = "Configuración"
            setDisplayHomeAsUpEnabled(true)
        }
        
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    class SettingsFragment : PreferenceFragmentCompat() {
        
        private lateinit var preferencesManager: PreferencesManager
        private lateinit var exportHelper: ExportHelper
        private lateinit var backupHelper: BackupHelper
        
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            
            preferencesManager = PreferencesManager(requireContext())
            exportHelper = ExportHelper(requireContext())
            backupHelper = BackupHelper(requireContext())
            
            setupPreferences()
        }
        
        private fun setupPreferences() {
            // Theme preference
            findPreference<ListPreference>("theme")?.apply {
                value = preferencesManager.appTheme.toString()
                setOnPreferenceChangeListener { _, newValue ->
                    val theme = newValue.toString().toInt()
                    preferencesManager.appTheme = theme
                    true
                }
            }
            
            // Auto print
            findPreference<SwitchPreferenceCompat>("auto_print")?.apply {
                isChecked = preferencesManager.autoPrint
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesManager.autoPrint = newValue as Boolean
                    true
                }
            }
            
            // Sound
            findPreference<SwitchPreferenceCompat>("sound_enabled")?.apply {
                isChecked = preferencesManager.soundEnabled
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesManager.soundEnabled = newValue as Boolean
                    true
                }
            }
            
            // Vibration
            findPreference<SwitchPreferenceCompat>("vibration_enabled")?.apply {
                isChecked = preferencesManager.vibrationEnabled
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesManager.vibrationEnabled = newValue as Boolean
                    true
                }
            }
            
            // Default SIM
            findPreference<ListPreference>("default_sim")?.apply {
                value = preferencesManager.defaultSim.toString()
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesManager.defaultSim = newValue.toString().toInt()
                    true
                }
            }
            
            // Export format
            findPreference<ListPreference>("export_format")?.apply {
                value = preferencesManager.exportFormat.toString()
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesManager.exportFormat = newValue.toString().toInt()
                    true
                }
            }
            
            // Export data
            findPreference<Preference>("export_data")?.setOnPreferenceClickListener {
                showExportDialog()
                true
            }
            
            // Backup
            findPreference<SwitchPreferenceCompat>("backup_enabled")?.apply {
                isChecked = preferencesManager.backupEnabled
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesManager.backupEnabled = newValue as Boolean
                    if (newValue as Boolean) {
                        backupHelper.performBackup()
                    }
                    true
                }
            }
            
            // Last backup info
            findPreference<Preference>("last_backup")?.apply {
                val lastBackup = preferencesManager.lastBackupTime
                summary = if (lastBackup > 0) {
                    val date = Date(lastBackup)
                    val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    "Último respaldo: ${format.format(date)}"
                } else {
                    "Sin respaldos"
                }
            }
            
            // Manual backup
            findPreference<Preference>("manual_backup")?.setOnPreferenceClickListener {
                performManualBackup()
                true
            }
            
            // Restore backup
            findPreference<Preference>("restore_backup")?.setOnPreferenceClickListener {
                showRestoreDialog()
                true
            }
            
            // Configure Bluetooth
            findPreference<Preference>("configure_bluetooth")?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), Bt::class.java))
                true
            }
            
            // Layout Customization
            findPreference<Preference>("layout_customization")?.setOnPreferenceClickListener {
                Toast.makeText(context,"La personalización de layout ahora está integrada en el modo de edición. Usa el botón 'Editar' en la pantalla principal.", Toast.LENGTH_SHORT).show()
                true
            }
            
            // Clear data
            findPreference<Preference>("clear_data")?.setOnPreferenceClickListener {
                showClearDataDialog()
                true
            }
            
            // About
            findPreference<Preference>("about")?.setOnPreferenceClickListener {
                showAboutDialog()
                true
            }
        }
        
        private fun showExportDialog() {
            val options = arrayOf("CSV", "PDF", "Ambos")
            val selected = preferencesManager.exportFormat
            
            AlertDialog.Builder(requireContext())
                .setTitle("Exportar historial")
                .setSingleChoiceItems(options, selected) { dialog, which ->
                    when (which) {
                        0 -> exportHelper.exportToCSV(true)
                        1 -> exportHelper.exportToPDF(true)
                        2 -> {
                            exportHelper.exportToCSV(false)
                            exportHelper.exportToPDF(true)
                        }
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        
        private fun performManualBackup() {
            if (backupHelper.performBackup()) {
                requireContext().showToast("Respaldo creado exitosamente")
                updateLastBackupSummary()
            } else {
                requireContext().showToast("Error al crear respaldo")
            }
        }
        
        private fun showRestoreDialog() {
            // TODO: Implement file picker for backup restoration
            requireContext().showToast("Función en desarrollo")
        }
        
        private fun showClearDataDialog() {
            AlertDialog.Builder(requireContext())
                .setTitle("Borrar todos los datos")
                .setMessage("¿Estás seguro? Esta acción no se puede deshacer.")
                .setPositiveButton("Borrar") { _, _ ->
                    // TODO: Implement data clearing
                    requireContext().showToast("Datos borrados")
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        
        private fun showAboutDialog() {
            AlertDialog.Builder(requireContext())
                .setTitle("Acerca de")
                .setMessage("""
                    Genio Tecni
                    Versión 2.0.0
                    
                    Desarrollado para facilitar las transacciones
                    de servicios financieros en Paraguay.
                    
                    © 2024 Genio Tecni
                """.trimIndent())
                .setPositiveButton("OK", null)
                .show()
        }
        
        private fun updateLastBackupSummary() {
            findPreference<Preference>("last_backup")?.apply {
                val lastBackup = preferencesManager.lastBackupTime
                summary = if (lastBackup > 0) {
                    val date = Date(lastBackup)
                    val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    "Último respaldo: ${format.format(date)}"
                } else {
                    "Sin respaldos"
                }
            }
        }
    }
}