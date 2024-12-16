package com.nobody.diasypedidos.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nobody.diasypedidos.dbFile

@Database(entities = [DateAndNoteDB::class], version = 1)
abstract class DateAndNotesDatabase : RoomDatabase() {
  abstract fun getNotesDAO(): DateAndNoteDBDao
  
  companion object {
    @Volatile
    lateinit var INSTANCE: DateAndNotesDatabase
    
    fun getInstance(context: Context): DateAndNotesDatabase {
      if (!Companion::INSTANCE.isInitialized) {
        INSTANCE = Room.databaseBuilder(context, DateAndNotesDatabase::class.java, dbFile).build()
      }
      return INSTANCE
    }
    
    fun getInstance(): DateAndNotesDatabase {
      return INSTANCE
    }
  }
}