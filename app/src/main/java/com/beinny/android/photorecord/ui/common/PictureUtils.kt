package com.beinny.android.photorecord

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.media.ExifInterface
import android.graphics.Matrix

private const val GET_BIMAP_ORIGIN = 11 // 이미지 비트맵 불러오기
private const val GET_BIMAP_RESIZE = 12 // 이미지 비트맵 (RESIZED) 불러오기

/** [화면 크기를 확인하고 해당 크기에맞춰 오버로딩된 함수를 호출] */
fun getScaledBitmap(path: String, activity: Activity, option: Int): Bitmap {
    val size = Point() // x,y 좌표

    @Suppress("DEPRECATION")
    activity.windowManager.defaultDisplay.getSize(size)

    if (option == GET_BIMAP_ORIGIN) {
        return getScaledBitmap(path, size.x, size.y)
    } else {
        return getScaledBitmap(path, size.x / 2, size.y / 2)
    }
}

/** [화면 크기를 확인하고 해당 크기에맞춰 오버로딩된 함수를 호출] */
fun getScaledBitmap(path: String, destWidth: Int, destHeight: Int): Bitmap {
    var options = BitmapFactory.Options()
    options.inJustDecodeBounds = true // true로 설정시 Bitmap 객체를 얻을때 메모리 할당하지 않고 해당 이미지의 width, height, MimeType 등 정보를 가져올 수 있다 (OutOfMemory Exception 방지)
    BitmapFactory.decodeFile(path, options) // Bitmap 객체를 얻는다

    val srcWidth = options.outWidth.toFloat()
    val srcHeight = options.outHeight.toFloat()

    // 크기를 얼마나 줄일지 파악
    var inSampleSize = 1
    if (srcHeight > destHeight || srcWidth > destWidth) {
        val heightScale = srcHeight / destHeight
        val widthScale = srcWidth / destWidth

        val sampleScale = if (heightScale > widthScale) {
            heightScale
        } else {
            widthScale
        }
        inSampleSize = Math.round(sampleScale)
    }

    options = BitmapFactory.Options() //새로운 Options 객체
    options.inSampleSize = inSampleSize

    // 이미지 회전현상 해결
    val exifDegree = getExifDegree(path)
    return rotate(BitmapFactory.decodeFile(path, options), exifDegree)
}

/** [이미지 회전현상 해결] */
fun getExifDegree(path: String): Int {
    val exif = path?.let { ExifInterface(it) }
    val exifOrientation: Int = exif!!.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    return exifOrientationToDegrees(exifOrientation)
}

/** [이미지 회전현상 해결] */
fun exifOrientationToDegrees(exifOrientation: Int): Int {
    if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
        return 90
    } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
        return 180
    } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
        return 270
    }
    return 0
}

/** [bitmap 회전] */
fun rotate(bitmap: Bitmap, degrees: Int): Bitmap { // 이미지 회전 및 이미지 사이즈 압축
    var bitmap = bitmap
    if (degrees != 0 && bitmap != null) {
        val m = Matrix()
        m.setRotate(
            degrees.toFloat(), bitmap.width.toFloat(),
            bitmap.height.toFloat()
        )
        try {
            val converted = Bitmap.createBitmap(
                bitmap, 0, 0,
                bitmap.width, bitmap.height, m, true
            )
            if (bitmap != converted) {
                bitmap.recycle()
                bitmap = converted
                //val options = BitmapFactory.Options()
                //options.inSampleSize = 4
                //bitmap = Bitmap.createScaledBitmap(bitmap, 1280, 1280, true) // 이미지 사이즈 줄이기
            }
        } catch (ex: OutOfMemoryError) {
            // 메모리가 부족하여 회전을 시키지 못할 경우 그냥 원본을 반환합니다.
        }
    }
    return bitmap
}

/** [bitmap 사이즈 조절] */
fun resizeBitmapToBitmap(bitmap: Bitmap, maxResolution: Int): Bitmap {
    val width: Int = bitmap.width
    val height: Int = bitmap.height
    var newWidth = width
    var newHeight = height
    var rate = 0.0f

    if (width > height) {
        if (maxResolution < width) {
            rate = maxResolution / width.toFloat()
            newHeight = (height * rate).toInt()
            newWidth = maxResolution
        }
    } else {
        if (maxResolution < height) {
            rate = maxResolution / height.toFloat()
            newWidth = (width * rate).toInt()
            newHeight = maxResolution
        }
    }

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}