package com.nobody.diasypedidos.db

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "DateAndNoteDB")
@Serializable
data class DateAndNoteDB(var time: Long,
                         var hours: Int = 0,
                         var minutes: Int = 0,
                         var text: String,
                         @SerialName("direction")
                         var address: String = "",
                         var app: String = "",
                         @SerialName("pic")
                         var picturePath: String = "",
                         @SerialName("")
                         var finished: Boolean = false) : Parcelable {
  @PrimaryKey(autoGenerate = true)
  var id: Long? = null
  
  constructor(parcel: Parcel) : this(
    parcel.readLong(),
    parcel.readInt(),
    parcel.readInt(),
    parcel.readString() ?: "",
    parcel.readString() ?: "",
    parcel.readString() ?: "",
    parcel.readString() ?: "",
    parcel.readByte() != 0.toByte()) {
    id = parcel.readLong()
    if (id == -1L) {
      id = null
    }
  }
  
  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeLong(time)
    parcel.writeInt(hours)
    parcel.writeInt(minutes)
    parcel.writeString(text)
    parcel.writeString(address)
    parcel.writeString(app)
    parcel.writeString(picturePath)
    parcel.writeByte(if (finished) 1 else 0)
    if (id != null) {
      parcel.writeLong(id!!)
    } else {
      parcel.writeLong(-1L)
    }
  }
  
  override fun describeContents(): Int {
    return 0
  }
  
  companion object CREATOR : Parcelable.Creator<DateAndNoteDB> {
    override fun createFromParcel(parcel: Parcel): DateAndNoteDB {
      return DateAndNoteDB(parcel)
    }
    
    override fun newArray(size: Int): Array<DateAndNoteDB?> {
      return arrayOfNulls(size)
    }
  }
}


