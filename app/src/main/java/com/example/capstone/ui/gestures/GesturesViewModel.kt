package com.example.capstone.ui.gestures

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.capstone.data.AppRepository
import com.example.capstone.data.model.Word
import kotlinx.coroutines.launch

class GesturesViewModel : ViewModel() {

    private val _words = MutableLiveData<List<Word>>()
    val words: LiveData<List<Word>> = _words

    init {
        getWordsList()
    }

    private fun getWordsList() {
        viewModelScope.launch {
            _words.value = AppRepository.getWordsList()
        }
    }
}