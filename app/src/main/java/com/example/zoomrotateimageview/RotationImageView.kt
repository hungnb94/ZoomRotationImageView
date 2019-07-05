package com.example.zoomrotateimageview

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import com.bumptech.glide.Glide
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
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
    private var bitmapDrawable: BitmapDrawable? = null

    var autoScale = false

    private var imgWidth = 0
    private var imgHeight = 0
    private var srcImg: Drawable? = null
    private var targetWidth = 0
    private var targetHeight = 0

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RotationImageView)

        autoScale = typedArray.getBoolean(R.styleable.RotationImageView_autoScale, false)
        val drawable = typedArray.getDrawable(R.styleable.RotationImageView_srcImage)
        if (drawable != null) setImage(drawable)
        targetWidth = typedArray.getDimensionPixelSize(R.styleable.RotationImageView_targetWidth, 0)
        targetHeight = typedArray.getDimensionPixelSize(R.styleable.RotationImageView_targetHeight, 0)

        typedArray.recycle()
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return false

        val isMatrixScaleType = scaleType == ScaleType.MATRIX

        val scale: Float

        // Handle touch events here...
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN //first finger down only
            -> {
                if (isMatrixScaleType) {
                    mSavedMatrix.set(mMatrix)
                } else {
                    mSavedMatrix.set(imageMatrix)
                }
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
        if (!isMatrixScaleType) scaleType = ScaleType.MATRIX

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
        if (targetWidth != 0 && targetHeight != 0) {
            Log.d(TAG, "Set dimension base on target width & target height")
            val specWidth = MeasureSpec.getSize(widthMeasureSpec)
            val specHeight = MeasureSpec.getSize(heightMeasureSpec)

            val ratioW = specWidth / targetWidth.toFloat()
            val ratioH = specHeight / targetHeight.toFloat()

            val targetW: Int
            val targetH: Int
            if (ratioW > ratioH) {
                targetW = (this.targetWidth * ratioH).toInt()
                targetH = specHeight
            } else {
                targetW = specWidth
                targetH = (this.targetHeight * ratioW).toInt()
            }
            setMeasuredDimension(targetW, targetH)
        } else if (autoScale && drawableWidth != null && drawableHeight != null) {
            Log.d(TAG, "Auto scale dimension")
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
            setMeasuredDimension(targetW, targetH)
        } else {
            Log.d(TAG, "Default measure")
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    @Deprecated("Use setImage instead")
    override fun setImageBitmap(bmp: Bitmap?) {
        super.setImageBitmap(bmp)
    }


    fun setImage(bmp: Bitmap?) {
        bitmapDrawable?.bitmap?.recycle()
        bitmapDrawable = BitmapDrawable(resources, bmp)

        setImage(bitmapDrawable)
    }

    @Deprecated("Use setImage instead")
    override fun setImageIcon(icon: Icon?) {
//        super.setImageIcon(icon)
    }

    @Deprecated("Use setImage instead")
    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
    }

    fun setImage(drawable: Drawable?) {
//        setImageDrawable(drawable)
        Glide.with(this)
                .load(drawable)
                .centerInside()
                .into(this)

        srcImg = drawable
        if (drawable != null) {
            imgWidth = drawable.intrinsicWidth
            imgHeight = drawable.intrinsicHeight
            //Set bound for drawable
            srcImg?.setBounds(0, 0, imgWidth, imgHeight)
        } else {
            imgWidth = 0
            imgHeight = 0
        }
    }

    @Deprecated("Use setImageBitmap instead")
    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
    }

    @Deprecated("Use setImageBitmap instead")
    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
    }

    fun setTargetDimension(width: Int, height: Int) {
        autoScale = false
        targetWidth = width
        targetHeight = height
        requestLayout()
    }

    fun getOutputBitmap(): Bitmap? {
        val drawable = srcImg ?: return null

        val dwidth = getDrawable().intrinsicWidth
        val dheight = getDrawable().intrinsicHeight

        val width = width
        val height = height
        Log.e(TAG, "Target: ($targetWidth, $targetHeight), Auto: $autoScale")

        val targetW: Int
        val targetH: Int
        if (targetWidth == 0 || targetHeight == 0) {
            if (autoScale) {
                targetW = drawable.intrinsicWidth
                targetH = drawable.intrinsicHeight
            } else {
                val ratio = max(drawable.intrinsicWidth.toFloat() / width,
                        drawable.intrinsicHeight.toFloat() / height)
                targetW = (width * ratio).toInt()
                targetH = (height * ratio).toInt()
            }
        } else {
            targetW = targetWidth
            targetH = targetHeight
        }

        val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val matrix = Matrix(imageMatrix)
        //First scale down to current visible drawable
        matrix.preScale(dwidth / imgWidth.toFloat(), dheight / imgHeight.toFloat())

        //Scale to target size
        val ratioW = targetW.toFloat() / width
        val ratioH = targetH.toFloat() / height
        matrix.postScale(ratioW, ratioH)

        //Draw to bitmap
        canvas.concat(matrix)
        drawable.draw(canvas)
        return bitmap
    }

}