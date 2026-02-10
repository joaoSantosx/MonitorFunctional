package com.jvf.monitorfunctional.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.jvf.monitorfunctional.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Modelo de Dados simples para a lista
data class LogVideoApp(
    val titulo: String = "",
    val seguro: Boolean = true,
    val motivo: String = "",
    val timestamp: Long = 0
)

class LogAdapter(private var listaLogs: List<LogVideoApp>) :
    RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtTitulo: TextView = view.findViewById(R.id.txtTituloVideo)
        val txtMotivo: TextView = view.findViewById(R.id.txtMotivo)
        val txtData: TextView = view.findViewById(R.id.txtDataHora)
        val imgStatus: ImageView = view.findViewById(R.id.imgStatus)
        val container: ConstraintLayout = view.findViewById(R.id.containerCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log_video, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = listaLogs[position]

        holder.txtTitulo.text = log.titulo

        // Formata a data
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        holder.txtData.text = sdf.format(Date(log.timestamp))

        if (log.seguro) {
            // Estilo VERDE (Seguro)
            holder.imgStatus.setImageResource(android.R.drawable.ic_input_add) // Ícone simples check
            holder.imgStatus.setColorFilter(Color.parseColor("#4CAF50")) // Verde
            holder.txtMotivo.text = "Conteúdo Seguro"
            holder.txtMotivo.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            // Estilo VERMELHO (Perigo)
            holder.imgStatus.setImageResource(android.R.drawable.ic_dialog_alert)
            holder.imgStatus.setColorFilter(Color.parseColor("#F44336")) // Vermelho
            holder.txtMotivo.text = "⚠️ ${log.motivo}"
            holder.txtMotivo.setTextColor(Color.parseColor("#D32F2F"))
        }
    }

    override fun getItemCount() = listaLogs.size

    fun atualizarLista(novaLista: List<LogVideoApp>) {
        listaLogs = novaLista
        notifyDataSetChanged()
    }
}