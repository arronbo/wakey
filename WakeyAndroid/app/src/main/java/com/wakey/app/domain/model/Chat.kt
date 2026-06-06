// 領域模型：聊天對話與訊息
package com.wakey.app.domain.model

// 一則訊息
data class ChatMessage(
    val id: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val text: String = "",
    val createdAtMillis: Long = 0L,   // 伺服器時間（毫秒）；尚未回填時為本機時間
    val pending: Boolean = false      // true = 本地樂觀顯示、尚未確認寫入雲端
) {
    fun isMine(myUid: String) = senderUid == myUid
}

// 對話（一對一或群組）
data class Conversation(
    val id: String = "",                 // 雲端 conversation 文件 id
    val type: ChatType = ChatType.DIRECT,
    val memberUids: List<String> = emptyList(),
    val groupCloudId: String? = null,    // 群組聊天才有，對應 groups/{cloudId}
    val lastText: String = "",
    val lastSenderUid: String = "",
    val lastAtMillis: Long = 0L,
    val readAtMillis: Long = 0L          // 「我」上次已讀時間（從 readAt map 取出）
) {
    // 對方 uid（僅一對一）：成員中扣掉自己
    fun otherUid(myUid: String): String? =
        if (type == ChatType.DIRECT) memberUids.firstOrNull { it != myUid } else null

    fun hasUnread(myUid: String): Boolean =
        lastAtMillis > readAtMillis && lastSenderUid != myUid && lastText.isNotBlank()
}

enum class ChatType { DIRECT, GROUP }

// conversation id 規則：
//   一對一：dm_{較小uid}_{較大uid}（雙方算出來一致）
//   群組：  grp_{groupCloudId}
object ConversationId {
    fun direct(a: String, b: String): String {
        val (x, y) = if (a <= b) a to b else b to a
        return "dm_${x}_$y"
    }

    fun group(groupCloudId: String): String = "grp_$groupCloudId"
}
