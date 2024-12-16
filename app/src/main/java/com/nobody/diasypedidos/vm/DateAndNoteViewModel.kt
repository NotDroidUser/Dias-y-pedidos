package com.nobody.diasypedidos.vm

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nobody.diasypedidos.db.DateAndNoteSaver
import com.nobody.diasypedidos.dateOnly
import com.nobody.diasypedidos.db.DateAndNoteDB
import com.nobody.diasypedidos.db.DateAndNotesDatabase
import com.nobody.diasypedidos.db.DateAndNotesRepo
import com.nobody.diasypedidos.settingsFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.EOFException
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.GregorianCalendar
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


@Suppress("UNCHECKED_CAST")
object DateAndNoteViewModelFactory: ViewModelProvider.Factory {
  lateinit var INSTANCE: DateAndNoteViewModel
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if(!DateAndNoteViewModelFactory::INSTANCE.isInitialized){
      INSTANCE = DateAndNoteViewModel(DateAndNotesRepo(DateAndNotesDatabase.getInstance()))
    }
    return INSTANCE as T
  }
}

const val importFilename=".importing"

class DateAndNoteViewModel(private val repo: DateAndNotesRepo): ViewModel() {
  private val json = Json { ignoreUnknownKeys = true }
  
  private val vmScope= CoroutineScope(Dispatchers.IO)
  val list: MutableLiveData<List<DateAndNoteDB>> = MutableLiveData(listOf())
  var daySeeing:Long=GregorianCalendar().apply { dateOnly() }.timeInMillis
    set(value) {
      field=value
      update()
    }
  
  var importing = MutableLiveData(false)
  var exporting = MutableLiveData(false)
  
  init {
      update()
  }
  
  private fun update(){
    vmScope.launch {
      list.postValue(repo.getNoteInDay(daySeeing).sortedBy {
        it.minutes
      }.sortedBy {
        it.hours
      })
    }
  }
  
  fun addNote(note: DateAndNoteDB){
    vmScope.launch {
      repo.addNote(note)
      update()
    }
  }
  
  fun editNote(note: DateAndNoteDB){
    vmScope.launch {
      repo.editNote(note)
      update()
    }
  }
  
  fun delete(note: DateAndNoteDB){
    vmScope.launch {
      repo.deleteNote(note)
      File(note.picturePath).apply {
        if(exists()){
          delete()
        }
      }
      update()
    }
  }
  
  fun saveDataToJson(out: OutputStream) {
    vmScope.launch {
      exporting.postValue(true)
      val saved = json.encodeToString(DateAndNoteSaver.serializer(), DateAndNoteSaver(repo.getNote()))
      out.writer().buffered().also {
        it.write(saved)
        it.flush()
        it.close()
      }
      exporting.postValue(false)
    }
  }
  
  fun saveDataToZip(out: OutputStream, filesDir: File) {
    vmScope.launch {
      exporting.postValue(true)
      val zipOut = ZipOutputStream(out)
      val saved = json.encodeToString(DateAndNoteSaver.serializer(), DateAndNoteSaver(repo.getNote()))
      zipOut.putNextEntry(ZipEntry(settingsFile))
      zipOut.write(saved.encodeToByteArray())
      zipOut.closeEntry()
      filesDir.listFiles { dir, name ->
        !File(dir, name).isDirectory
      }?.forEach {
        zipOut.putNextEntry(ZipEntry(it.name))
        zipOut.write(it.readBytes())
        zipOut.closeEntry()
      }
      zipOut.flush()
      zipOut.close()
      exporting.postValue(false)
    }
  }
  
  fun importOldData(input: InputStream, filesDir: File){
    vmScope.launch {
      importing.postValue(true)
      val data=loadDBDataFromJson(input)
      importData(data.list,filesDir)
      File(filesDir, settingsFile).apply {
        if(exists()){
          delete()
        }
      }
    }
  }
  
  fun importDataFromJson(input: InputStream,filesDir: File){
    vmScope.launch {
      importing.postValue(true)
      val data=loadDBDataFromJson(input)
      importData(data.list,filesDir)
    }
  }
  
  fun importData(list: List<DateAndNoteDB>, filesDir: File) {
    vmScope.launch {
      val importFile=File(filesDir, importFilename).apply {
        if(!exists()){
          createNewFile()
        }
      }
      repo.getNote().forEach{ note-> repo.deleteNote(note) }
      list.forEach{note -> repo.addNote(note)}
      update()
      importFile.delete()
      importing.postValue(false)
    }
  }
  
  fun loadDataFromZipAndImport(verify: InputStream, read: InputStream, filesDir: File, onFailure:()->Unit) {
    vmScope.launch (Dispatchers.IO){
      importing.postValue(true)
      //TODO this keep waiting on FM for some reason
      Thread.sleep(100)
      if(verifyZipFile(verify)){
        loadDataFromZip(read, filesDir)
        importData(loadDBDataFromJson(File(filesDir, settingsFile).inputStream()).list,filesDir)
        File(filesDir, settingsFile).apply {
          if(exists()){
            delete()
          }
        }
      }
      else{
        launch(Dispatchers.Main){ onFailure() }
      }
    }
  }
  
  fun loadDataFromZip(input: InputStream, contextPathFile:File) {
    clearOldFiles(contextPathFile)
    val zipArchiveInputStream= ZipInputStream(input)
    var entry = zipArchiveInputStream.nextEntry
    try {
      while (entry!=null){
        val file = File(contextPathFile,entry.name).apply {
          if(!exists()){
            createNewFile()
          }
        }.outputStream()
        zipArchiveInputStream.copyTo(file)
        file.flush()
        file.close()
        entry = zipArchiveInputStream.nextEntry
      }
    }catch (ex: IOException){
      Log.e("LOAD FILE:", "loadDataExternally Failed: ${ex.stackTrace}")
    }
  }
  
  fun verifyZipFile(input: InputStream): Boolean {
    val zipArchiveInputStream= ZipInputStream(input)
    var isBackup=false
    var entry = zipArchiveInputStream.nextEntry
    try {
      while (entry!=null){
        isBackup = entry.name.contains(settingsFile)
        entry = zipArchiveInputStream.nextEntry
        if(isBackup){
          break
        }
      }
    }catch (_: IOException){
    
    }
    return isBackup
  }
  
  
  fun clearOldFiles(contextPathFile:File){
    for (i in contextPathFile.listFiles()?: arrayOf()){
      try {
        i.delete()
      }
      catch (_: EOFException){}
      catch (_: FileNotFoundException){}
    }
  }
  
  fun loadDBDataFromJson(input: InputStream): DateAndNoteSaver {
    return try {
      val toLoad = input.reader().buffered().readText()
      json.decodeFromString(DateAndNoteSaver.serializer(),toLoad)
    }catch(e: FileNotFoundException){
      DateAndNoteSaver(listOf())
    }
  }
  
}