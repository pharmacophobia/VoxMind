package com.voxmind.app.data.models

import java.util.UUID

data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class Checklist(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val items: List<TodoItem> = emptyList(),
    val colorHex: String = "#6366F1",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getCompletedCount(): Int = items.count { it.isDone }
    fun getTotalCount(): Int = items.size
    fun getProgress(): Float = if (items.isEmpty()) 0f else getCompletedCount().toFloat() / items.size.toFloat()
}
