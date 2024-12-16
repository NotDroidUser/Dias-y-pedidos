package com.nobody.diasypedidos

import android.os.Bundle
import android.os.Parcelable
import java.util.Calendar
import java.util.GregorianCalendar

fun dateToText(time:Long, hours: Int=0, minutes:Int=0):String{
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

//fun Calendar.toDateOf():DateOf{
//    return DateOf(this.timeInMillis,this.get(Calendar.HOUR_OF_DAY),this.get(Calendar.MINUTE))
//}

fun Calendar.dateOnly(): Calendar {
    this.set(Calendar.HOUR_OF_DAY,0)
    this.set(Calendar.MINUTE,0)
    this.set(Calendar.SECOND,0)
    this.set(Calendar.MILLISECOND,0)
  return this
}

fun Long.addDays(count:Int):Long{
  return GregorianCalendar().apply {
    timeInMillis=this@addDays
    add(Calendar.DAY_OF_YEAR,count)
  }.timeInMillis
}


inline fun <reified T : Parcelable> Bundle.getParcelableCompat(str:String): T? {
  return if(android.os.Build.VERSION_CODES.TIRAMISU <= android.os.Build.VERSION.SDK_INT){
    getParcelable(str, T::class.java)
  }else{
    //tiramisu things
    @Suppress("DEPRECATION")
    getParcelable(str)
  }
}

const val dbFile = "DateAndNote.db"
const val settingsFile = "DateAndNote.json"