package com.jerrynifemi.brainblitz

/** A single multiple-choice question. */
data class Question(
    val text: String,
    val options: List<String>,
    val answerIndex: Int,
    val categoryId: String,
    val categoryName: String
)
