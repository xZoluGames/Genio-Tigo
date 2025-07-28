package com.example.geniotecni.tigo.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.example.geniotecni.tigo.R
import com.example.geniotecni.tigo.helpers.BackupHelper
import com.example.geniotecni.tigo.helpers.ExportHelper
import com.example.geniotecni.tigo.managers.PreferencesManager
import com.example.geniotecni.tigo.managers.PrintDataManager
import com.example.geniotecni.tigo.ui.viewmodels.SettingsViewModel
import com.example.geniotecni.tigo.utils.BaseActivity
import com.example.geniotecni.tigo.utils.showToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class SettingsActivity : BaseActivity() {
    
    override val tag = "SettingsActivity"
    
    // ViewModel with dependency injection
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Configuración"
        }

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private lateinit var preferencesManager: PreferencesManager
        private lateinit var exportHelper: ExportHelper
        private lateinit var backupHelper: BackupHelper
        private lateinit var printDataManager: PrintDataManager

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            preferencesManager = PreferencesManager(requireContext())
            exportHelper = ExportHelper(requireContext())
            backupHelper = BackupHelper(requireContext())
            printDataManager = PrintDataManager(requireContext())

            setupPreferences()
        }

        private fun setupPreferences() {
            // Theme preference
            findPreference<ListPreference>("app_theme")?.apply {
                value = preferencesManager.appTheme.toString()
                setOnPreferenceChangeListener { _, newValue ->
                    val theme = (newValue as String).toInt()
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

            // Sound enabled
            findPreference<SwitchPreferenceCompat>("sound_enabled")?.apply {
                isChecked = preferencesManager.soundEnabled
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesManager.soundEnabled = newValue as Boolean
                    true
                }
            }

            // Vibration enabled
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
                    preferencesManager.defaultSim = (newValue as String).toInt()
                    true
                }
            }
            
            // Print size
            findPreference<ListPreference>("print_size")?.apply {
                value = preferencesManager.printSize
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesManager.printSize = newValue as String
                    true
                }
            }

            // Export data
            findPreference<Preference>("export_data")?.setOnPreferenceClickListener {
                showExportDialog()
                true
            }

            // Backup settings
            findPreference<SwitchPreferenceCompat>("backup_enabled")?.apply {
                isChecked = preferencesManager.backupEnabled
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesManager.backupEnabled = newValue as Boolean
                    true
                }
            }

            // Manual backup
            findPreference<Preference>("manual_backup")?.apply {
                setOnPreferenceClickListener {
                    performManualBackup()
                    true
                }
                updateLastBackupSummary()
            }

            // Restore backup
            findPreference<Preference>("restore_backup")?.setOnPreferenceClickListener {
                showRestoreDialog()
                true
            }

            // Layout customization
            findPreference<Preference>("layout_customization")?.setOnPreferenceClickListener {
                requireContext().showToast("Usa el botón 'Editar' en la pantalla principal")
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

            // Privacy policy
            findPreference<Preference>("privacy_policy")?.setOnPreferenceClickListener {
                showPrivacyPolicy()
                true
            }
        }

        private fun updateLastBackupSummary() {
            findPreference<Preference>("manual_backup")?.apply {
                val lastBackup = preferencesManager.lastBackupTime
                summary = if (lastBackup > 0) {
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    "Último respaldo: ${dateFormat.format(Date(lastBackup))}"
                } else {
                    "Nunca se ha realizado un respaldo"
                }
            }
        }

        private fun showExportDialog() {
            val options = arrayOf("CSV", "PDF", "Ambos")
            val selected = when (preferencesManager.exportFormat) {
                "csv" -> 0
                "pdf" -> 1
                "both" -> 2
                else -> 0
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Exportar historial")
                .setSingleChoiceItems(options, selected) { dialog, which ->
                    when (which) {
                        0 -> {
                            exportHelper.exportToCSV(true)
                            preferencesManager.exportFormat = "csv"
                        }
                        1 -> {
                            exportHelper.exportToPDF(true)
                            preferencesManager.exportFormat = "pdf"
                        }
                        2 -> {
                            exportHelper.exportToCSV(false)
                            exportHelper.exportToPDF(true)
                            preferencesManager.exportFormat = "both"
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
            val backupFiles = backupHelper.getAvailableBackups()

            if (backupFiles.isEmpty()) {
                requireContext().showToast("No hay respaldos disponibles")
                return
            }

            val fileNames = backupFiles.map {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                "Respaldo del ${dateFormat.format(Date(it.lastModified()))}"
            }.toTypedArray()

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Seleccionar respaldo")
                .setItems(fileNames) { _, which ->
                    restoreBackup(backupFiles[which])
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        private fun restoreBackup(file: java.io.File) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Restaurar respaldo")
                .setMessage("¿Estás seguro? Esto reemplazará todos los datos actuales.")
                .setPositiveButton("Restaurar") { _, _ ->
                    if (backupHelper.restoreBackup(file)) {
                        requireContext().showToast("Respaldo restaurado exitosamente")
                        // Restart app to apply changes
                        val intent = requireContext().packageManager
                            .getLaunchIntentForPackage(requireContext().packageName)
                        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        if (intent != null) {
                            startActivity(intent)
                        }
                        requireActivity().finish()
                    } else {
                        requireContext().showToast("Error al restaurar respaldo")
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        private fun showClearDataDialog() {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Borrar todos los datos")
                .setMessage("¿Estás seguro? Esta acción no se puede deshacer.\n\nSe borrarán:\n• Historial de transacciones\n• Estadísticas\n• Configuraciones personalizadas")
                .setPositiveButton("Borrar") { _, _ ->
                    // Clear all data
                    printDataManager.clearAllData()
                    preferencesManager.clearAll()
                    requireContext().showToast("Todos los datos han sido borrados")

                    // Restart app
                    val intent = requireContext().packageManager
                        .getLaunchIntentForPackage(requireContext().packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    if (intent != null) {
                        startActivity(intent)
                    }
                    requireActivity().finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        private fun showAboutDialog() {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Acerca de")
                .setMessage("""
                    Genio Tecni
                    Versión 2.0.0
                    
                    Desarrollado para facilitar las transacciones
                    de servicios financieros en Paraguay.
                    
                    © 2024 Genio Tecni
                    Todos los derechos reservados
                """.trimIndent())
                .setPositiveButton("Aceptar", null)
                .show()
        }

        private fun showPrivacyPolicy() {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Política de Privacidad")
                .setMessage("""
                    Tu privacidad es importante para nosotros.
                    
                    • No compartimos tus datos personales
                    • Toda la información se almacena localmente
                    • No se requiere conexión a internet
                    • Tú tienes el control total de tus datos
                    
                    Para más información, visita nuestra página web.
                """.trimIndent())
                .setPositiveButton("Aceptar", null)
                .show()
        }
    }
}