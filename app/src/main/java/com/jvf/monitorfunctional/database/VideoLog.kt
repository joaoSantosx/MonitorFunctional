package com.jvf.monitorfunctional.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "historico_videos")
data class VideoLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titulo: String,
    val timestamp: Long,
    val dataFormatada: String // Vamos salvar a data bonitinha já
)