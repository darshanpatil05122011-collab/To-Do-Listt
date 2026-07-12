package com.example.dailylist

data class Task(
    val id: Long,
    var text: String,
    var done: Boolean = false
)
