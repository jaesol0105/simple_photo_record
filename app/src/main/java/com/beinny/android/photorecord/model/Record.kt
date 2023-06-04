package com.beinny.android.photorecord.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
class Record(
    @PrimaryKey val id: UUID= UUID.randomUUID(),
    var label: String = "",
    var date: Date = Date(),
    var memo: String = "",
    var isNew: Boolean =true,
    var isChecked: Boolean =false) {
    // 연산속성 : 다른 속성의 값을 이용하여 값을 산출한다, 값을 저장하는 필드를 갖지는 않는다.
    val photoFileName
        get() = "IMG_$id.jpg"
    val thumbFileName
        get() = "THUMB_$id.jpg"
    val tempFileName
        get() = "TEMP_$id.jpg"
}