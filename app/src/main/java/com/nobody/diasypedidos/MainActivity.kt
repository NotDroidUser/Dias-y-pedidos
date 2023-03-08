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
        Log.i(TAG, "onActivityResult: Aqui en el data")
        dataIs.extras?.let { extras ->
          Log.i(TAG, "onActivityResult: Aqui en el extras")
          val old = extras.getParcelable<DateAndNote>(OLDNOTE)
          val new = extras.getParcelable<DateAndNote>(DATENOTE)
          val editable = extras.getBoolean(EDITNOTE, false)
          if (editable && new != null) {
            (binding.recycle.adapter as DateAndNoteAdapter).submitList(
              loadData(this).list.toMutableList().apply {
                if (old != null) {
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
          Log.e(TAG, "onActivityResult: "+(actualData).list.joinToString() )
          saveDataExternally(actualData, contentResolver.openOutputStream(uri)!!)
          Toast.makeText(this, "Datos exportados", Toast.LENGTH_SHORT).show()
        } catch (ex: Exception) {
          Toast.makeText(this, "No se pudo abrir el archivo para exportar", Toast.LENGTH_SHORT).show()
          Log.e(TAG, "onActivityResult: No se pudo abrir el archivo para exportar\n"+ex.stackTraceToString())
        }
      }
    } else if (requestCode == REQUEST_SAF_IMPORT && resultCode == RESULT_OK) {
      data?.data?.let { uri ->
        try {
          val dataLoaded = loadDataExternally(contentResolver.openInputStream(uri)!!)
          val list = dataLoaded.list
          loadData(this).list.toMutableList().apply {
            addAll(list)
          }.also {
            saveData(DateAndNoteSaver(it), this)
          }
          Toast.makeText(this, "Datos Importados", Toast.LENGTH_SHORT).show()
          Log.e(TAG, "onActivityResult: "+(dataLoaded).list.joinToString(), )
        } catch (ex: Exception) {
          Toast.makeText(this, "No se pudo abrir el archivo para importar", Toast.LENGTH_SHORT).show()
          Log.e(TAG, "onActivityResult: No se pudo abrir el archivo para importar\n"+ex.stackTraceToString())
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
    const val DAYSEE = "DAYSEE"
    const val REQUEST_NOTE = 643
    private const val REQUEST_SAF_EXPORT: Int = 123123
    private const val REQUEST_SAF_IMPORT: Int = 123124
    
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
      else -> super.onOptionsItemSelected(item)
    }
  }
  
  private fun exportData() {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
      .setType("application/json")
      .addCategory(Intent.CATEGORY_OPENABLE)
    
    startActivityForResult(intent, REQUEST_SAF_EXPORT)
  }
  
  private fun importData() {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
      .setType("application/json")
      .addCategory(Intent.CATEGORY_OPENABLE)
    
    startActivityForResult(intent, REQUEST_SAF_IMPORT)
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
  
  override fun viewItem(item: DateAndNote) {
  }
}