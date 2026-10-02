package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "boards")
data class BoardEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val backgroundColor: Long,
    val textureName: String,
    val pageCount: Int,
    val contentJson: String,
    val thumbnailBase64: String? = null
)
