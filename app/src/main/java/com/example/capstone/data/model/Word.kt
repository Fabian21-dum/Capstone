package com.example.capstone.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Word(
    val id: Int,
    val title: String,
    val summary: String,
    val description: String,
    val videoUrl: String
): Parcelable
