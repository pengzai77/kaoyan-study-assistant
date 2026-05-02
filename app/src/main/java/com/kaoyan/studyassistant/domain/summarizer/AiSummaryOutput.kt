package com.kaoyan.studyassistant.domain.summarizer

data class AiSummaryOutput(
    val summary: String = "",
    val strengths: List<String> = emptyList(),
    val problems: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val tomorrowFocus: List<String> = emptyList(),
    val confidence: String = "",
    val tone: String = ""
)
