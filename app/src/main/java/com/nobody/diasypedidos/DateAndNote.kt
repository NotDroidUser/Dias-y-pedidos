package com.nobody.diasypedidos

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import com.nobody.diasypedidos.databinding.DateNoteHolderBinding
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

val settingsFile = "DateAndNote.json"

data class DateAndNote(var time: Long,
                       var hours: Int = 0,
                       var minutes: Int =0,
                       var text: String,
                       var direction: String = "") : Parcelable {
    constructor(parcel: Parcel) : this(
      parcel.readLong(),
      parcel.readInt(),
      parcel.readInt(),
      parcel.readString()?:"",
      parcel.readString()?:"")
    
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(time)
        parcel.writeInt(hours)
        parcel.writeInt(minutes)
        parcel.writeString(text)
        parcel.writeString(direction)
    }
    
    override fun describeContents(): Int {
        return 0
    }
    
    companion object CREATOR : Parcelable.Creator<DateAndNote> {
        override fun createFromParcel(parcel: Parcel): DateAndNote {
            return DateAndNote(parcel)
        }
        
        override fun newArray(size: Int): Array<DateAndNote?> {
            return arrayOfNulls(size)
        }
    }
}


class DateAndNoteSaver(var list:List<DateAndNote>)

data class DateOf(var time:Long, var hours: Int=0,var minutes:Int=0){
    
    fun toText():String{
        return GregorianCalendar().let{
            it.timeInMillis=time
            "${it.get(Calendar.DAY_OF_MONTH)}/" +
                    "${it.get(Calendar.MONTH)+1}/" +
                    "${it.get(Calendar.YEAR)}${if(hours!=0){
                        " a las ${
                            if (hours/12==1){
                                hours.mod(12).toString()+":" +
                                  "${if (minutes>9)
                                        minutes
                                     else
                                        "0$minutes"
                                  } pm"
                            }else{
                                "$hours:${
                                    if (minutes > 9)
                                        minutes
                                    else
                                        "0$minutes"
                                } am"
                            }
                        }"
                    }else ""}"
        }
    }
}

//fun Calendar.toDateOf():DateOf{
//    return DateOf(this.timeInMillis,this.get(Calendar.HOUR_OF_DAY),this.get(Calendar.MINUTE))
//}

fun saveData(toSave: DateAndNoteSaver,context: Context) {
    val saved = GsonBuilder().setPrettyPrinting().create().toJson(toSave)
    val out = context.openFileOutput(settingsFile, Context.MODE_PRIVATE)
    out.writer().buffered().also {
        it.write(saved)
        it.flush()
        it.close()
    }
}

fun saveDataExternally(toSave: DateAndNoteSaver,out:OutputStream) {
    val saved = GsonBuilder().setPrettyPrinting().create().toJson(toSave)
    out.writer().buffered().also {
        it.write(saved)
        it.flush()
        it.close()
    }
}

fun loadData(context: Context): DateAndNoteSaver {
    return try {
        val input: FileInputStream = context.openFileInput(settingsFile)
        val toLoad = input.reader().buffered().readText()
        GsonBuilder().setPrettyPrinting().create().fromJson(toLoad, DateAndNoteSaver::class.java)
            ?: DateAndNoteSaver(listOf())
    }catch(e:FileNotFoundException){
        DateAndNoteSaver(listOf())
    }
}

fun loadDataExternally(input: InputStream): DateAndNoteSaver {
    return try {
        val toLoad = input.reader().buffered().readText()
        GsonBuilder().setPrettyPrinting().create().fromJson(toLoad, DateAndNoteSaver::class.java)
            ?: DateAndNoteSaver(listOf())
    }catch(e:FileNotFoundException){
        DateAndNoteSaver(listOf())
    }
}

class DateAndNoteAdapter(var dateAndNoteHandler:DateAndNoteHandler):ListAdapter<DateAndNote,DateAndNoteAdapter.DateAndNoteHolder>(object :DiffUtil.ItemCallback<DateAndNote>(){
    override fun areItemsTheSame(oldItem: DateAndNote, newItem: DateAndNote): Boolean {
        return oldItem.text==newItem.text
    }
    
    override fun areContentsTheSame(oldItem: DateAndNote, newItem: DateAndNote): Boolean {
        return oldItem.text==newItem.text &&
                oldItem.hours==newItem.hours &&
                oldItem.direction==newItem.direction &&
                oldItem.time==newItem.time &&
                oldItem.minutes == newItem.minutes
    }
    
}){
    
    
    
    class DateAndNoteHolder(var binding:DateNoteHolderBinding): RecyclerView.ViewHolder(binding.root)
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)=
            DateAndNoteHolder (DateNoteHolderBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    
    override fun onBindViewHolder(holder: DateAndNoteHolder, position: Int) {
        with(holder.binding){
            val item = getItem(position)
            direccion.text= item.direction
            text.text= item.text
            hour.text= DateOf(item.time,item.hours,item.minutes).toText()
            if(position<itemCount){
                divider.visibility= View.VISIBLE
            }
            root.setOnCreateContextMenuListener { menu, v, menuInfo ->
                MenuInflater(root.context).inflate(R.menu.context,menu)
                menu.forEach {
                    when (it.itemId) {
                        R.id.action_delete -> {
                            it.setOnMenuItemClickListener {
                                dateAndNoteHandler.removeItem(item)
                                true
                            }
                        }
                        R.id.action_edit->{
                            it.setOnMenuItemClickListener {
                                dateAndNoteHandler.editItem(item)
                                true
                            }
                        }
                    }
                }
            }
            root.setOnClickListener {
                dateAndNoteHandler.viewItem(item)
            }
        }
    }
    
    
}
interface DateAndNoteHandler{
    fun removeItem(item:DateAndNote)
    fun editItem(item:DateAndNote)
    fun viewItem(item:DateAndNote)
}


fun Calendar.setCalendarDateOnly(){
    this.set(Calendar.HOUR_OF_DAY,0)
    this.set(Calendar.MINUTE,0)
    this.set(Calendar.SECOND,0)
    this.set(Calendar.MILLISECOND,0)
}