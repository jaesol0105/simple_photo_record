package com.beinny.android.photorecord.ui.backup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BackUpViewModel : ViewModel() {
    private val _text = MutableLiveData<String>().apply {
        value = "This is backUp Fragment"
    }
    val text: LiveData<String> = _text
}