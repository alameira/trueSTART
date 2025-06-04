package dev.mc.truestart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AlarmAdapter(private val alarms: List<String>, private val onLongClick: (String, Int) -> Unit) :
    RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    class AlarmViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val alarmTime: TextView = view.findViewById(R.id.alarmTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.alarm_item, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val time = alarms[position]
        holder.alarmTime.text = alarms[position]
        holder.itemView.setOnLongClickListener {
            onLongClick(time, position)
            true
        }
    }

    override fun getItemCount(): Int = alarms.size
}
