package com.beinny.android.photorecord.ui.common

import android.widget.Button
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.beinny.android.photorecord.common.DATE_FORMAT
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@BindingAdapter("dateInKorean")
fun applyDateFormat(view: Button, date: Date?){
    if(date != null){
        val df : DateFormat = SimpleDateFormat(DATE_FORMAT) // 날짜를 문자열로 변환
        view.text = df.format(date)
    }
}

@BindingAdapter("thumbnailDateInKorean")
fun applyThumbnailDateFormat(view: TextView, date: Date?){
    if(date != null){
        val df : DateFormat = SimpleDateFormat("yyyy/M/d hh:mm a", Locale.UK) // 날짜를 문자열로 변환
        view.text = df.format(date)
    }
}