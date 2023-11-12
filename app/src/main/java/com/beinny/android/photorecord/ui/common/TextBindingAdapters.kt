package com.beinny.android.photorecord.ui.common

import android.graphics.Paint
import android.text.Editable
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.beinny.android.photorecord.common.DATE_FORMAT
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@BindingAdapter("dateInKorean")
fun applyDateFormat(view: TextView, date: Date?){
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

@BindingAdapter("memoTextCount")
fun applyMemoCountFormat(view: TextView, memo:String?){
    if(memo != null){
        if (memo.isEmpty()) {
            view.text = ""
        } else {
            view.text = memo.length.toString() + " / 8000"
        }
    }
}
