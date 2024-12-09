package com.example.capstone.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Alphabet(
    val id: Int,
    val alphabet: String,
    val description: String
): Parcelable
