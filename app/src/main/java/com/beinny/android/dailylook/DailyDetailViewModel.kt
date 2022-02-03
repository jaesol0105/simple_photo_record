package com.beinny.android.dailylook

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import java.io.File
import java.util.*

class DailyDetailViewModel : ViewModel() {
    private val dailyRepository = DailyRepository.get()
    private val dailyIdLiveData = MutableLiveData<UUID>()

    var dailyLiveData: LiveData<Daily?> =
        Transformations.switchMap(dailyIdLiveData) { dailyId -> dailyRepository.getDaily(dailyId) }

    fun loadDaily(dailyId:UUID) {
        dailyIdLiveData.value = dailyId
    }

    fun saveDaily(dailylook:Daily){
        dailyRepository.updateDaily(dailylook)
    }
    // 사진 파일이 가르킬 위치(File객체)를 CrimeFragment에 제공.
    fun getPhotoFile(dailylook:Daily): File {
        return dailyRepository.getPhotoFile(dailylook)
    }
    fun getThumbFile(dailylook:Daily): File {
        return dailyRepository.getThumbFile(dailylook)
    }
}