package com.example.zoomrotateimageview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView

class ImageLayout @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val TAG = "ImageLayout"

    var imageView: RotationImageView
    private var imageWidth = 0
    private var imageHeight = 0
    private val layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

    init {
        layoutParams.gravity = Gravity.CENTER
        imageView = RotationImageView(context)
        imageView.background = ColorDrawable(Color.GREEN)
        imageView.scaleType = ImageView.ScaleType.MATRIX

        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.ImageLayout)
        val drawable = typeArray.getDrawable(R.styleable.ImageLayout_src)
        typeArray.recycle()

        post {
            addView(imageView, layoutParams)
            if (drawable != null) setImageDrawable(drawable)
        }
    }

//    @RequiresApi(Build.VERSION_CODES.M)
//    fun setImageIcon(icon: Icon?) {
//        imageView?.setImageIcon(icon)
//    }

    fun setImageBitmap(bitmap: Bitmap?) {
        if (bitmap != null) updateLayoutParam(bitmap.width, bitmap.height)

        imageView.setImageBitmap(bitmap)
    }

    fun setImageDrawable(drawable: Drawable?) {
        if (drawable != null) updateLayoutParam(drawable.intrinsicWidth, drawable.intrinsicHeight)

        imageView.setImageDrawable(drawable)
    }

    fun setImageResource(resId: Int) {
        val option = BitmapFactory.Options()
        option.inJustDecodeBounds = true
        val bitmap = BitmapFactory.decodeResource(resources, resId, option)
        val width = option.outWidth
        val height = option.outHeight
        bitmap.recycle()

        updateLayoutParam(width, height)

        imageView.setImageResource(resId)
    }

    fun setImageURI(uri: Uri?) {
        if (uri != null) {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            updateLayoutParam(bitmap.width, bitmap.height)
            bitmap.recycle()
        }
        imageView.setImageURI(uri)
    }

//    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
//        if (imageWidth != 0 && imageHeight != 0) {
//            Log.e(TAG, "update from measure")
//            updateLayoutParam(imageWidth, imageHeight)
//        }
//    }

    private fun updateLayoutParam(imgWidth: Int, imgHeight: Int) {
        imageWidth = imgWidth
        imageHeight = imgHeight

        val specWidth = measuredWidth
        val specHeight = measuredHeight
        if (specWidth != 0 || specHeight != 0) {
            val ratioW = specWidth / imgWidth.toFloat()
            val ratioH = specHeight / imgHeight.toFloat()
            val targetW: Int
            val targetH: Int
            if (ratioW > ratioH) {
                targetW = (imgHeight * ratioH).toInt()
                targetH = specHeight
            } else {
                targetW = specWidth
                targetH = (imgWidth * ratioW).toInt()
            }
            layoutParams.width = targetW
            layoutParams.height = targetH

            imageView.layoutParams = layoutParams
            Log.e(TAG, "Update layout done")
        } else {
            Log.e(TAG, "Update layout failed")
        }
    }
}