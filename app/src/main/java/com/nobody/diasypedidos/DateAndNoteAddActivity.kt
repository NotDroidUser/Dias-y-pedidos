package com.nobody.diasypedidos

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.datetime.datePicker
import com.afollestad.materialdialogs.datetime.timePicker
import com.nobody.diasypedidos.MainActivity.Companion.DATENOTE
import com.nobody.diasypedidos.MainActivity.Companion.OLDNOTE
import com.nobody.diasypedidos.databinding.ActivityAddBinding
import com.nobody.diasypedidos.db.DateAndNoteDB
import java.io.File
import java.io.FileNotFoundException
import java.util.Calendar
import java.util.GregorianCalendar


class DateAndNoteAddActivity : AppCompatActivity() {
    private var onResult: Boolean = false
    private lateinit var oldnote: DateAndNoteDB
    private lateinit var note: DateAndNoteDB
    private var newPath:String=""
    
    private val picChooser=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if (it.resultCode == RESULT_OK) {
            it.data?.data?.let { file->
                try {
                    val photo = contentResolver.openInputStream(file)!!
                    val photoFile = File(filesDir, "${GregorianCalendar().timeInMillis}.jpg")
                        .apply {
                            createNewFile()
                            writeBytes(photo.readBytes())
                            photo.close()
                        }
                    newPath = photoFile.path
                    if (::oldnote.isInitialized) {
                        if (note.picturePath!=oldnote.picturePath) {
                            File(note.picturePath).apply {
                                if(exists()){
                                    delete()
                                }
                            }
                        }
                    } else {
                        if(note.picturePath!=""){
                            File(note.picturePath).apply {
                                if(exists()){
                                    delete()
                                }
                            }
                        }
                    }
                    binding.photo.setImageDrawable(Drawable.createFromPath(photoFile.path))
                    note.picturePath = photoFile.path
                    if (MainActivity.DEBUG) {
                        Log.e(TAG, "onActivityResult: $note" )
                    }
                } catch (_: FileNotFoundException) {
                    Toast.makeText(this@DateAndNoteAddActivity, "No se pudo obtener la foto", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private val binding: ActivityAddBinding by lazy {
        ActivityAddBinding.inflate(layoutInflater)
    }
    
    private fun picInflate() {
        if (MainActivity.DEBUG) {
            Log.e(TAG, "$note")
        }
        if (note.picturePath != "" && File(note.picturePath).exists()) {
            binding.photo.setImageDrawable(Drawable.createFromPath(note.picturePath))
        } else {
            binding.photo.setImageResource(R.mipmap.placeholder)
        }
    }
    
    
    override fun onCreate(savedInstanceState: Bundle?) {
        
        super.onCreate(savedInstanceState)
        intent.extras?.let { bundle ->
            //tiramisu things
            bundle.getParcelableCompat<DateAndNoteDB>(DATENOTE)?.let {
                note = it.copy()
                note.id=it.id
                oldnote = it.copy()
                oldnote.id= it.id
            }
        }
        if (MainActivity.DEBUG) {
            Log.e(TAG, "onCreate: oldnote:${if (::oldnote.isInitialized) oldnote.toString()
            else "NULL"}" )
        }
        savedInstanceState?.let { saved ->
            saved.getParcelableCompat<DateAndNoteDB>(DATENOTE)?.let { note = it }
            saved.getParcelableCompat<DateAndNoteDB>(OLDNOTE)?.let { oldnote = it }
            saved.getString(NEWPATH,newPath).also {
                if (newPath!=""){
                    //when photo selected
                    if (newPath!=it&&it!="") {
                        File(it).apply {
                            if(exists()){
                                delete()
                            }
                        }
                    }
                }else{
                    //for photo viewer
                    newPath=it
                }
                
            }
            if (MainActivity.DEBUG) {
                Log.e(TAG, "onSaved: $newPath" )
            }
        }
        if (newPath!="" && note.picturePath!=newPath) {
            note.picturePath=newPath
        }
        with(binding) {
            setContentView(root)
            if (!::note.isInitialized) {
                note = DateAndNoteDB(
                    time = GregorianCalendar().timeInMillis,
                    text = ""
                )
            }
            
            message.setText(note.text)
            address.setText(note.address)
            dateHour.text = dateToText( note.time, note.hours, note.minutes)
            finishedCheck.isChecked=note.finished
            datebutton.setOnClickListener {
                if (MainActivity.DEBUG){
                    Log.e(TAG, "onCreate: ${note.time}" )
                    Log.e(TAG, "minDate: ${(if (note.time == 0L) GregorianCalendar()
                    else GregorianCalendar().apply { timeInMillis = kotlin.math.min(timeInMillis, note.time) }).timeInMillis}" )
                }
                MaterialDialog(this@DateAndNoteAddActivity).datePicker(
                      // fixed cannot go back to current date
                    minDate = GregorianCalendar().apply { timeInMillis = kotlin.math.min(timeInMillis, if (::oldnote.isInitialized){
                        oldnote.time
                    } else {
                        note.time
                    })},
                    currentDate = if (note.time == 0L) GregorianCalendar()
                    else GregorianCalendar().apply { timeInMillis = note.time }
                ) { _, datetime ->
                    note.time = datetime.timeInMillis
                    dateHour.text = dateToText(note.time, note.hours, note.minutes)
                }.show()
            }
            photoAdd.setOnClickListener {
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                picChooser.launch(Intent.createChooser(intent, "Select Picture"))
            }
            picInflate()
            photoDelete.setOnClickListener {
                note.picturePath = ""
                deleteIfIsValid()
                picInflate()
            }
            photo.setOnClickListener {
                if(note.picturePath!=""){
                    startActivity(Intent(this@DateAndNoteAddActivity, ViewPictureActivity::class.java).also {
                        it.putExtra("path", note.picturePath)
                        if (MainActivity.DEBUG) {
                            Log.e(TAG, note.picturePath)
                        }
                    })
                }
            }
            
            val adapter: ArrayAdapter<String> = ArrayAdapter<String>(this@DateAndNoteAddActivity,
              android.R.layout.simple_dropdown_item_1line, arrayOf("Whatsapp", "Telegram", "Facebook"))
            app.setAdapter(adapter)
            app.setText(note.app)
            hourbutton.setOnClickListener {
                MaterialDialog(this@DateAndNoteAddActivity).timePicker(currentTime = GregorianCalendar()
                  .apply {
                      set(Calendar.HOUR_OF_DAY, note.hours)
                      set(Calendar.MINUTE, note.minutes)
                  }, show24HoursView = false)
                { _, datetime ->
                    note.hours = datetime.get(Calendar.HOUR_OF_DAY)
                    note.minutes = datetime.get(Calendar.MINUTE)
                    dateHour.text = dateToText(note.time, note.hours, note.minutes)
                }.show()
            }
            fabadd.setOnClickListener {
                if (isValid()) {
                    if (MainActivity.DEBUG) {
                        Log.e(TAG, "onResult: oldnote:${if (::oldnote.isInitialized) oldnote
                        else DateAndNoteDB(
                          time = 0,
                          text = "",
                        )}\ndate:$note\n" +
                          "newPath:$newPath\n" )
                    }
                    note.text = message.text.toString().trim()
                    note.address = address.text.toString().trim()
                    note.app = app.text.toString().trim()
                    note.finished= finishedCheck.isChecked
                    setResult(RESULT_OK, Intent().apply {
                        putExtras(
                          Bundle().apply {
                              putExtra(OLDNOTE,
                                if (::oldnote.isInitialized) oldnote
                                else DateAndNoteDB(
                                  time = 0,
                                  text = "",
                                ))
                              putExtra(DATENOTE, note)
                          }
                        )
                    })
                    onResult = true
                    finish()
                }
            }
        }
    }
    
    private fun isValid(): Boolean {
        var isValid=true
        with(binding) {
            if (note.time == 0L) {
                datebutton.setError("Ponga una fecha de entrega",
                  ContextCompat.getDrawable(this@DateAndNoteAddActivity, android.R.drawable.stat_notify_error))
                isValid = false
            }
            if (message.text.toString().trim().isEmpty()) {
                message.error = "Introduzca un mensaje"
                isValid = false
            }
        }
        return isValid
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(DATENOTE, note)
        outState.putString(NEWPATH, newPath)
        if (::oldnote.isInitialized)
            outState.putParcelable(OLDNOTE, oldnote)
        Log.d(TAG, "onSaveInstanceState: ")
    }
    
    private fun deleteIfIsValid() {
        //save the old one fot the case if doesn't change any but delete the picture
        if (newPath!="") {
            File(newPath).apply {
                if(exists()){
                    delete()
                }
            }
        }
    }
    
    override fun onDestroy() {
        if (!onResult) {
            deleteIfIsValid()
        }
        super.onDestroy()
    }
    
    companion object {
        private val TAG=DateAndNoteAddActivity::class.simpleName
        const val NEWPATH = "NEWPATH"
    }
}