package com.nobody.diasypedidos.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DateAndNoteDBDao {
  @Query("select * from DateAndNoteDB")
  fun getDateAndNoteDB(): List<DateAndNoteDB>
  
  @Query("Select * from DateAndNoteDB where time >= :date and time < :datePlusOne ")
  fun getDateAndNoteInRange(date:Long, datePlusOne:Long):List<DateAndNoteDB>
  
  @Query("Select count(id) from DateAndNoteDB where time >= :date and time < :datePlusOne ")
  fun getRangeWorksCount(date:Long, datePlusOne:Long):Long
  
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  fun insertDateAndNoteDB(note: DateAndNoteDB)
  
  @Update
  fun updateDateAndNoteDB(note: DateAndNoteDB)
  
  @Delete
  fun deleteDateAndNoteDB(note: DateAndNoteDB)
  
}