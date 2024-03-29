package com.nobody.diasypedidos

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nobody.diasypedidos.databinding.DateNoteHolderBinding
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

const val settingsFile = "DateAndNote.json"

@Serializable
data class DateAndNote(var time: Long,
                       var hours: Int = 0,
                       var minutes: Int =0,
                       var text: String,
                       var direction: String = "",
                       var app:String = "",
                       //@SerializedName("pic")
                       @SerialName("pic")
                       var picturePath:String = "",
                       //@SerializedName("")
                       @SerialName("")
                       var finished:Boolean=false) : Parcelable {
    constructor(parcel: Parcel) : this(
      parcel.readLong(),
      parcel.readInt(),
      parcel.readInt(),
      parcel.readString()?:"",
      parcel.readString()?:"",
      parcel.readString()?:"",
      parcel.readString()?:"",
      parcel.readByte() != 0.toByte())
    
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(time)
        parcel.writeInt(hours)
        parcel.writeInt(minutes)
        parcel.writeString(text)
        parcel.writeString(direction)
        parcel.writeString(app)
        parcel.writeString(picturePath)
        parcel.writeByte(if (finished) 1 else 0)
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


@Serializable
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
    val saved = json.encodeToString(DateAndNoteSaver.serializer(),toSave)
    val out = context.openFileOutput(settingsFile, Context.MODE_PRIVATE)
    out.writer().buffered().also {
        it.write(saved)
        it.flush()
        it.close()
    }
}

fun saveDataExternally(out:OutputStream,context:Context) {
    val zipOut = ZipArchiveOutputStream(out)
    context.filesDir.listFiles{ dir, name ->
        !File(dir,name).isDirectory
    }?.forEach {
        zipOut.putArchiveEntry(zipOut.createArchiveEntry(it,it.name))
        zipOut.write(it.readBytes())
        zipOut.closeArchiveEntry()
    }
    zipOut.flush()
    zipOut.close()
}

private val json = Json { ignoreUnknownKeys = true }

fun saveDataExternallyOld(toSave: DateAndNoteSaver, out:OutputStream) {
    val saved = json.encodeToString(DateAndNoteSaver.serializer(),toSave)
    out.writer().buffered().also {
        it.write(saved)
        it.flush()
        it.close()
    }
}

inline fun <reified T : Parcelable> Bundle.getParcelableCompat(str:String): T? {
    return if(android.os.Build.VERSION_CODES.TIRAMISU <= android.os.Build.VERSION.SDK_INT){
        getParcelable(str, T::class.java)
    }else{
        //tiramisu making things better to avoid
        @Suppress("DEPRECATION")
        getParcelable(str)
    }
}

fun loadData(context: Context): DateAndNoteSaver {
    return try {
        val input: FileInputStream = context.openFileInput(settingsFile)
        val toLoad = input.reader().buffered().readText()
        json.decodeFromString(DateAndNoteSaver.serializer(),toLoad)
    }catch(e:FileNotFoundException){
        DateAndNoteSaver(listOf())
    }
}

fun verifyFile(input: InputStream): Boolean {
    val entries = mutableListOf<ZipArchiveEntry>()
    val zipArchiveInputStream= ZipArchiveInputStream(input)
    var isBackup=false
    var entry = zipArchiveInputStream.nextEntry
    try {
        while (entry!=null){
            entries.add(entry)
            isBackup=entry.name.contains(settingsFile)||isBackup
            entry = zipArchiveInputStream.nextEntry
        }
    }catch (_:IOException){
    
    }
    return isBackup
}

fun clearData(context: Context){
    for (i in context.fileList()){
        try {
            File(i).delete()
        }catch (_:EOFException){
        
        }catch (_:FileNotFoundException){
        
        }
    }
}

fun loadDataExternally(input: InputStream,context: Context) {
    clearData(context)
    val zipArchiveInputStream= ZipArchiveInputStream(input)
    var entry = zipArchiveInputStream.nextEntry
    try {
        while (entry!=null){
            val file = context.openFileOutput(entry.name, Context.MODE_PRIVATE)
            zipArchiveInputStream.copyTo(file)
            file.flush()
            file.close()
            entry = zipArchiveInputStream.nextEntry
        }
    }catch (ex:IOException){
        Log.e("LOAD FILE:", "loadDataExternally Failed: ${ex.stackTrace}")
    }
}

fun loadDataExternallyOld(input: InputStream): DateAndNoteSaver {
    return try {
        val toLoad = input.reader().buffered().readText()
        json.decodeFromString(DateAndNoteSaver.serializer(),toLoad)
    }catch(e:FileNotFoundException){
        DateAndNoteSaver(listOf())
    }
}

class DateAndNoteAdapter(private var dateAndNoteHandler:DateAndNoteHandler):ListAdapter<DateAndNote,DateAndNoteAdapter.DateAndNoteHolder>(object :DiffUtil.ItemCallback<DateAndNote>(){
    override fun areItemsTheSame(oldItem: DateAndNote, newItem: DateAndNote): Boolean {
        return oldItem.text==newItem.text &&
            oldItem.picturePath==newItem.picturePath
    }
    
    override fun areContentsTheSame(oldItem: DateAndNote, newItem: DateAndNote): Boolean {
        return oldItem.text==newItem.text &&
                oldItem.hours==newItem.hours &&
                oldItem.direction==newItem.direction &&
                oldItem.time==newItem.time &&
                oldItem.minutes == newItem.minutes&&
                oldItem.picturePath==newItem.picturePath&&
                oldItem.app==newItem.app
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
                        R.id.action_edit->{
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
                photoView.visibility=View.VISIBLE
                photoView.setImageDrawable(Drawable.createFromPath(item.picturePath))
            }else{
                photoView.visibility=View.GONE
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
interface DateAndNoteHandler{
    fun removeItem(item:DateAndNote)
    fun editItem(item:DateAndNote)
    fun modifyItem(old:DateAndNote, isChecked:Boolean)
    fun viewPicture(item: DateAndNote)
    fun viewItem(item:DateAndNote)
}


fun Calendar.setCalendarDateOnly(){
    this.set(Calendar.HOUR_OF_DAY,0)
    this.set(Calendar.MINUTE,0)
    this.set(Calendar.SECOND,0)
    this.set(Calendar.MILLISECOND,0)
}