package com.nobody.diasypedidos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.datetime.datePicker
import com.nobody.diasypedidos.databinding.ActivityMainBinding
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity(), DateAndNoteHandler {
  private var daysee: Long = GregorianCalendar().apply { setCalendarDateOnly() }.timeInMillis
  private val TAG: String? = MainActivity::class.simpleName
  private val binding: ActivityMainBinding by lazy {
    ActivityMainBinding.inflate(layoutInflater)
  }
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    savedInstanceState?.let {
      daysee = it.getLong(DAYSEE, daysee)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationManagerCompat.from(this).createNotificationChannel(NotificationChannel(BatteryService.NOTIFICATION_CHANNEL, "Pedidos", NotificationManager.IMPORTANCE_HIGH))
    }
    setContentView(binding.root)
    binding.daySeeText.text = DateOf(daysee).toText()
    val dateAndNoteAdapter = DateAndNoteAdapter(this)
    binding.recycle.layoutManager = LinearLayoutManager(this)
    binding.recycle.adapter = dateAndNoteAdapter
    dateAndNoteAdapter.submitList(loadData(this).list.filter {
      it.time > daysee &&
        GregorianCalendar().apply {
          timeInMillis = daysee
          add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis > it.time
    }.sortedBy {
      it.minutes
    }.sortedBy {
      it.hours
    })
    binding.fab.setOnClickListener {
      startActivityForResult(
        Intent(this@MainActivity, AddActivity::class.java),
        REQUEST_NOTE)
    }
    binding.nextDayButton.setOnClickListener {
      daysee = GregorianCalendar().apply {
        timeInMillis = daysee
        add(Calendar.DAY_OF_YEAR, 1)
      }.timeInMillis
      binding.daySeeText.text = DateOf(daysee).toText()
      updateList()
    }
    binding.backDayButton.setOnClickListener {
      daysee = GregorianCalendar().apply {
        timeInMillis = daysee
        add(Calendar.DAY_OF_YEAR, -1)
      }.timeInMillis
      binding.daySeeText.text = DateOf(daysee).toText()
      updateList()
    }
    if (!BatteryService.isAlreadyRunning)
      startService(Intent(this, BatteryService::class.java).apply { action = BatteryService.ACTION_START })
  }
  
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == REQUEST_NOTE && resultCode == RESULT_OK) {
      data?.let { dataIs ->
        dataIs.extras?.let { extras ->
          val old = extras.getParcelable<DateAndNote>(OLDNOTE)
          val new = extras.getParcelable<DateAndNote>(DATENOTE)
          val editable = extras.getBoolean(EDITNOTE, false)
          if (editable && new != null) {
            (binding.recycle.adapter as DateAndNoteAdapter).submitList(
              loadData(this).list.toMutableList().also{
                if(BuildConfig.DEBUG){
                  Log.e(TAG, "onActivityResult: currentList:${(binding.recycle.adapter as DateAndNoteAdapter).currentList}", )
                  Log.e(TAG, "onActivityResult: savedList:$it")
                  Log.e(TAG, "onActivityResult: old:$old")
                  Log.e(TAG, "onActivityResult: new:$new")
                }
              }.apply {
                if (old != null) {
                  if(new.picturePath!=old.picturePath) {
                    if(File(old.picturePath).exists())
                      File(old.picturePath).delete()
                  }
                  remove(old)
                }
                if (!this.contains(new))
                  add(new)
              }.sortedBy {
                it.hours
              }.also {
                saveData(DateAndNoteSaver(it), this)
              }.filter {
                it.time > daysee &&
                  GregorianCalendar().apply {
                    timeInMillis = daysee
                    add(Calendar.DAY_OF_YEAR, 1)
                  }.timeInMillis > it.time
              }
            )
          } else throw IllegalArgumentException(" The \"new\" must be a note")
        }
      }
    } else if (requestCode == REQUEST_SAF_EXPORT && resultCode == RESULT_OK) {
      data?.data?.let { uri ->
        try {
          val actualData = loadData(this)
          if (BuildConfig.DEBUG) {
            Log.e(TAG, "onActivityResult: "+(actualData).list.joinToString() )
          }
          saveDataExternallyOld(actualData, contentResolver.openOutputStream(uri)!!)
          Toast.makeText(this, "Datos exportados", Toast.LENGTH_SHORT).show()
        } catch (ex: Exception) {
          if (BuildConfig.DEBUG) {
            Log.e(TAG, "onActivityResult: No se pudo abrir el archivo para exportar\n"+ex.stackTraceToString())
          }
          Toast.makeText(this, "No se pudo abrir el archivo para exportar", Toast.LENGTH_SHORT).show()
        }
      }
    } else if (requestCode == REQUEST_SAF_IMPORT && resultCode == RESULT_OK) {
      data?.data?.let { uri ->
        try {
          val dataLoaded = loadDataExternallyOld(contentResolver.openInputStream(uri)!!)
          val list = dataLoaded.list
          loadData(this).list.toMutableList().apply {
            addAll(list)
          }.also {
            saveData(DateAndNoteSaver(it), this)
          }
          updateList()
          if (BuildConfig.DEBUG) {
            Log.e(TAG, "onActivityResult: "+(dataLoaded).list.joinToString(), )
          }
          Toast.makeText(this, "Datos Importados", Toast.LENGTH_SHORT).show()
        } catch (ex: Exception) {
          if (BuildConfig.DEBUG) {
            Log.e(TAG, "onActivityResult: No se pudo abrir el archivo para importar\n"+ex.stackTraceToString())
          }
          Toast.makeText(this, "No se pudo abrir el archivo para importar", Toast.LENGTH_SHORT).show()

        }
      }
    }else if(requestCode == REQUEST_SAF_EXPORT_NEW && resultCode == RESULT_OK){
      data?.data?.let {uri->
        try {
          saveDataExternally(contentResolver.openOutputStream(uri)!!,this)
        }catch (ex: Exception) {
          if (BuildConfig.DEBUG) {
            Log.e(TAG, "onActivityResult: No se pudo abrir el archivo para exportar\n"+ex.stackTraceToString())
          }
          Toast.makeText(this, "No se pudo abrir el archivo para exportar", Toast.LENGTH_SHORT).show()
        }
      }
    }else if(requestCode == REQUEST_SAF_IMPORT_NEW && resultCode == RESULT_OK){
      data?.data?.let {uri->
        try {
          if(verifyFile(contentResolver.openInputStream(uri)!!)){
            loadDataExternally(contentResolver.openInputStream(uri)!!,this)
            updateList()
            Toast.makeText(this, "Datos Importados", Toast.LENGTH_SHORT).show()
          }else {
            Toast.makeText(this, "El archivo no es un backup de este programa", Toast.LENGTH_SHORT).show();
          }
        }catch (ex:Exception){
          if (BuildConfig.DEBUG) {
            Log.e(TAG, "onActivityResult: No se pudo abrir el archivo para importar\n"+ex.stackTraceToString())
          }
          Toast.makeText(this, "No se pudo abrir el archivo para importar", Toast.LENGTH_SHORT).show()
      
        }
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data)
    }
  }
  
  companion object {
    const val DATENOTE = "DATE&NOTE"
    const val EDITNOTE = "EDITNOTE"
    const val OLDNOTE = "OLDNOTE"
    const val NEWPATH = "NEWPATH"
    const val DAYSEE = "DAYSEE"
    const val REQUEST_NOTE = 643
    private const val REQUEST_SAF_EXPORT: Int = 123123
    private const val REQUEST_SAF_IMPORT: Int = 123124
    private const val REQUEST_SAF_EXPORT_NEW: Int = 123125
    private const val REQUEST_SAF_IMPORT_NEW: Int = 123126
  
  
  }
  
  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    MenuInflater(this).inflate(R.menu.main, menu)
    return super.onCreateOptionsMenu(menu)
  }
  
  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.action_goto_day -> {
        daysee = GregorianCalendar().let {
          it.setCalendarDateOnly()
          it.timeInMillis
        }
        updateList()
        binding.daySeeText.text = DateOf(daysee).toText()
        true
      }
      R.id.action_goto_date -> {
        MaterialDialog(this).show {
          datePicker { dialog, datetime ->
            daysee = datetime.let {
              it.setCalendarDateOnly()
              it.timeInMillis
            }
            updateList()
            binding.daySeeText.text = DateOf(daysee).toText()
          }
        }
        true
      }
      R.id.action_export -> {
        exportData()
        true
      }
      R.id.action_import -> {
        importData()
        true
      }
      R.id.action_export_new-> {
        exportDataNew()
        true
      }
      R.id.action_import_new-> {
        importDataNew()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }
  
  private fun exportData() {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
      .setType("application/json")
      .addCategory(Intent.CATEGORY_OPENABLE)
    startActivityForResult(intent, REQUEST_SAF_EXPORT)
  }
  
  private fun exportDataNew() {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
      .setType("application/zip")
      .addCategory(Intent.CATEGORY_OPENABLE)
    startActivityForResult(intent, REQUEST_SAF_EXPORT_NEW)
  }
  
  private fun importData() {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
      .setType("application/json")
      .addCategory(Intent.CATEGORY_OPENABLE)
    
    startActivityForResult(intent, REQUEST_SAF_IMPORT)
  }
  
  
  private fun importDataNew() {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
      .setType("application/zip")
      .addCategory(Intent.CATEGORY_OPENABLE)
    
    startActivityForResult(intent, REQUEST_SAF_IMPORT_NEW)
  }
  
  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putLong(DAYSEE, daysee)
    
  }
  
  fun updateList() {
    val dateAndNoteAdapter = binding.recycle.adapter as DateAndNoteAdapter
    dateAndNoteAdapter.submitList(loadData(this).list.filter {
      it.time > daysee &&
        GregorianCalendar().apply {
          timeInMillis = daysee
          add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis > it.time
    }.sortedBy {
      it.hours
    })
  }
  
  override fun removeItem(item: DateAndNote) {
    val dateAndNoteAdapter = binding.recycle.adapter as DateAndNoteAdapter
    val c = loadData(this).list.toMutableList().apply {
      //TODO INVESTIGATE HOW FUCKING WAY IT IS GETTING NULL :|
      if(!item.picturePath.isNullOrBlank())
        if(File(item.picturePath).exists())
          File(item.picturePath).delete()
      remove(item)
    }.sortedBy {
      it.hours
    }.also {
      saveData(DateAndNoteSaver(it.sortedBy { it.time }), this)
    }.filter {
      it.time > daysee &&
        GregorianCalendar().apply {
          timeInMillis = daysee
          add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis > it.time
    }
    dateAndNoteAdapter.submitList(c)
  }
  
  override fun editItem(item: DateAndNote) {
    startActivityForResult(
      Intent(this@MainActivity, AddActivity::class.java).apply {
        this.putExtra(DATENOTE, item)
        this.putExtra(EDITNOTE, true)
      },
      REQUEST_NOTE)
  }
  
  override fun modifyItem(old: DateAndNote, isChecked: Boolean) {
    val dateAndNoteAdapter = binding.recycle.adapter as DateAndNoteAdapter
    val c = loadData(this).list.toMutableList().apply {
      this.remove(old)
      old.finished=isChecked
      this.add(old)
    }.sortedBy {
      it.hours
    }.also {
      saveData(DateAndNoteSaver(it.sortedBy { it.time }), this)
    }.filter {
      it.time > daysee &&
        GregorianCalendar().apply {
          timeInMillis = daysee
          add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis > it.time
    }
    dateAndNoteAdapter.submitList(c)
  }
  
  override fun viewItem(item: DateAndNote) {
    startActivity(
      Intent(this,ViewPictureActivity::class.java)
        .apply {
          putExtra("path",item.picturePath)
        }
    )
  }
}