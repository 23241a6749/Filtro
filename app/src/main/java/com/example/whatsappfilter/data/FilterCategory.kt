package com.example.whatsappfilter.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.*

@Entity(tableName = "filter_categories")
data class FilterCategory(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val isEnabled: Boolean = true,
    @ColumnInfo(name = "keywords")
    val keywords: List<String>,
    @ColumnInfo(name = "color")
    val color: Int,
    @ColumnInfo(name = "icon")
    val icon: String
) 