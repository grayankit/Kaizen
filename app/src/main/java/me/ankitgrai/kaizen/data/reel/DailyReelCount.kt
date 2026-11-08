package me.ankitgrai.kaizen.data.reel

import java.time.LocalDate

data class DailyReelCount(
    val date: LocalDate,
    val count: Int,
)
