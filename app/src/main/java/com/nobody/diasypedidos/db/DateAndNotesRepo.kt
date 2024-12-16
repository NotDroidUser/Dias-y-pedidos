package com.nobody.diasypedidos.db

import com.nobody.diasypedidos.addDays
import com.nobody.diasypedidos.dateOnly
import java.util.Calendar
import java.util.GregorianCalendar

class DateAndNotesRepo(db: DateAndNotesDatabase) {
  private val notesDao: DateAndNoteDBDao = db.getNotesDAO()
  
  
  fun getNote() = notesDao.getDateAndNoteDB()
  
  fun getNoteInDay(day: Long): List<DateAndNoteDB> {
    return notesDao.getDateAndNoteInRange(day,day.addDays(1))
  }
  
  fun addNote(note: DateAndNoteDB) {
    notesDao.insertDateAndNoteDB(note)
  }
  
  fun editNote(note: DateAndNoteDB) {
    notesDao.updateDateAndNoteDB(note)
  }
  
  fun deleteNote(note: DateAndNoteDB) {
    notesDao.deleteDateAndNoteDB(note)
  }
  fun getTomorrowNotes():Long{
    return notesDao.getRangeWorksCount(
      //Tomorrow
      GregorianCalendar().dateOnly().apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis,
      //after that
      GregorianCalendar().dateOnly().apply { add(Calendar.DAY_OF_YEAR, 2) }.timeInMillis
    )
  }
  
  
}