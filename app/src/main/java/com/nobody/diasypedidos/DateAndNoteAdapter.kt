package com.nobody.diasypedidos

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nobody.diasypedidos.databinding.DateNoteHolderBinding
import com.nobody.diasypedidos.db.DateAndNoteDB
import java.io.File

class DateAndNoteAdapter(private var dateAndNoteHandler: DateAndNoteHandler): ListAdapter<DateAndNoteDB, DateAndNoteAdapter.DateAndNoteHolder>(object : DiffUtil.ItemCallback<DateAndNoteDB>(){
    override fun areItemsTheSame(oldItem: DateAndNoteDB, newItem: DateAndNoteDB): Boolean {
        return oldItem.text==newItem.text &&
            oldItem.picturePath==newItem.picturePath
    }
    
    override fun areContentsTheSame(oldItem: DateAndNoteDB, newItem: DateAndNoteDB): Boolean {
        return oldItem.text==newItem.text &&
                oldItem.hours==newItem.hours &&
                oldItem.address==newItem.address &&
                oldItem.time==newItem.time &&
                oldItem.minutes == newItem.minutes&&
                oldItem.picturePath==newItem.picturePath&&
                oldItem.app==newItem.app
    }
    
}){
    
    class DateAndNoteHolder(var binding: DateNoteHolderBinding): RecyclerView.ViewHolder(binding.root)
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)=
            DateAndNoteHolder (DateNoteHolderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    
    override fun onBindViewHolder(holder: DateAndNoteHolder, position: Int) {
        with(holder.binding){
            val item = getItem(position)
            direccion.text= item.address
            text.text= item.text
            hour.text= dateToText(item.time, item.hours, item.minutes)
            if(position<itemCount){
                divider.visibility= View.VISIBLE
            }
            root.setOnCreateContextMenuListener { menu, _, _ ->
                MenuInflater(root.context).inflate(R.menu.context,menu)
                menu.forEach {
                    when (it.itemId) {
                        R.id.action_delete -> {
                            it.setOnMenuItemClickListener {
                                dateAndNoteHandler.removeItem(item)
                                true
                            }
                        }
                        R.id.action_edit ->{
                            it.setOnMenuItemClickListener {
                                dateAndNoteHandler.editItem(item)
                                true
                            }
                        }
                    }
                }
            }
            appView.text=item.app
            if(File(item.picturePath).exists()) {
                photoView.visibility= View.VISIBLE
                photoView.setImageDrawable(Drawable.createFromPath(item.picturePath))
            }else{
                photoView.visibility= View.GONE
            }
            photoView.setOnClickListener {
                dateAndNoteHandler.viewPicture(item)
            }
            root.setOnClickListener{
                dateAndNoteHandler.viewItem(item)
            }
            finished.isChecked=item.finished
            finished.setOnCheckedChangeListener { _, isChecked ->
                dateAndNoteHandler.modifyItem(old = item, isChecked)
            }
        }
    }
    
    
}