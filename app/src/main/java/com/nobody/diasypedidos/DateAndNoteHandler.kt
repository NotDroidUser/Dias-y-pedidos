package com.nobody.diasypedidos

import com.nobody.diasypedidos.db.DateAndNoteDB

interface DateAndNoteHandler{
    fun removeItem(item: DateAndNoteDB)
    fun editItem(item: DateAndNoteDB)
    fun modifyItem(old: DateAndNoteDB, isChecked:Boolean)
    fun viewPicture(item: DateAndNoteDB)
    fun viewItem(item: DateAndNoteDB)
}