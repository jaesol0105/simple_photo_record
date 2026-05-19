package com.beinny.android.photorecord

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Point
import android.media.ExifInterface
import com.beinny.android.photorecord.common.GET_BITMAP_ORIGIN

/** 화면 크기에 맞춰 스케일된 Bitmap 반환 */
fun getScaledBitmap(path: String, activity: Activity, option: Int): Bitmap {
    val size = Point() // x,y 좌표

    @Suppress("DEPRECATION")
    activity.windowManager.defaultDisplay.getSize(size)

    return if (option == GET_BITMAP_ORIGIN) {
        getScaledBitmap(path, size.x, size.y)
    } else {
        getScaledBitmap(path, size.x / 2, size.y / 2)
    }
}

/** 지정한 크기에 맞춰 스케일된 Bitmap 반환 */
fun getScaledBitmap(path: String, destWidth: Int, destHeight: Int): Bitmap {
    var options = BitmapFactory.Options()
    options.inJustDecodeBounds = true // 실제 디코딩 없이 이미지 크기만 읽음 (OOM 방지)
    BitmapFactory.decodeFile(path, options)

    val srcWidth = options.outWidth.toFloat()
    val srcHeight = options.outHeight.toFloat()

    // 크기를 얼마나 줄일지 파악
    var inSampleSize = 1
    if (srcHeight > destHeight || srcWidth > destWidth) {
        val heightScale = srcHeight / destHeight
        val widthScale = srcWidth / destWidth
        inSampleSize = Math.round(if (heightScale > widthScale) heightScale else widthScale)
    }

    options = BitmapFactory.Options()
    options.inSampleSize = inSampleSize

    // 이미지 회전현상 해결
    return rotate(BitmapFactory.decodeFile(path, options), getExifDegree(path))
}

/** EXIF 정보에서 회전 각도 추출 */
fun getExifDegree(path: String): Int {
    val exif = ExifInterface(path)
    val exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    return exifOrientationToDegrees(exifOrientation)
}

/** EXIF orientation 상수를 각도로 변환 */
fun exifOrientationToDegrees(exifOrientation: Int): Int {
    return when (exifOrientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
}

/** Bitmap 회전 */
fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
    var result = bitmap
    if (degrees != 0) {
        val m = Matrix()
        m.setRotate(degrees.toFloat(), bitmap.width.toFloat(), bitmap.height.toFloat())
        try {
            val converted = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
            if (bitmap != converted) {
                bitmap.recycle()
                result = converted
            }
        } catch (ex: OutOfMemoryError) {
            // OOM 발생 시 원본 반환
        }
    }
    return result
}

/** 최대 해상도 기준으로 Bitmap 크기 조절 */
fun resizeBitmapToBitmap(bitmap: Bitmap, maxResolution: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    var newWidth = width
    var newHeight = height

    if (width > height) {
        if (maxResolution < width) {
            val rate = maxResolution / width.toFloat()
            newHeight = (height * rate).toInt()
            newWidth = maxResolution
        }
    } else {
        if (maxResolution < height) {
            val rate = maxResolution / height.toFloat()
            newWidth = (width * rate).toInt()
            newHeight = maxResolution
        }
    }

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}
