// Repository：好友列表管理
package com.wakey.app.data.repository

import com.wakey.app.data.local.dao.FriendDao
import com.wakey.app.data.local.entity.FriendEntity
import com.wakey.app.domain.model.Friend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRepository @Inject constructor(
    private val dao: FriendDao
) {
    val friends: Flow<List<Friend>> = dao.observeAll().map { it.map(FriendEntity::toDomain) }

    suspend fun getById(id: Long): Friend? = dao.getById(id)?.toDomain()

    suspend fun getByUserId(userId: String): Friend? = dao.getByUserId(userId)?.toDomain()

    suspend fun save(friend: Friend): Long = dao.upsert(FriendEntity.fromDomain(friend))

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun deleteByUserId(userId: String) = dao.deleteByUserId(userId)

    suspend fun clearLocal() = dao.deleteAll()

    suspend fun updateFcmToken(id: Long, token: String) = dao.updateFcmToken(id, token)
}
