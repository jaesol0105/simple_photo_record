package com.beinny.android.photorecord.ui.common

import android.app.Application
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.beinny.android.photorecord.GlideApp
import com.beinny.android.photorecord.PhotoRecordApplication
import com.beinny.android.photorecord.common.GET_BIMAP_ORIGIN
import com.beinny.android.photorecord.getScaledBitmap
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import java.io.File

private val filesDir = PhotoRecordApplication.applicationContext().applicationContext.filesDir //*

@BindingAdapter("imageUrl")
fun loadImage(view: ImageView, imageUrl: String?) {
    if (!imageUrl.isNullOrEmpty()) {
        val photoFile = File(filesDir, imageUrl)
        if (photoFile.exists()) { // 사진파일이 존재할 경우
            GlideApp.with(view)
                .load(photoFile)
                .apply(
                    RequestOptions()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                )
                .into(view)
        }
    }
}


@BindingAdapter("thumbnailImageUrl")
fun loadThumbnailImage(view: ImageView, imageUrl: String?) {
    if (!imageUrl.isNullOrEmpty()) {
        val thumbFile = File(filesDir, imageUrl)
        if (thumbFile.exists()) { // 사진파일이 존재할 경우
            GlideApp.with(view)
                .load(thumbFile)
                .apply(
                    RequestOptions()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                )
                .into(view)
        }
    }
}