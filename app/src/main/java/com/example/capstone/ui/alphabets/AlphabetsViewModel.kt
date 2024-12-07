package com.example.capstone.ui.alphabets

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.capstone.data.AppRepository
import com.example.capstone.data.model.Alphabet
import com.example.capstone.data.model.Word
import kotlinx.coroutines.launch

class AlphabetsViewModel : ViewModel() {

    private val _alphabets = MutableLiveData<List<Alphabet>>()
    val alphabets: LiveData<List<Alphabet>> = _alphabets

    init {
        getAlphabetsList()
    }

    private fun getAlphabetsList() {
        viewModelScope.launch {
            _alphabets.value = AppRepository.getAlphabet()
        }
    }
}