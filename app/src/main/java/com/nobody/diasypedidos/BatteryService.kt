package com.nobody.diasypedidos

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.PermissionChecker
import java.util.*

@Suppress("NonAsciiCharacters")
class BatteryService : Service() {
  private var lastBattery=-1
  private lateinit var bm: BatteryManager
  private lateinit var notificationManager: NotificationManager
  var count = 0
  private lateinit var looper:HandlerThread
  private lateinit var handler:HandleService
  
  
  companion object{
    var isAlreadyRunning = false
    const val ACTION_START="com.nobody.diasypedidos.start"
    const val ACTION_STOP="com.nobody.diasypedidos.stop"
    const val NOTIFICATION_ID=123123
    const val NOTIFICATION_CHANNEL="BATDIASYPEDIDOS"
  }
  
  override fun onBind(intent: Intent?): IBinder? {
    return null
  }
  
  inner class HandleService(private val loop:Looper):Handler(loop){
    
    override fun handleMessage(msg: Message) {
      super.handleMessage(msg)
      if (msg.data?.getString(ACTION_STOP)!=null){
        loop.quitSafely()
        return
      }
      else {
        count++
        if (MainActivity.DEBUG) {
          Log.e("HandleService", "handleMessage: $count")
        }
        if (count == 10) {
          count = 0
          doNotificationWork()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && PermissionChecker.checkSelfPermission(applicationContext,android.Manifest.permission.POST_NOTIFICATIONS)!=PermissionChecker.PERMISSION_GRANTED) {
          this.sendMessageDelayed(Message(), 50000)
        }else{
          this.sendMessageDelayed(Message(), 10000)
        }
      }
    }
  }
  
  override fun onCreate() {
    bm = getSystemService(BATTERY_SERVICE) as BatteryManager
    notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    if (MainActivity.DEBUG) {
      Log.e("BatteryService", "onCreate: creating service" )
    }
    super.onCreate()
  }
  
  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    
    super.onStartCommand(intent, flags, startId)
    if (MainActivity.DEBUG) {
      Log.i("BatteryService", "onStartCommand: started service for see battery" +
        "level and remember on percentage/3==0" )
    }
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
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && PermissionChecker.checkSelfPermission(applicationContext,android.Manifest.permission.POST_NOTIFICATIONS)!=PermissionChecker.PERMISSION_GRANTED) {
            Toast.makeText(this, "Tienes $count pendientes para mañana", Toast.LENGTH_SHORT).show()
          }
          else{
            doNotification(battery, count)
          }
        }
      }
    }
  }
  
  @SuppressLint("MissingPermission")
  fun doNotification(battery:Int,count:Int){
    NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID,
      NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentText("Tienes $count pendientes para mañana" + if (battery < 40) {
          " y pon a cargar"
        } else "")
        .setContentTitle("RECUERDA")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(PendingIntent.getActivity(applicationContext,
          0,Intent(applicationContext,MainActivity::class.java),PendingIntent.FLAG_MUTABLE))
        .build())
  }
  
  override fun onDestroy() {
    if (::handler.isInitialized)
      handler.sendMessage(Message().apply {  data.putString(ACTION_STOP, ACTION_STOP)})
    if (count>1)
      Toast.makeText(applicationContext, "Recuerda revisar los pedidos de mañana ", Toast.LENGTH_SHORT).show()
    super.onDestroy()
  }
}