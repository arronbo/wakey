// 雲端聊天：以 Firestore 儲存對話與訊息
// 結構：
//   conversations/{conversationId}
//       type: "direct" | "group"
//       memberUids: List<String>
//       groupCloudId: String?            // 群組聊天才有
//       lastText / lastSenderUid / lastAt
//       readAt: { uid: Timestamp }       // 各成員上次已讀時間
//   conversations/{conversationId}/messages/{messageId}
//       senderUid / senderName / text / createdAt
package com.wakey.app.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.wakey.app.domain.model.ChatMessage
import com.wakey.app.domain.model.ChatType
import com.wakey.app.domain.model.Conversation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRemote @Inject constructor() {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private fun conversations() = db.collection("conversations")
    private fun convDoc(id: String) = conversations().document(id)
    private fun messages(id: String) = convDoc(id).collection("messages")

    // 確保對話文件存在（建立或更新成員清單），回傳 conversationId
    suspend fun ensureConversation(
        conversationId: String,
        type: ChatType,
        memberUids: List<String>,
        groupCloudId: String? = null
    ) {
        if (conversationId.isBlank() || memberUids.isEmpty()) return
        val data = mapOf(
            "type" to if (type == ChatType.GROUP) "group" else "direct",
            "memberUids" to memberUids,
            "groupCloudId" to groupCloudId
        )
        convDoc(conversationId).set(data, SetOptions.merge()).await()
    }

    // 送出一則訊息（同時更新對話摘要 lastText/lastAt/lastSenderUid）
    suspend fun sendMessage(
        conversationId: String,
        senderUid: String,
        senderName: String,
        text: String
    ) {
        if (conversationId.isBlank() || text.isBlank()) return
        val now = FieldValue.serverTimestamp()
        val batch = db.batch()
        val msgRef = messages(conversationId).document()
        batch.set(
            msgRef,
            mapOf(
                "senderUid" to senderUid,
                "senderName" to senderName,
                "text" to text,
                "createdAt" to now
            )
        )
        batch.set(
            convDoc(conversationId),
            mapOf(
                "lastText" to text,
                "lastSenderUid" to senderUid,
                "lastAt" to now,
                "readAt" to mapOf(senderUid to now)  // 自己送出視為已讀
            ),
            SetOptions.merge()
        )
        batch.commit().await()
    }

    // 標記「我」已讀到現在
    suspend fun markRead(conversationId: String, myUid: String) {
        if (conversationId.isBlank() || myUid.isBlank()) return
        runCatching {
            convDoc(conversationId).set(
                mapOf("readAt" to mapOf(myUid to FieldValue.serverTimestamp())),
                SetOptions.merge()
            ).await()
        }
    }

    // 訂閱某對話的訊息（依時間排序）
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> = callbackFlow {
        if (conversationId.isBlank()) { trySend(emptyList()); close(); return@callbackFlow }
        val reg: ListenerRegistration = messages(conversationId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val list = snap?.documents?.map { d ->
                    ChatMessage(
                        id = d.id,
                        senderUid = d.getString("senderUid") ?: "",
                        senderName = d.getString("senderName") ?: "",
                        text = d.getString("text") ?: "",
                        createdAtMillis = d.getTimestamp("createdAt")?.toDate()?.time
                            ?: System.currentTimeMillis()
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    // 訂閱「我參與的所有對話」（依最後訊息時間排序）
    fun observeConversations(myUid: String): Flow<List<Conversation>> = callbackFlow {
        if (myUid.isBlank()) { trySend(emptyList()); close(); return@callbackFlow }
        val reg: ListenerRegistration = conversations()
            .whereArrayContains("memberUids", myUid)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val list = snap?.documents?.mapNotNull { d ->
                    @Suppress("UNCHECKED_CAST")
                    val members = (d.get("memberUids") as? List<String>) ?: return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    val readAt = (d.get("readAt") as? Map<String, Timestamp>)
                    Conversation(
                        id = d.id,
                        type = if (d.getString("type") == "group") ChatType.GROUP else ChatType.DIRECT,
                        memberUids = members,
                        groupCloudId = d.getString("groupCloudId"),
                        lastText = d.getString("lastText") ?: "",
                        lastSenderUid = d.getString("lastSenderUid") ?: "",
                        lastAtMillis = d.getTimestamp("lastAt")?.toDate()?.time ?: 0L,
                        readAtMillis = readAt?.get(myUid)?.toDate()?.time ?: 0L
                    )
                }?.sortedByDescending { it.lastAtMillis } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }
}
