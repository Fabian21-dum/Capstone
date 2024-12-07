package com.example.capstone.data

import com.example.capstone.data.model.Alphabet
import com.example.capstone.data.model.Word

object AppRepository {
    fun getWordsList(): List<Word> = listOf(
        Word(
            id = 1,
            title = "Introduction 1",
            summary = "All you need to learn to start learning Sign Language",
            description = "Lorem ipsum dolor sit amet consectetur. Eleifend egestas ut lacus elit vitae varius dolor diam feugiat. Nascetur placerat etiam proin phasellus at elit tortor sem. Interdum at amet nulla enim ut arcu. Lacus pretium odio varius sed nunc tincidunt neque. Molestie tristique sociis nunc nibh id.\n" + "At sit tellus mauris cras. Fusce at cras nisl scelerisque a eget cursus at.",
            videoUrl = "https://download.samplelib.com/mp4/sample-30s.mp4"
        ),
        Word(
            id = 2,
            title = "Introduction 2",
            summary = "All you need to learn to start learning Sign Language",
            description = "Lorem ipsum dolor sit amet consectetur. Eleifend egestas ut lacus elit vitae varius dolor diam feugiat. Nascetur placerat etiam proin phasellus at elit tortor sem. Interdum at amet nulla enim ut arcu. Lacus pretium odio varius sed nunc tincidunt neque. Molestie tristique sociis nunc nibh id.\n" + "At sit tellus mauris cras. Fusce at cras nisl scelerisque a eget cursus at.",
            videoUrl = "https://download.samplelib.com/mp4/sample-30s.mp4"
        ),
        Word(
            id = 3,
            title = "Introduction 3",
            summary = "All you need to learn to start learning Sign Language",
            description = "Lorem ipsum dolor sit amet consectetur. Eleifend egestas ut lacus elit vitae varius dolor diam feugiat. Nascetur placerat etiam proin phasellus at elit tortor sem. Interdum at amet nulla enim ut arcu. Lacus pretium odio varius sed nunc tincidunt neque. Molestie tristique sociis nunc nibh id.\n" + "At sit tellus mauris cras. Fusce at cras nisl scelerisque a eget cursus at.",
            videoUrl = "https://download.samplelib.com/mp4/sample-30s.mp4"
        ),
        Word(
            id = 4,
            title = "Introduction 4",
            summary = "All you need to learn to start learning Sign Language",
            description = "Lorem ipsum dolor sit amet consectetur. Eleifend egestas ut lacus elit vitae varius dolor diam feugiat. Nascetur placerat etiam proin phasellus at elit tortor sem. Interdum at amet nulla enim ut arcu. Lacus pretium odio varius sed nunc tincidunt neque. Molestie tristique sociis nunc nibh id.\n" + "At sit tellus mauris cras. Fusce at cras nisl scelerisque a eget cursus at.",
            videoUrl = "https://download.samplelib.com/mp4/sample-30s.mp4"
        ),
        Word(
            id = 5,
            title = "Introduction 5",
            summary = "All you need to learn to start learning Sign Language",
            description = "Lorem ipsum dolor sit amet consectetur. Eleifend egestas ut lacus elit vitae varius dolor diam feugiat. Nascetur placerat etiam proin phasellus at elit tortor sem. Interdum at amet nulla enim ut arcu. Lacus pretium odio varius sed nunc tincidunt neque. Molestie tristique sociis nunc nibh id.\n" + "At sit tellus mauris cras. Fusce at cras nisl scelerisque a eget cursus at.",
            videoUrl = "https://download.samplelib.com/mp4/sample-30s.mp4"
        )
    )

    fun getAlphabet(): List<Alphabet> {
        val list = mutableListOf<Alphabet>()

        ('A'..'Z').forEachIndexed { index, char ->
            list.add(
                Alphabet(
                    id = index,
                    alphabet = char.toString(),
                    description = "Lorem ipsum dolor sit amet consectetur. Eleifend egestas ut lacus elit vitae varius dolor diam feugiat. Nascetur placerat etiam proin phasellus at elit tortor sem. Interdum at amet nulla enim ut arcu. Lacus pretium odio varius sed nunc tincidunt neque. Molestie tristique sociis nunc nibh id.\n" + "At sit tellus mauris cras. Fusce at cras nisl scelerisque a eget cursus at."
                )
            )
        }

        return list
    }
}