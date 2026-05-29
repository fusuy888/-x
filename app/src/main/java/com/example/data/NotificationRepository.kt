package com.example.data

import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val dao: NotificationDao) {
    val allHistory: Flow<List<NotificationHistory>> = dao.getAllHistory()

    suspend fun insert(history: NotificationHistory) = dao.insertHistory(history)

    suspend fun deleteById(id: Int) = dao.deleteHistoryById(id)

    suspend fun clearAll() = dao.clearAllHistory()
}
