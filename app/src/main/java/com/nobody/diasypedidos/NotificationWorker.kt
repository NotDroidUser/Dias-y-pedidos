package com.nobody.diasypedidos

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nobody.diasypedidos.db.DateAndNotesDatabase
import com.nobody.diasypedidos.db.DateAndNotesRepo

class NotificationWorker(val context:Context, params:WorkerParameters):Worker(context,params) {
  companion object{
    val NOTIFICATION_ID=123123
    val NOTIFICATION_CHANNEL="BATDIASYPEDIDOS"
  }
  
  private fun getTomorrowNotes(context: Context):Long{
    return DateAndNotesRepo(DateAndNotesDatabase.getInstance(context)).getTomorrowNotes()
  }
  
  override fun doWork(): Result {
    try {
      val bm = getSystemService(context,BatteryManager::class.java) as BatteryManager
      val count = getTomorrowNotes(context)
      if(count>0) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
          Toast.makeText(context, context.getString(R.string.tomorrow_count, count.toString()), Toast.LENGTH_SHORT).show()
        } else {
          NotificationManagerCompat.from(context).notify(NOTIFICATION_ID,
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
              .setSmallIcon(R.mipmap.ic_launcher)
              .setContentText(context.getString(R.string.tomorrow_count, count.toString()) +
                if (bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) <= 40) {
                  " y recomendable poner a cargar el telefono"
                } else "")
              .setContentTitle("RECUERDA")
              .setPriority(NotificationCompat.PRIORITY_HIGH)
              .setContentIntent(PendingIntent.getActivity(applicationContext,
                0, Intent(applicationContext, MainActivity::class.java), PendingIntent.FLAG_MUTABLE))
              .build())
        }
      }
      return Result.success()
    }catch (e:Exception) {
      return Result.failure()
    }
  }
  
}