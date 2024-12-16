package com.nobody.diasypedidos

import com.google.gson.GsonBuilder
import com.nobody.diasypedidos.db.DateAndNoteSaver
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readText
import kotlin.io.path.writeBytes

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

data class DateAndNoteOld(var time: Long,
                       var hours: Int = 0,
                       var minutes: Int =0,
                       var text: String,
                       var direction: String = "",
                       var app:String = "")

class DateAndNoteSaverOld(var list:List<DateAndNoteOld>)

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }
  
  //loaded Data has a null reference https://stackoverflow.com/a/76224866
  @Test
  fun test_Json_Converting(){
    val toSave = DateAndNoteSaverOld(listOf(DateAndNoteOld(time=0L,text="test")))
    val saved = GsonBuilder().setPrettyPrinting().create().toJson(toSave)
    val tempFile = kotlin.io.path.createTempFile(prefix = "test", suffix = "json")
    tempFile.writeBytes(saved.toByteArray())
    val loaded2 = Json.decodeFromString<DateAndNoteSaver>(tempFile.readText())
    assertEquals("loaded Data isn't null","",loaded2.list[0].picturePath)
    tempFile.deleteIfExists()
  }
}