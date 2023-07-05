package com.nobody.diasypedidos

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.datetime.datePicker
import com.afollestad.materialdialogs.datetime.timePicker
import com.nobody.diasypedidos.MainActivity.Companion.DATENOTE
import com.nobody.diasypedidos.MainActivity.Companion.EDITNOTE
import com.nobody.diasypedidos.MainActivity.Companion.NEWPATH
import com.nobody.diasypedidos.MainActivity.Companion.OLDNOTE
import com.nobody.diasypedidos.databinding.ActivityAddBinding
import java.io.File
import java.io.FileNotFoundException
import java.util.*


class AddActivity : AppCompatActivity() {
    private var onResult: Boolean = false
    private lateinit var oldnote: DateAndNote
    private lateinit var date: DateAndNote
    private var newPath:String=""
    private var editable: Boolean = true
    
    private val picChooser=registerForActivityResult(GetPictureContract()){
        val file=it
        if (file != null) {
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
                    if (date.picturePath!=oldnote.picturePath) {
                        File(date.picturePath).delete()
                    }
                }
                binding.photo.setImageDrawable(Drawable.createFromPath(photoFile.path))
                date.picturePath = photoFile.path
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "onActivityResult: $date" )
                }
            } catch (_: FileNotFoundException) {
                Toast.makeText(this@AddActivity, "No se pudo obtener la foto", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private val binding: ActivityAddBinding by lazy {
        ActivityAddBinding.inflate(layoutInflater)
    }
    
    private fun picInflate() {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, "$date")
        }
        if (date.picturePath != "" && File(date.picturePath).exists()) {
            binding.photo.setImageDrawable(Drawable.createFromPath(date.picturePath))
        } else {
            binding.photo.setImageResource(R.mipmap.placeholder)
        }
    }
    
    
    @SuppressLint("UseCompatLoadingForDrawables", "SimpleDateFormat", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        
        super.onCreate(savedInstanceState)
        intent.extras?.let { bundle ->
            bundle.getParcelable<DateAndNote>(DATENOTE)?.let {
                date = it.copy()
                oldnote = it.copy()
            }
            bundle.getBoolean(EDITNOTE).also { editable = it }
        }
        if (BuildConfig.DEBUG) {
            Log.e(TAG, "onCreate: oldnote:${if (::oldnote.isInitialized) oldnote
            else DateAndNote(
              time = 0,
              hours = 0,
              text = "",
              direction = ""
            )}" )
        }
        savedInstanceState?.let { saved ->
            saved.getParcelable<DateAndNote>(DATENOTE)?.let { date = it }
            saved.getBoolean(EDITNOTE,editable).also { editable = it }
            saved.getParcelable<DateAndNote>(OLDNOTE)?.let { oldnote = it }
            saved.getString(NEWPATH,newPath).also {
                if (newPath!=""){
                    val path=newPath
                    newPath=it
                    deleteIfIsValid()
                    newPath=path
                }else{
                    newPath=it
                }
            }
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "onSaved: $newPath" )
            }
        }
        if (!editable && ::date.isInitialized) {
            //TODO MAKE NEW VIEW FOR SEE IT
            //MEH
        } else {
            if (newPath!="" && date.picturePath!=newPath) {
                date.picturePath=newPath
            }
            if (!::date.isInitialized) {
                date = DateAndNote(
                        time = 0,
                        hours = 0,
                        text = "",
                        direction = ""
                )
            } else {
                binding.message.setText(date.text)
                binding.direction.setText(date.direction)
                binding.dateHour.text = DateOf(date.time,date.hours,
                  date.minutes).toText()
            }
            setContentView(binding.root)
            with(binding) {
                datebutton.setOnClickListener {
                    MaterialDialog(this@AddActivity).datePicker(
                        // fixed cannot go back to current date
                        minDate = if (date.time == 0L) GregorianCalendar()
                        else GregorianCalendar().apply { timeInMillis = kotlin.math.min(timeInMillis, date.time) },
                        currentDate = if (date.time == 0L) GregorianCalendar()
                                      else GregorianCalendar().apply { timeInMillis = date.time })
                    { _, datetime ->
                        date.time = datetime.timeInMillis
                        dateHour.text = DateOf(date.time, date.hours,
                            date.minutes).toText()
                    }.show()
                }
                photoAdd.setOnClickListener {
                    picChooser.launch(Unit)
                }
                picInflate()
                photoDelete.setOnClickListener {
                    date.picturePath = ""
                    deleteIfIsValid()
                    picInflate()
                }
                photo.setOnClickListener {
                    startActivity(Intent(this@AddActivity, ViewPictureActivity::class.java).also {
                        it.putExtra("path", date.picturePath)
                        if (BuildConfig.DEBUG) {
                            Log.e(TAG, date.picturePath)
                        }
                    })
                }
                
                val adapter: ArrayAdapter<String> = ArrayAdapter<String>(this@AddActivity,
                    android.R.layout.simple_dropdown_item_1line, arrayOf("Whatsapp", "Telegram", "Facebook"))
                app.setAdapter(adapter)
                app.setText(date.app)
                hourbutton.setOnClickListener {
                    MaterialDialog(this@AddActivity).timePicker(currentTime = GregorianCalendar()
                        .apply {
                            set(Calendar.HOUR_OF_DAY, date.hours)
                            set(Calendar.MINUTE, date.minutes)
                        }, show24HoursView = false)
                    { _, datetime ->
                        
                        date.hours = datetime.get(Calendar.HOUR_OF_DAY)
                        date.minutes = datetime.get(Calendar.MINUTE)
                        binding.dateHour.text = DateOf(if (date.time == 0L) GregorianCalendar().timeInMillis
                        else date.time, date.hours, date.minutes).toText()
                    }.show()
                }
                fabadd.setOnClickListener {
                    val messageText = message.text.toString()
                    if (messageText.isNotEmpty() && date.time != 0L) {
                        date.text = messageText
                        date.direction = direction.text.toString()
                        date.app = app.text.toString()
                        setResult(RESULT_OK, Intent().apply {
                            putExtras(
                                Bundle().apply {
                                    putExtra(OLDNOTE,
                                        if (::oldnote.isInitialized) oldnote
                                        else DateAndNote(
                                            time = 0,
                                            hours = 0,
                                            text = "",
                                            direction = ""
                                        ))
                                    putExtra(DATENOTE, date)
                                    putExtra(EDITNOTE, editable)
                                }
                            )
                        })
                        if (BuildConfig.DEBUG) {
                            Log.e(TAG, "onResult: oldnote:${if (::oldnote.isInitialized) oldnote
                            else DateAndNote(
                              time = 0,
                              hours = 0,
                              text = "",
                              direction = ""
                            )}\ndate:$date\n" +
                              "editable:$editable\nnewPath:$newPath\n" )
                        }
                        onResult = true
                        finish()
                    }
                    if (date.time != 0L) {
                        datebutton.setError("Ponga una fecha de entrega", getDrawable(android.R.drawable.stat_notify_error))
                    }
                    if (messageText.isEmpty()) {
                        message.error = "Introduzca un mensaje"
                    }
                }
            }
            
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(DATENOTE, date)
        outState.putBoolean(EDITNOTE, editable)
        outState.putString(NEWPATH, newPath)
        if (::oldnote.isInitialized)
            outState.putParcelable(OLDNOTE, oldnote)
        Log.d(TAG, "onSaveInstanceState: ")
    }
    
    private fun deleteIfIsValid() {
        //save the old one fot the case if doesn't change any but delete the picture
        if (newPath!="") {
            File(newPath).delete()
        }
    }
    
    override fun onDestroy() {
        if (!onResult) {
            deleteIfIsValid()
        }
        super.onDestroy()
    }
    
    companion object {
        private val TAG=AddActivity::class.simpleName
    }
}