package com.nobody.diasypedidos

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.*
//import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.*


class BatteryService : Service() {
  var lastBattery=-1
  lateinit var bm: BatteryManager
  lateinit var notificationManager: NotificationManager
  var count = 0
  lateinit var looper:HandlerThread
  lateinit var handler:HandleService
  
  
  companion object{
    var isAlreadyRunning = false
    val ACTION_START="com.nobody.diasypedidos.start"
    val ACTION_STOP="com.nobody.diasypedidos.stop"
    const val NOTIFICATION_ID=123123
    const val NOTIFICATION_CHANNEL="BATDIASYPEDIDOS"
  }
  
  override fun onBind(intent: Intent?): IBinder? {
    return null
  }
  
  inner class HandleService(loop:Looper):Handler(loop){
    
    override fun handleMessage(msg: Message) {
      super.handleMessage(msg)
      if (msg.data?.getString(ACTION_STOP)!=null){
        this@BatteryService.looper.quitSafely()
        return
      }
      else {
        count++
        //Log.e("", "onStartCommand: $count")
        if (count == 10) {
          count = 0
          doNotificationWork()
        }
        this.sendMessageDelayed(Message(), 10000)
      }
    }
  }
  
  override fun onCreate() {
  
    bm = getSystemService(BATTERY_SERVICE) as BatteryManager
    notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    //Log.e("some", "onStartCommand: A" )
  
    super.onCreate()
  }
  
  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    
    super.onStartCommand(intent, flags, startId)
    //Log.i("some", "Battery remember: started" )
    if(!isAlreadyRunning){
      isAlreadyRunning=true
      if (intent?.action==null && lastBattery==-1){
        doBatteryWork()
        doNotificationWork()
      }
    }
    if (intent?.action!=null){
      when(intent.action){
        ACTION_START->{
          doBatteryWork()
          //doNotificationWork()
        }
        ACTION_STOP->{
          this.stopSelf()
        }
        else->{}
      }
    }
    looper=HandlerThread("").apply {
      start()
      HandleService(looper).sendMessage(Message())
    }
    return START_STICKY
  }
  
  private fun doBatteryWork() {
    lastBattery=bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
  }
  
  private fun doNotificationWork() {
    if(lastBattery!=bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)) {
      val battery= bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    
      if (battery%3==0){
        val dayToSee: Long= GregorianCalendar().apply { setCalendarDateOnly();add(Calendar.DAY_OF_YEAR,1) }.timeInMillis
        val dayAfter: Long= GregorianCalendar().apply { setCalendarDateOnly();add(Calendar.DAY_OF_YEAR,2) }.timeInMillis
        val count = loadData(applicationContext).list.count { it.time in (dayToSee + 1) until dayAfter }
        if (count >0) {
          NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID,
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL)
              .setSmallIcon(R.mipmap.ic_launcher)
              .setContentText("Tienes $count pendientes para mañana" + if (battery < 40) {
                " y pon a cargar"
              } else "")
              .setContentTitle("RECUERDA")
              .setPriority(NotificationCompat.PRIORITY_HIGH)
              .setContentIntent(PendingIntent.getActivity(applicationContext,
                0,Intent(applicationContext,MainActivity::class.java),0))
              .build())
        }
      }
    }
  }
  
  override fun onDestroy() {
    if (::handler.isInitialized)
      handler.sendMessage(Message().apply {  data.putString(ACTION_STOP, ACTION_STOP)})
    if (count>1)
      Toast.makeText(this, "Recuerda revisar los pedidos de mañana ", Toast.LENGTH_SHORT).show()
    super.onDestroy()
  }
}