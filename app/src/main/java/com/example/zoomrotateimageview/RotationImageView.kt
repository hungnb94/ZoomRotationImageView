package com.example.zoomrotateimageview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import kotlin.math.atan2
import kotlin.math.sqrt

class RotationImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    private val TAG = "TouchImageView"

    // These matrices will be used to move and zoom image
    var mMatrix = Matrix()
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


    override fun onTouchEvent(event: MotionEvent): Boolean {
//        if (event == null) return false

        if (scaleType != ScaleType.MATRIX) scaleType = ScaleType.MATRIX

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
                        diff, measuredWidth / 2f,
                        measuredHeight / 2f
                    )
                }
            }
        }
        // Perform the transformation
        imageMatrix = mMatrix

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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val drawableWidth = drawable?.intrinsicWidth
        val drawableHeight = drawable?.intrinsicHeight
        if (drawableWidth != null && drawableHeight != null) {
            Log.e(TAG, "Custom width and height")
            val specWidth = MeasureSpec.getSize(widthMeasureSpec)
            val specHeight = MeasureSpec.getSize(heightMeasureSpec)
            val ratioW = specWidth / drawableWidth.toFloat()
            val ratioH = specHeight / drawableHeight.toFloat()
            val targetW: Int
            val targetH: Int
            if (ratioW > ratioH) {
                targetW = (drawableWidth * ratioH).toInt()
                targetH = specHeight
            } else {
                targetW = specWidth
                targetH = (drawableHeight * ratioW).toInt()
            }
            Log.e(TAG, "Drawable: $drawableWidth, $drawableHeight")
            Log.e(TAG, "Spec    : $specWidth, $specHeight")
            Log.e(TAG, "Target  : $targetW, $targetH")
            setMeasuredDimension(targetW, targetH)
        } else {
            Log.d(TAG, "Default measure")
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

//    override fun onDraw(canvas: Canvas) {
//        val mDrawable = drawable ?: return  // couldn't resolve the URI
//
////        if (imageMatrix == null) {
////            mDrawable.draw(canvas)
////        } else {
////        val saveCount = canvas.saveCount
//        canvas.save()
//
//        canvas.concat(mMatrix)
//        mDrawable.draw(canvas)
////        canvas.restoreToCount(saveCount)
////        }
//    }

}