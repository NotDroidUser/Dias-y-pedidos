package com.nobody.diasypedidos

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.datetime.datePicker
import com.afollestad.materialdialogs.datetime.timePicker
import com.nobody.diasypedidos.MainActivity.Companion.DATENOTE
import com.nobody.diasypedidos.MainActivity.Companion.EDITNOTE
import com.nobody.diasypedidos.MainActivity.Companion.OLDNOTE
import com.nobody.diasypedidos.databinding.ActivityAddBinding
import java.util.*


class AddActivity : AppCompatActivity() {
    
    private val binding: ActivityAddBinding by lazy {
        ActivityAddBinding.inflate(layoutInflater)
    }
    lateinit var date: DateAndNote
    var editable: Boolean = true
    
    @SuppressLint("UseCompatLoadingForDrawables", "SimpleDateFormat", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.extras?.let {
            it.getParcelable<DateAndNote>(DATENOTE)?.let { date = it }
            it.getBoolean(EDITNOTE).also { editable = it }
        }
        savedInstanceState?.let { saved ->
            saved.getParcelable<DateAndNote>(DATENOTE)?.let { date = it }
            saved.getBoolean(EDITNOTE,editable).also { editable = it }
        }
        if (!editable && ::date.isInitialized) {
            //TODO MAKE NEW VIEW FOR SEE IT
            //MEH
        } else {
            if (!::date.isInitialized) {
                date = DateAndNote(
                        time = 0,
                        hours = 0,
                        text = "",
                        direction = ""
                )
            }else{
                binding.message.setText(date.text)
                binding.direction.setText(date.direction)
                binding.dateHour.text = DateOf(date.time,date.hours,
                  date.minutes).toText()
            }
            setContentView(binding.root)
            with(binding) {
                datebutton.setOnClickListener {
                    MaterialDialog(this@AddActivity).datePicker(
                      minDate = if (date.time==0L) GregorianCalendar()
                                else GregorianCalendar().apply {timeInMillis=date.time},
                      currentDate = if (date.time==0L) GregorianCalendar()
                                    else GregorianCalendar().apply {timeInMillis=date.time}  )
                    { dialog, datetime ->
                        //TODO CHANGE ITEM FOR SEE CURRENT DATE
                        date.time = datetime.timeInMillis
                        dateHour.text = DateOf(date.time,date.hours,
                          date.minutes).toText()
                    }.show()
                }
                hourbutton.setOnClickListener {
                    MaterialDialog(this@AddActivity).timePicker( currentTime = GregorianCalendar()
                            .apply{
                                set(Calendar.HOUR_OF_DAY,date.hours)
                                set(Calendar.MINUTE,date.minutes)
                            }, show24HoursView = false)
                            { dialog, datetime ->
                                //TODO CHANGE ITEM FOR SEE CURRENT DATE
                                date.hours = datetime.get(Calendar.HOUR_OF_DAY)
                                date.minutes= datetime.get(Calendar.MINUTE)
                                binding.dateHour.text = DateOf(if (date.time==0L) GregorianCalendar().timeInMillis
                                else date.time,date.hours,date.minutes).toText()
                            }.show()
                }
                fabadd.setOnClickListener {
                    val messageText = message.text.toString()
                    if (messageText.isNotEmpty() && date.time != 0L) {
                        val directionText = direction.text.toString()
                        date.text = messageText
                        date.direction = directionText
                        setResult(RESULT_OK, Intent().apply {
                            putExtras(
                                    Bundle().apply {
                                        intent.extras?.let {
                                            putExtra(OLDNOTE, it.getParcelable<DateAndNote>(DATENOTE)
                                                ?: DateAndNote(
                                                        time = 0,
                                                        hours = 0,
                                                        text = "",
                                                        direction = ""
                                                ))
                                        }
                                        putExtra(DATENOTE, date)
                                        putExtra(EDITNOTE, editable)
                                    }
                            )
                        })
                        finish()
                    }
                    if(date.time != 0L){
                        datebutton.setError("Ponga una fecha de entrega",getDrawable(android.R.drawable.stat_notify_error))
                    }
                    if(messageText.isEmpty()){
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
    }
}