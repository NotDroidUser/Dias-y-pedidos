package com.nobody.diasypedidos

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity

class GetPictureContract:ActivityResultContract<Unit,Uri?>() {

    override fun createIntent(context: Context, input: Unit): Intent {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        return Intent.createChooser(intent, "Select Picture")
    }
    
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (resultCode == AppCompatActivity.RESULT_OK ) {
            intent?.data?.let { return it }
        }
        return null
    }
}