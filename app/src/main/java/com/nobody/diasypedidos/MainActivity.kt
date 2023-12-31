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
import androidx.activity.result.contract.ActivityResultContracts
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
  private val binding: ActivityMainBinding by lazy {
    ActivityMainBinding.inflate(layoutInflater)
  }
  
  companion object {
    private const val TAG: String = "MainActivity"
    const val DATENOTE = "DATE&NOTE"
    const val OLDNOTE = "OLDNOTE"
    const val NEWPATH = "NEWPATH"
    const val DAYSEE = "DAYSEE"
    const val DEBUG=true
  }
  
  private val notificationPermission=(registerForActivityResult(ActivityResultContracts.RequestPermission()){ result->
    if(!result){
      Toast.makeText(this, "Se utilizaran estos carteles", Toast.LENGTH_SHORT).show();
    }
  })
  
  private val newEditNoteContract=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
    if (result.resultCode == RESULT_OK) {
      result.data?.let { dataIs ->
        dataIs.extras?.let { extras ->
          val old = extras.getParcelableCompat<DateAndNote>(OLDNOTE)
          val new = extras.getParcelableCompat<DateAndNote>(DATENOTE)
          if ( new != null) {
            (binding.recycle.adapter as DateAndNoteAdapter).submitList(
              loadData(this).list.toMutableList().also{
                if(DEBUG){
                  Log.i(TAG, "onActivityResult: currentList:${(binding.recycle.adapter as DateAndNoteAdapter).currentList}" )
                  Log.i(TAG, "onActivityResult: savedList:$it")
                  Log.i(TAG, "onActivityResult: old:$old")
                  Log.i(TAG, "onActivityResult: new:$new")
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
    }
  }
  
  private val exportData= registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
    if ( result.resultCode == RESULT_OK) {
      result.data?.data?.let { uri ->
        try {
          val actualData = loadData(this)
          if (DEBUG) {
            Log.e(TAG, "onActivityResult: "+(actualData).list.joinToString() )
          }
          saveDataExternallyOld(actualData, contentResolver.openOutputStream(uri)!!)
          Toast.makeText(this, "Datos exportados", Toast.LENGTH_SHORT).show()
        } catch (ex: Exception) {
          if (DEBUG) {
            Log.e(TAG, "onActivityResult: No se pudo abrir el archivo para exportar\n"+ex.stackTraceToString())
          }
          Toast.makeText(this, "No se pudo abrir el archivo para exportar", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }
  
  private val importData=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
    if ( result.resultCode == RESULT_OK) {
      result.data?.data?.let { uri ->
        try {
          val dataLoaded = loadDataExternallyOld(contentResolver.openInputStream(uri)!!)
          val list = dataLoaded.list
          loadData(this).list.toMutableList().apply {
            addAll(list)
          }.also {
            saveData(DateAndNoteSaver(it), this)
          }
          updateList()
          if (DEBUG) {
            Log.e(TAG, "onActivityResult: "+(dataLoaded).list.joinToString())
          }
          Toast.makeText(this, "Datos Importados", Toast.LENGTH_SHORT).show()
        } catch (ex: Exception) {
          if (DEBUG) {
            Log.e(TAG, "onActivityResult: No se pudo abrir el archivo para importar\n"+ex.stackTraceToString())
          }
          Toast.makeText(this, "No se pudo abrir el archivo para importar", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }
  
  private val exportNewData=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
    if(result.resultCode == RESULT_OK){
      result.data?.data?.let {uri->
        try {
          saveDataExternally(contentResolver.openOutputStream(uri)!!,this)
        }catch (ex: Exception) {
          if (DEBUG) {
            Log.e(TAG, "onActivityResult: No se pudo abrir el archivo para exportar\n"+ex.stackTraceToString())
          }
          Toast.makeText(this, "No se pudo abrir el archivo para exportar", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }
  
  private val importNewData=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
    if(result.resultCode == RESULT_OK){
      result.data?.data?.let {uri->
        try {
          if(verifyFile(contentResolver.openInputStream(uri)!!)){
            loadDataExternally(contentResolver.openInputStream(uri)!!,this)
            updateList()
            Toast.makeText(this, "Datos Importados", Toast.LENGTH_SHORT).show()
          } else {
            Toast.makeText(this, "El archivo no es un backup de este programa", Toast.LENGTH_SHORT).show();
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
      newEditNoteContract.launch(Intent(this@MainActivity, AddActivity::class.java))
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
          datePicker { _, datetime ->
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
        exportData.launch(Intent(Intent.ACTION_CREATE_DOCUMENT)
          .setType("application/json")
          .addCategory(Intent.CATEGORY_OPENABLE))
        true
      }
      R.id.action_import -> {
        importData.launch(Intent(Intent.ACTION_OPEN_DOCUMENT)
          .setType("application/json")
          .addCategory(Intent.CATEGORY_OPENABLE))
        true
      }
      R.id.action_export_new-> {
        exportNewData.launch(Intent(Intent.ACTION_CREATE_DOCUMENT)
          .setType("application/zip")
          .addCategory(Intent.CATEGORY_OPENABLE))
        true
      }
      R.id.action_import_new-> {
        importNewData.launch(Intent(Intent.ACTION_OPEN_DOCUMENT)
          .setType("application/zip")
          .addCategory(Intent.CATEGORY_OPENABLE))
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }
  
  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putLong(DAYSEE, daysee)
  }
  
  private fun updateList() {
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
      if(item.picturePath.isNotBlank())
        if(File(item.picturePath).exists())
          File(item.picturePath).delete()
      remove(item)
    }.sortedBy {
      it.hours
    }.also {list ->
      saveData(DateAndNoteSaver(list), this)
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
    newEditNoteContract.launch(
      Intent(this@MainActivity, AddActivity::class.java).apply {
        this.putExtra(DATENOTE, item)
      })
  }
  
  override fun modifyItem(old: DateAndNote, isChecked: Boolean) {
    val dateAndNoteAdapter = binding.recycle.adapter as DateAndNoteAdapter
    val c = loadData(this).list.toMutableList().apply {
      this.remove(old)
      old.finished=isChecked
      this.add(old)
    }.sortedBy {
      it.hours
    }.also { list ->
      saveData(DateAndNoteSaver(list), this)
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
      Intent(this@MainActivity, ViewNoteActivity::class.java).apply {
        this.putExtra(DATENOTE, item)
      })
  }
  
  override fun viewPicture(item: DateAndNote) {
    startActivity(
      Intent(this,ViewPictureActivity::class.java)
        .apply {
          putExtra("path",item.picturePath)
        }
    )
  }
}