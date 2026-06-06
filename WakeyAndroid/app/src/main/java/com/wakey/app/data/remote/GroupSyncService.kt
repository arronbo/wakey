// 群組同步服務：訂閱全域 groups collection 中「我是成員」的所有群組，覆蓋本機 cache
package com.wakey.app.data.remote

import android.util.Log
import com.wakey.app.data.repository.GroupRepository
import com.wakey.app.data.repository.UserProfileRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupSyncService @Inject constructor(
    private val directory: FirestoreUserDirectory,
    private val groupRepository: GroupRepository,
    private val userProfileRepository: UserProfileRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            try {
                val myUid = userProfileRepository.getOrCreate().uuid
                if (myUid.isBlank()) return@launch
                directory.observeGroupsForMember(myUid).collectLatest { groups ->
                    Log.d(TAG, "remote groups=${groups.size}")
                    groupRepository.applyRemote(groups)
                }
            } catch (e: Exception) {
                Log.e(TAG, "GroupSyncService crashed", e)
            }
        }
    }

    fun stop() {
        job?.cancel(); job = null
    }

    companion object { private const val TAG = "GroupSyncService" }
}
