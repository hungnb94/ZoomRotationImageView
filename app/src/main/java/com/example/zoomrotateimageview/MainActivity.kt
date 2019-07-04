package com.example.zoomrotateimageview

import android.graphics.*
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import kotlin.math.atan2
import kotlin.math.sqrt


class MainActivity : AppCompatActivity(), View.OnTouchListener {
    private val TAG = "MainTouchImage"

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.haizewang_90)

//        btnSave.setOnClickListener {
//            if (ivShowResult.visibility == View.VISIBLE) {
//                ivShowResult.visibility = View.GONE
//                imageLayout.visibility = View.VISIBLE
//            } else {
//                val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
//                val canvas = Canvas(output)
//                val matrix = Matrix(imageLayout.imageView.imageMatrix)
//                val ratioW = bitmap.width.toFloat() / imageLayout.imageView.width
//                val ratioH = bitmap.height.toFloat() / imageLayout.imageView.height
//                matrix.postScale(ratioW, ratioH)
//                canvas.drawBitmap(bitmap, matrix, null)
//                ivShowResult.visibility = View.VISIBLE
//                imageLayout.visibility = View.GONE
//                ivShowResult.setImageBitmap(output)
//                Log.e(TAG, "Input(${bitmap.width}, ${bitmap.height}, ${getBitmapSize(bitmap)})" +
//                        " vs Output(${output.width}, ${output.height}, ${getBitmapSize(output)})")
//            }
//        }


//        layoutImage.post {
//            val specWidth = layoutImage.width
//            val specHeight = layoutImage.height
//            val drawableWidth = bitmap.width
//            val drawableHeight = bitmap.height
//            val ratioW = specWidth / drawableWidth.toFloat()
//            val ratioH = specHeight / drawableHeight.toFloat()
//            val targetW: Int
//            val targetH: Int
//            if (ratioW > ratioH) {
//                targetW = (drawableWidth * ratioH).toInt()
//                targetH = specHeight
//            } else {
//                targetW = specWidth
//                targetH = (drawableHeight * ratioW).toInt()
//            }
//            val params = imageLayout.layoutParams
//            params.width = targetW
//            params.height = targetH
//            imageLayout.layoutParams = params
//        }
//        imageLayout.setOnTouchListener(this)
        btnSave.setOnClickListener {
            if (ivShowResult.visibility == View.VISIBLE) {
                ivShowResult.visibility = View.GONE
                imageLayout.visibility = View.VISIBLE
            } else {
                val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                val matrix = Matrix(imageLayout.imageMatrix)
                val ratioW = bitmap.width.toFloat() / imageLayout.width
                val ratioH = bitmap.height.toFloat() / imageLayout.height
                matrix.postScale(ratioW, ratioH)
                canvas.drawBitmap(bitmap, matrix, null)
                ivShowResult.visibility = View.VISIBLE
                imageLayout.visibility = View.GONE
                ivShowResult.setImageBitmap(output)
                Log.e(TAG, "Input(${bitmap.width}, ${bitmap.height}, ${getBitmapSize(bitmap)})" +
                        " vs Output(${output.width}, ${output.height}, ${getBitmapSize(output)})")
            }
        }
    }


    // These matrices will be used to move and zoom image
    private var mMatrix = Matrix()
    private var mSavedMatrix = Matrix()

    // We can be in one of these 3 states
    val NONE = 0
    val DRAG = 1
    val ZOOM = 2
    var mode = NONE

    // Remember some things for zooming
    private var mStart = PointF()
    private var mMid = PointF()
    private var mOldDistance = 1f
    private var mLastEvent: FloatArray? = null
    private var mRotation = 0f

    override fun onTouch(v: View, event: MotionEvent): Boolean {
//        if (v == null || event == null) return false

        val imageView = v as ImageView
        if (imageView.scaleType != ImageView.ScaleType.MATRIX) imageView.scaleType = ImageView.ScaleType.MATRIX

        val scale: Float

        // Handle touch events here...
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN //first finger down only
            -> {
                mSavedMatrix.set(mMatrix)
                mStart[event.x] = event.y
                mode = DRAG
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                mOldDistance = spacing(event)
                if (mOldDistance > 10f) {
                    mSavedMatrix.set(mMatrix)
                    midPoint(mMid, event)
                    mode = ZOOM
                }
                mLastEvent = FloatArray(4)
                mLastEvent!![0] = event.getX(0)
                mLastEvent!![1] = event.getX(1)
                mLastEvent!![2] = event.getY(0)
                mLastEvent!![3] = event.getY(1)
                mRotation = rotation(event)
            }

            MotionEvent.ACTION_UP //first finger lifted
                , MotionEvent.ACTION_POINTER_UP //second finger lifted
            -> {
                mode = NONE
            }


            MotionEvent.ACTION_MOVE -> if (mode == DRAG) {
                // ...
                mMatrix.set(mSavedMatrix)
                mMatrix.postTranslate(event.x - mStart.x, event.y - mStart.y)
            } else if (mode == ZOOM && event.pointerCount == 2) {
                val newDist = spacing(event)
                mMatrix.set(mSavedMatrix)
                if (newDist > 10f) {
                    scale = newDist / mOldDistance
                    mMatrix.postScale(scale, scale, mMid.x, mMid.y)
                }
                if (mLastEvent != null) {
                    val newRot = rotation(event)
                    val diff = newRot - mRotation
                    mMatrix.postRotate(
                            diff, imageView.measuredWidth / 2f,
                            imageView.measuredHeight / 2f
                    )
                }
            }
        }
        // Perform the transformation
        imageView.imageMatrix = mMatrix

        return true // indicate event was handled
    }


    private fun rotation(event: MotionEvent): Float {
        val deltaX = (event.getX(0) - event.getX(1)).toDouble()
        val deltaY = (event.getY(0) - event.getY(1)).toDouble()
        val radians = atan2(deltaY, deltaX)

        return Math.toDegrees(radians).toFloat()
    }

    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt(x * x + y * y)
    }

    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }

    private fun getBitmapSize(bitmap: Bitmap): Long {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val imageInByte = stream.toByteArray()
        return imageInByte.size.toLong()
    }
}
