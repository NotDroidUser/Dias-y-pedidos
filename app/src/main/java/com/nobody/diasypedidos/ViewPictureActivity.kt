package com.nobody.diasypedidos

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nobody.diasypedidos.databinding.ViewPictureBinding

class ViewPictureActivity: AppCompatActivity() {
  
  private val binding:ViewPictureBinding by lazy {
    ViewPictureBinding.inflate(layoutInflater)
  }
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    this.setContentView(binding.root)
    val path= intent.extras?.getString("path")
    if (path!=null){
      if (path==""){
        Toast.makeText(this, "No se encontro la foto", Toast.LENGTH_SHORT).show();
        finish()
      }
      binding.imageView2.setImageDrawable(Drawable.createFromPath(path))
    }else
      finish()
  }
}