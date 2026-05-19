package com.beinny.android.photorecord.ui.common

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.beinny.android.photorecord.GlideApp
import com.beinny.android.photorecord.PhotoRecordApplication
import com.beinny.android.photorecord.R
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import java.io.File
import java.util.*

private val filesDir = PhotoRecordApplication.applicationContext().applicationContext.filesDir //*

@BindingAdapter("imageUrl")
fun loadImage(view: ImageView, imageUrl: String?) {
    if (!imageUrl.isNullOrEmpty()) {
        val photoFile = File(filesDir, imageUrl)
        if (photoFile.exists()) {
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
        if (thumbFile.exists()) {
            GlideApp.with(view)
                .load(thumbFile)
                .apply(
                    RequestOptions()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                )
                .into(view)
        } else {
            view.setImageResource(0)
        }
    }
}

fun applyCheckAnimation(
    checkmarkView: ImageView,
    frameView: android.view.View?,
    oldChecked: Boolean,
    checked: Boolean,
    checkedDrawable: Int
) {
    checkmarkView.animate().cancel()
    if (checked) {
        checkmarkView.setImageResource(checkedDrawable)
        if (!oldChecked) {
            checkmarkView.scaleX = 0.3f
            checkmarkView.scaleY = 0.3f
            checkmarkView.alpha = 0f
            checkmarkView.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(260)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
            frameView?.also { frame ->
                frame.animate().cancel()
                frame.scaleX = 0f
                frame.scaleY = 0f
                frame.alpha = 0f
            }
        } else {
            checkmarkView.scaleX = 1f
            checkmarkView.scaleY = 1f
            checkmarkView.alpha = 1f
        }
    } else {
        if (oldChecked) {
            checkmarkView.animate()
                .scaleX(0.3f).scaleY(0.3f).alpha(0f)
                .setDuration(260)
                .setInterpolator(android.view.animation.AccelerateInterpolator())
                .withEndAction {
                    checkmarkView.setImageResource(0)
                    checkmarkView.scaleX = 1f
                    checkmarkView.scaleY = 1f
                    checkmarkView.alpha = 1f
                }
                .start()
            frameView?.also { frame ->
                frame.animate().cancel()
                frame.scaleX = 0.5f
                frame.scaleY = 0.5f
                frame.alpha = 0f
                frame.animate()
                    .scaleX(1f).scaleY(1f).alpha(1f)
                    .setDuration(260)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }
        } else {
            checkmarkView.setImageResource(0)
            checkmarkView.scaleX = 1f
            checkmarkView.scaleY = 1f
            checkmarkView.alpha = 1f
        }
    }
}

@BindingAdapter("checked")
fun applyCheckSign(view: ImageView, oldChecked: Boolean, checked: Boolean) {
    val frameView = (view.parent as? android.view.ViewGroup)
        ?.findViewById<android.view.View>(R.id.view_item_record_checkbox_frame)
    applyCheckAnimation(view, frameView, oldChecked, checked, R.drawable.ic_checkbox_checked)
}
