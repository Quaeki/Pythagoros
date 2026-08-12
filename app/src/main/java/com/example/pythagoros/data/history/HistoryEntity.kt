package com.example.pythagoros.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "solution_history")
data class HistoryEntity(
    @PrimaryKey val id: Long,
    val createdAtMillis: Long,
    val recognizedText: String,
    val imagePath: String?,
    val expression: String,
    val problemType: String,
    val answer: String,
    val steps: String,
    val graphTitle: String?,
    val graphVariable: String?,
    val graphCoefficients: String?,
    val graphRoots: String?,
    val graphVertexX: Double?,
    val graphVertexY: Double?,
    val graphVertexLabel: String?,
)
