package com.beinny.android.dailylook

import androidx.lifecycle.ViewModel
import java.io.File

class DailyListViewModel: ViewModel() {
    private val dailyRepository = DailyRepository.get()
    val dailyListLiveData = dailyRepository.getDailys()

    // 새로운 데일리룩 추가하기
    fun addDaily (dailylook:Daily) {
        dailyRepository.addDailyLook(dailylook)
    }

    // 사진 파일이 가르킬 위치(File객체)를 CrimeFragment에 제공.
    fun getPhotoFile(dailylook:Daily): File {
        return dailyRepository.getPhotoFile(dailylook)
    }

    fun getThumbFile(dailylook:Daily): File {
        return dailyRepository.getThumbFile(dailylook)
    }
}