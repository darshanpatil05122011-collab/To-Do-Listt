package com.example.dailylist

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private val tasks: MutableList<Task>,
    private val onChanged: () -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.taskCheckBox)
        val textView: TextView = view.findViewById(R.id.taskText)
        val deleteBtn: Button = view.findViewById(R.id.deleteTaskBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.textView.text = task.text
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = task.done
        applyDoneStyle(holder.textView, task.done)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            task.done = isChecked
            applyDoneStyle(holder.textView, isChecked)
            onChanged()
        }

        holder.deleteBtn.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                tasks.removeAt(pos)
                notifyItemRemoved(pos)
                onChanged()
            }
        }
    }

    private fun applyDoneStyle(textView: TextView, done: Boolean) {
        if (done) {
            textView.paintFlags = textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            textView.setTextColor(0xFFB0AA98.toInt())
        } else {
            textView.paintFlags = textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            textView.setTextColor(0xFF2E2B24.toInt())
        }
    }

    override fun getItemCount(): Int = tasks.size
}
