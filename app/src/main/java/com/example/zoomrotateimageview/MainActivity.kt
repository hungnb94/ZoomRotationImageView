package com.example.zoomrotateimageview

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream


class MainActivity : AppCompatActivity() {
    private val TAG = "MainTouchImage"

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.haizewang_90)
        btnSave.setOnClickListener {
            if (ivShowResult.visibility == View.VISIBLE) {
                ivShowResult.visibility = View.GONE
                imageLayout.visibility = View.VISIBLE
            } else {
                ivShowResult.visibility = View.VISIBLE
                imageLayout.visibility = View.GONE
                val output = imageLayout.getOutputBitmap()
                ivShowResult.setImageBitmap(output)
                Log.e(TAG, "Input(${bitmap.width}, ${bitmap.height}, ${getBitmapSize(bitmap)})" +
                        " vs Output(${output.width}, ${output.height}, ${getBitmapSize(output)})")
            }
        }
    }

    private fun getBitmapSize(bitmap: Bitmap): Long {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val imageInByte = stream.toByteArray()
        val size = imageInByte.size.toLong()
        stream.close()
        return size
    }
}
