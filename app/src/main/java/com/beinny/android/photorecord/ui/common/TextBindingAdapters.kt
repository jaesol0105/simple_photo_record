package com.beinny.android.photorecord.ui.common

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.beinny.android.photorecord.PhotoRecordApplication
import com.beinny.android.photorecord.R
import com.beinny.android.photorecord.common.DATE_FORMAT
import com.beinny.android.photorecord.common.DATE_FORMAT_OF_THUMBNAIL
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
        val df : DateFormat = SimpleDateFormat(DATE_FORMAT_OF_THUMBNAIL, Locale.UK) // 날짜를 문자열로 변환
        view.text = df.format(date)
    }
}

@BindingAdapter("memoTextCount")
fun applyMemoCountFormat(view: TextView, memo:String?){
    if(memo != null){
        if (memo.isEmpty()) {
            view.text = PhotoRecordApplication.applicationContext().getString(R.string.recorddetail_non_text)
        } else {
            view.text = memo.length.toString() + PhotoRecordApplication.applicationContext().getString(R.string.recorddetail_memo_length)
        }
    }
}

@BindingAdapter("deleteButtonName")
fun applyDeleteButtonNameFormat(view: TextView, isNew:Boolean?){
    if(isNew != null)
        if (isNew)
            view.text = PhotoRecordApplication.applicationContext().getString(R.string.all_cancel)
}