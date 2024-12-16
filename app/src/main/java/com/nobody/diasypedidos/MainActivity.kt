package com.nobody.diasypedidos

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.datetime.datePicker
import com.nobody.diasypedidos.databinding.ActivityMainBinding
import com.nobody.diasypedidos.db.DateAndNoteDB
import com.nobody.diasypedidos.db.DateAndNotesDatabase
import com.nobody.diasypedidos.vm.DateAndNoteViewModel
import com.nobody.diasypedidos.vm.DateAndNoteViewModelFactory
import java.io.File
import java.util.GregorianCalendar
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

class MainActivity : AppCompatActivity(), DateAndNoteHandler {

  private val binding: ActivityMainBinding by lazy {
    ActivityMainBinding.inflate(layoutInflater)
  }

  private val gestureDetector by lazy{
    GestureDetector(this,OnSwipeDetector())
  }
  
  private val settings by lazy {
    getSharedPreferences("Settings", Context.MODE_PRIVATE)
  }
  
  private val vm: DateAndNoteViewModel by viewModels<DateAndNoteViewModel>{
    DateAndNotesDatabase.getInstance(this)
    DateAndNoteViewModelFactory
  }
  
  private val importDialog:AlertDialog by lazy {
    AlertDialog.Builder(this).apply {
      this.setMessage("Se estan importando los datos desde el archivo, este dialogo se cerrara solo, cuando termine el proceso.")
      this.setCancelable(false)
      this.setTitle("Importando datos")
    }.create()
  }

  private val exportDialog:AlertDialog by lazy {
    AlertDialog.Builder(this).apply {
      this.setMessage("Se estan exportando los datos desde la aplicacion, este dialogo se cerrara solo, cuando termine el proceso.")
      this.setCancelable(false)
      this.setTitle("Exportando datos")
    }.create()
  }
  
  companion object {
    private const val TAG: String = "MainActivity"
    const val DATENOTE = "DATE&NOTE"
    const val OLDNOTE = "OLDNOTE"
    const val DEBUG=false
    const val NOTIFICATION_DIALOG="NOTIFICATION_DIALOG"
    const val BATTERY_DIALOG="BATTERY_DIALOG"
  }
  
  private val notificationPermission = (registerForActivityResult(ActivityResultContracts.RequestPermission()){ result->
    if(!result){
      Toast.makeText(this, "Se utilizaran Toasts", Toast.LENGTH_SHORT).show()
    }
  })
  
  @SuppressLint("BatteryLife")
  private val batterySaverPermission = (registerForActivityResult(ActivityResultContracts.RequestPermission()){ result->
    if(result){
      val pm = getSystemService(POWER_SERVICE) as PowerManager
      if(!pm.isIgnoringBatteryOptimizations(packageName)){
        Intent().apply {
          setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
          setData(Uri.parse("package:$packageName"))
          startActivity(this)
        }
      }
    }
  })
  
  private val newEditNoteContract=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
    if (result.resultCode == RESULT_OK) {
      result.data?.let { dataIsNotNull ->
        dataIsNotNull.extras?.let { extras ->
          val old = extras.getParcelableCompat<DateAndNoteDB>(OLDNOTE)
          val new = extras.getParcelableCompat<DateAndNoteDB>(DATENOTE)
          if ( new != null && old != null) {
            if(new.picturePath!=old.picturePath && File(old.picturePath).exists()) {
              File(old.picturePath).delete()
            }
            if(old.id!=null){
              vm.editNote( new.apply { id = old.id })
            }
            else{
              vm.addNote(new)
            }
          } else throw IllegalArgumentException(" The \"new one\" must be a note")
        }
      }
    }
  }
  
  private val exportJson= registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
    if ( result.resultCode == RESULT_OK) {
      result.data?.data?.let { uri ->
        try {
          vm.saveDataToJson(contentResolver.openOutputStream(uri)!!)
        } catch (ex: Exception) {
          if (DEBUG) {
            Log.e(TAG, "onActivityResult: No se pudo abrir el archivo para exportar\n"+ex.stackTraceToString())
          }
          Toast.makeText(this, "No se pudo abrir el archivo para exportar", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }
  
  private val importJson=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
    if ( result.resultCode == RESULT_OK) {
      result.data?.data?.let { uri ->
        try {
          vm.importDataFromJson(contentResolver.openInputStream(uri)!!,filesDir)
        } catch (ex: Exception) {
          if (DEBUG) {
            Log.e(TAG, "onActivityResult: No se pudo abrir el archivo para importar\n"+ex.stackTraceToString())
          }
          Toast.makeText(this, "No se pudo abrir el archivo para importar", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }
  
  private val exportZip=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
    if(result.resultCode == RESULT_OK){
      result.data?.data?.let {uri->
        try {
          vm.saveDataToZip(contentResolver.openOutputStream(uri)!!,this.filesDir)
        } catch (ex: Exception) {
          if (DEBUG) {
            Log.e(TAG, "onActivityResult: No se pudo abrir el archivo para exportar\n"+ex.stackTraceToString())
          }
          Toast.makeText(this, "No se pudo abrir el archivo para exportar", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }
  
  private val importZip=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
    if(result.resultCode == RESULT_OK){
      result.data?.data?.let {uri->
        try {
          vm.loadDataFromZipAndImport(contentResolver.openInputStream(uri)!!,contentResolver.openInputStream(uri)!!,this.filesDir){
            Toast.makeText(this,
              "Fallo la importacion, verifique el archivo para importar sea valido",
              Toast.LENGTH_SHORT).show()
          }
        }catch (ex:Exception){
          if (DEBUG) {
            Log.e(TAG, "onActivityResult: No se pudo abrir el archivo para importar\n"+ex.stackTraceToString())
          }
          Toast.makeText(this, "No se pudo abrir el archivo para importar", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }
  
  override fun onTouchEvent(event: MotionEvent): Boolean {
    return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
  }
  
  fun swipeToRight(){
    binding.nextDayButton.callOnClick()
  }
  
  fun swipeToLeft(){
    binding.backDayButton.callOnClick()
  }
  
  inner class OnSwipeDetector:GestureDetector.OnGestureListener{
    
    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
      val diffx=(e1?:e2).x -e2.x
      val diffy= ((e1?:e2).y -e2.y)
      if(DEBUG){
        Log.e(TAG, "onFling: ${diffx.absoluteValue}" )
      }
      if(diffy.absoluteValue<diffx.absoluteValue&&diffx.absoluteValue>100 && velocityX.absoluteValue >100){
          if(diffx>0){
            swipeToRight()
            return true
          }else{
            swipeToLeft()
            return true
          }
      }
      
      return false
    }
    
    override fun onDown(e: MotionEvent)=false
    override fun onShowPress(e: MotionEvent) {}
    override fun onSingleTapUp(e: MotionEvent)=false
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float)=false
    override fun onLongPress(e: MotionEvent) {}

  }
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    WorkManager.getInstance(this)
      .enqueueUniquePeriodicWork(
        "Notifications",
        ExistingPeriodicWorkPolicy.UPDATE,
        PeriodicWorkRequestBuilder<NotificationWorker>(
          repeatInterval = 4,
          repeatIntervalTimeUnit =  TimeUnit.HOURS
        ).setInitialDelay(1,TimeUnit.HOURS).build()
      )
    
    if(DEBUG){
      settings.edit().putBoolean(NOTIFICATION_DIALOG,false).apply()
      settings.edit().putBoolean(BATTERY_DIALOG,false).apply()
    }
    
    with(binding) {
      
      setContentView(root)
      
      daySeeText.also {
        it.setOnClickListener { showSelectDayDialog() }
      }.text = dateToText(vm.daySeeing)
      
      val dateAndNoteAdapter = DateAndNoteAdapter(this@MainActivity)
      recycle.layoutManager = LinearLayoutManager(this@MainActivity)
      recycle.adapter = dateAndNoteAdapter
      
      fab.setOnClickListener {
        newEditNoteContract.launch(Intent(this@MainActivity, DateAndNoteAddActivity::class.java))
      }
      
      nextDayButton.setOnClickListener {
        vm.daySeeing = vm.daySeeing.addDays(1)
        daySeeText.text = dateToText(vm.daySeeing)
      }
      
      backDayButton.setOnClickListener {
        vm.daySeeing = vm.daySeeing.addDays(-1)
        daySeeText.text = dateToText(vm.daySeeing)
      }
      
      with(vm) {
        val contextFiles = this@MainActivity.filesDir
        
        File(contextFiles, ".importing").apply {
          if (!exists()) {
            File(contextFiles, settingsFile).let { settings ->
              if (settings.exists()) {
                vm.importOldData(openFileInput(settingsFile),this@MainActivity.filesDir)
                Toast.makeText(this@MainActivity, getString(R.string.import_progress), Toast.LENGTH_SHORT).show()
              }
            }
          } else {
            
            delete()
            
            File(contextFiles, settingsFile).let {
              if(exists())
                delete()
            }
            
            AlertDialog.Builder(this@MainActivity).setMessage(getString(R.string.import_error)).create().also { dial ->
              Thread {
                Thread.sleep(2000)
                if (dial.isShowing) {
                  dial.hide()
                }
              }
            }.show()
            
          }
        }
        
        list.observe(this@MainActivity) { list ->
          dateAndNoteAdapter.submitList(list)
        }
        
        importing.observe(this@MainActivity) { importing ->
          if (importing) {
            importDialog.show()
          } else {
            importDialog.cancel()
          }
        }
        
        exporting.observe(this@MainActivity) { exporting ->
          if (exporting) {
            exportDialog.show()
          } else {
            exportDialog.cancel()
          }
        }
      }
    }
    
    
    //TODO Just in case that going below M again
    @SuppressLint("ObsoleteSdkInt")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !settings.getBoolean(BATTERY_DIALOG,false)) {
      AlertDialog.Builder(this).setMessage(getString(R.string.dialog_battery_service)).setPositiveButton("Si"){ dial, _->
        batterySaverPermission.launch(android.Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        dial.dismiss()
        settings.edit().putBoolean(BATTERY_DIALOG,true).apply()
      }.setNegativeButton("No") { dial, _ ->
        dial.dismiss()
        settings.edit().putBoolean(BATTERY_DIALOG,true).apply()
      }.show()
    }
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationManagerCompat.from(this)
        .createNotificationChannel(
          NotificationChannel(NotificationWorker.NOTIFICATION_CHANNEL,
            "Pedidos",
            NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&  !settings.getBoolean(NOTIFICATION_DIALOG,false)) {
      AlertDialog.Builder(this).setMessage(getString(R.string.notification_dialog)).setPositiveButton("Si"){ dial, _->
        notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        dial.dismiss()
        settings.edit().putBoolean(NOTIFICATION_DIALOG,true).commit()
      }.setNegativeButton("No") { dial, _ -> dial
        .dismiss()
        settings.edit().putBoolean(NOTIFICATION_DIALOG,true).commit()
      }.show()
    }
    
//    if (!BatteryService.isAlreadyRunning)
//      startService(Intent(this, BatteryService::class.java).apply { action = BatteryService.ACTION_START })
  }
  
  
  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    MenuInflater(this).inflate(R.menu.main, menu)
    return super.onCreateOptionsMenu(menu)
  }
  
  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.action_goto_day -> {
        vm.daySeeing = GregorianCalendar().dateOnly().timeInMillis
        binding.daySeeText.text = dateToText(vm.daySeeing)
        true
      }
      R.id.action_goto_date -> {
        showSelectDayDialog()
        true
      }
      R.id.action_export -> {
        exportJson.launch(Intent(Intent.ACTION_CREATE_DOCUMENT)
          .setType("application/json")
          .addCategory(Intent.CATEGORY_OPENABLE))
        true
      }
      R.id.action_export_new-> {
        exportZip.launch(Intent(Intent.ACTION_CREATE_DOCUMENT)
          .setType("application/zip")
          .addCategory(Intent.CATEGORY_OPENABLE))
        true
      }
      R.id.action_import -> {
        importJson.launch(Intent(Intent.ACTION_OPEN_DOCUMENT)
          .setType("application/json")
          .addCategory(Intent.CATEGORY_OPENABLE))
        true
      }
      R.id.action_import_new-> {
        importZip.launch(Intent(Intent.ACTION_OPEN_DOCUMENT)
          .setType("application/zip")
          .addCategory(Intent.CATEGORY_OPENABLE))
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }
  
  private fun showSelectDayDialog() {
    MaterialDialog(this).show {
      datePicker { _, datetime ->
        vm.daySeeing = datetime.let {
          it.dateOnly()
          it.timeInMillis
        }
        binding.daySeeText.text = dateToText(vm.daySeeing)
      }
    }
  }
  
  override fun removeItem(item: DateAndNoteDB) {
    vm.delete(item)
  }
  
  override fun editItem(item: DateAndNoteDB) {
    newEditNoteContract.launch(
      Intent(this@MainActivity, DateAndNoteAddActivity::class.java).apply {
        this.putExtra(DATENOTE, item)
      }
    )
  }
  
  override fun modifyItem(old: DateAndNoteDB, isChecked: Boolean) {
    vm.editNote(old.apply { finished=isChecked })
  }
  
  override fun viewItem(item: DateAndNoteDB) {
    startActivity(
      Intent(this@MainActivity, ViewNoteActivity::class.java).apply {
        this.putExtra(DATENOTE, item)
      })
  }
  
  override fun viewPicture(item: DateAndNoteDB) {
    startActivity(
      Intent(this,ViewPictureActivity::class.java)
        .apply {
          putExtra("path",item.picturePath)
        }
    )
  }
}