package com.example.geniotecni.tigo.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.geniotecni.tigo.R
import com.example.geniotecni.tigo.models.PrintData
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class PrintHistoryAdapter(
    private var printHistory: List<PrintData>,
    private val onReprintClick: (PrintData) -> Unit,
    private val onShareClick: (PrintData) -> Unit = {},
    private val onDeleteClick: (PrintData) -> Unit = {}
) : RecyclerView.Adapter<PrintHistoryAdapter.ViewHolder>() {

    private val expandedItems = mutableSetOf<Int>()
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    companion object {
        private val serviceIcons = mapOf(
            "Giros Tigo" to R.drawable.ic_money,
            "ANDE" to R.drawable.ic_electricity,
            "Reseteo de Cliente" to R.drawable.ic_reset,
            "Telefonia Tigo" to R.drawable.ic_phone,
            "Retiros Tigo" to R.drawable.ic_money,
            "Carga Billetera Tigo" to R.drawable.ic_money,
            "ESSAP" to R.drawable.ic_water,
            "COPACO" to R.drawable.ic_phone
        )

        private val serviceColors = mapOf(
            "Giros Tigo" to R.color.service_tigo,
            "ANDE" to R.color.service_ande,
            "Reseteo de Cliente" to R.color.service_tigo,
            "Telefonia Tigo" to R.color.service_tigo,
            "Retiros Tigo" to R.color.service_tigo,
            "Carga Billetera Tigo" to R.color.service_tigo,
            "ESSAP" to R.color.status_info,
            "COPACO" to R.color.status_success
        )
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconBackground: View = view.findViewById(R.id.iconBackground)
        val serviceIcon: ImageView = view.findViewById(R.id.serviceIcon)
        val textViewService: TextView = view.findViewById(R.id.textViewService)
        val textViewDateTime: TextView = view.findViewById(R.id.textViewDateTime)
        val textViewAmount: TextView = view.findViewById(R.id.textViewAmount)
        val textViewReference: TextView = view.findViewById(R.id.textViewReference)
        val textViewMessage: TextView = view.findViewById(R.id.textViewMessage)
        val buttonReprint: MaterialButton = view.findViewById(R.id.buttonReprint)
        val buttonMore: MaterialButton = view.findViewById(R.id.buttonMore)
        val statusChip: Chip = view.findViewById(R.id.statusChip)
        val amountRow: LinearLayout = view.findViewById(R.id.amountRow)
        val detailsContainer: LinearLayout = view.findViewById(R.id.detailsContainer)

        init {
            // Set click listener for expand/collapse
            itemView.setOnClickListener {
                toggleExpanded(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_print_history, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val printData = printHistory[position]
        val context = holder.itemView.context
        val isExpanded = expandedItems.contains(position)

        // Set service info
        holder.textViewService.text = printData.service
        holder.textViewDateTime.text = "${printData.date} ${printData.time}"

        // Set icon and color
        val iconRes = serviceIcons[printData.service] ?: R.drawable.ic_money
        val colorRes = serviceColors[printData.service] ?: R.color.md_theme_light_primary
        holder.serviceIcon.setImageResource(iconRes)
        holder.iconBackground.background.setTint(ContextCompat.getColor(context, colorRes))

        // Extract and display amount if available
        val amount = extractAmount(printData.message)
        if (amount > 0) {
            holder.amountRow.visibility = View.VISIBLE
            holder.textViewAmount.text = formatAmount(amount)
        } else {
            holder.amountRow.visibility = View.GONE
        }

        // Set reference
        val reference = extractReference(printData.message)
        holder.textViewReference.text = reference ?: "N/A"

        // Set status
        updateStatusChip(holder.statusChip, printData, context)

        // Show/hide expanded content
        holder.textViewMessage.apply {
            text = printData.message
            visibility = if (isExpanded) View.VISIBLE else View.GONE

            if (isExpanded) {
                val slideDown = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
                startAnimation(slideDown)
            }
        }

        // Setup buttons
        holder.buttonReprint.setOnClickListener {
            showReprintConfirmation(context, printData)
        }

        holder.buttonMore.setOnClickListener {
            showMoreOptions(context, printData, position)
        }
    }

    override fun getItemCount() = printHistory.size

    private fun toggleExpanded(position: Int) {
        if (expandedItems.contains(position)) {
            expandedItems.remove(position)
        } else {
            expandedItems.add(position)
        }
        notifyItemChanged(position)
    }

    private fun extractAmount(message: String): Long {
        val regex = Regex("""Monto:\s*([0-9,]+)\s*Gs\.""")
        val match = regex.find(message)
        return match?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull() ?: 0L
    }

    private fun extractReference(message: String): String? {
        val regex = Regex("""Ref:\s*(\w+)""")
        val match = regex.find(message)
        return match?.groupValues?.get(1)
    }

    private fun formatAmount(amount: Long): String {
        return "${DecimalFormat("#,###").format(amount)} Gs."
    }

    private fun updateStatusChip(chip: Chip, printData: PrintData, context: Context) {
        // Check if transaction has references (indicating success)
        val hasReferences = printData.referenceData.ref1.isNotEmpty() ||
                printData.referenceData.ref2.isNotEmpty()

        if (hasReferences) {
            chip.text = "Completado"
            chip.chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.status_success)
        } else {
            chip.text = "Pendiente"
            chip.chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.status_warning)
        }
    }

    private fun showReprintConfirmation(context: Context, printData: PrintData) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Reimprimir Transacción")
            .setMessage("¿Desea reimprimir esta transacción?")
            .setPositiveButton("Reimprimir") { _, _ ->
                onReprintClick(printData)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showMoreOptions(context: Context, printData: PrintData, position: Int) {
        val options = arrayOf("Compartir", "Copiar referencia", "Ver detalles", "Eliminar")

        MaterialAlertDialogBuilder(context)
            .setTitle("Opciones")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> shareTransaction(context, printData)
                    1 -> copyReference(context, printData)
                    2 -> showDetails(context, printData)
                    3 -> confirmDelete(context, printData, position)
                }
            }
            .show()
    }

    private fun shareTransaction(context: Context, printData: PrintData) {
        val shareText = buildString {
            append("Transacción ${printData.service}\n")
            append("Fecha: ${printData.date} ${printData.time}\n")
            append("------------------------\n")
            append(printData.message)
        }

        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
        }

        context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir transacción"))
        onShareClick(printData)
    }

    private fun copyReference(context: Context, printData: PrintData) {
        val reference = extractReference(printData.message) ?: "N/A"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Referencia", reference)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(context, "Referencia copiada", Toast.LENGTH_SHORT).show()
    }

    private fun showDetails(context: Context, printData: PrintData) {
        val details = buildString {
            append("Servicio: ${printData.service}\n")
            append("Fecha: ${printData.date}\n")
            append("Hora: ${printData.time}\n")

            val amount = extractAmount(printData.message)
            if (amount > 0) {
                append("Monto: ${formatAmount(amount)}\n")

                // Calculate commission
                val commission = (amount * 0.01).toLong()
                append("Comisión: ${formatAmount(commission)}\n")
            }

            if (printData.referenceData.ref1.isNotEmpty()) {
                append("\nReferencia 1: ${printData.referenceData.ref1}")
            }
            if (printData.referenceData.ref2.isNotEmpty()) {
                append("\nReferencia 2: ${printData.referenceData.ref2}")
            }

            val reference = extractReference(printData.message)
            if (reference != null) {
                append("\nCódigo: $reference")
            }
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("Detalles de la Transacción")
            .setMessage(details)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun confirmDelete(context: Context, printData: PrintData, position: Int) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Eliminar Transacción")
            .setMessage("¿Está seguro de que desea eliminar esta transacción del historial?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteItem(position)
                onDeleteClick(printData)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteItem(position: Int) {
        val newList = printHistory.toMutableList()
        newList.removeAt(position)
        updateData(newList)
    }

    fun updateData(newData: List<PrintData>) {
        val diffCallback = PrintHistoryDiffCallback(printHistory, newData)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        printHistory = newData
        expandedItems.clear()
        diffResult.dispatchUpdatesTo(this)
    }

    class PrintHistoryDiffCallback(
        private val oldList: List<PrintData>,
        private val newList: List<PrintData>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // Compare by unique combination of service, date, and time
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem.service == newItem.service &&
                    oldItem.date == newItem.date &&
                    oldItem.time == newItem.time
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}