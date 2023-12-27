package com.nobody.diasypedidos

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nobody.diasypedidos.databinding.ActivityViewNoteBinding
import java.io.File

class ViewNoteActivity :AppCompatActivity(){
  private lateinit var date: DateAndNote
  private val binding:ActivityViewNoteBinding by lazy {  ActivityViewNoteBinding.inflate(layoutInflater) }
  
  @SuppressLint("SetTextI18n")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(binding.root)
    intent.extras?.let { bundle ->
      bundle.getParcelableCompat<DateAndNote>(MainActivity.DATENOTE)?.let {
        date = it.copy()
      }
    }
    savedInstanceState?.let{ state ->
      state.getParcelableCompat<DateAndNote>(MainActivity.DATENOTE)?.let {
        date = it.copy()
      }
    }
    if(!::date.isInitialized){
        finish()
    }
    with(binding){
      if (date.picturePath != "" && File(date.picturePath).exists()) {
        imageView.setImageDrawable(Drawable.createFromPath(date.picturePath))
      } else {
        imageView.setImageResource(R.mipmap.placeholder)
      }
      imageView.setOnClickListener {
        startActivity(
          Intent(this@ViewNoteActivity,ViewPictureActivity::class.java)
            .apply {
              putExtra("path",date.picturePath)
            }
        )
      }
      messageText.text = date.text
      addressText.text = date.direction
      appText.text = date.app
      DateOf(date.time,date.hours,0).toText().also {
        dateText.text=it
      }
      if(date.finished){
        finishedText.text = "Finalizado"
      }else{
        finishedText.text = "Pendiente"
      }
    }
  }
  
  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putParcelable(MainActivity.DATENOTE,date)
  }
  
}