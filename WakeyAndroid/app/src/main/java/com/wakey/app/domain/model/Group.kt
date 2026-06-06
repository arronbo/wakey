// 領域模型：群組
package com.wakey.app.domain.model

data class Group(
    val id: Long = 0,
    val cloudId: String = "",        // 雲端文件 ID（跨裝置穩定）
    val ownerUid: String = "",       // 建立者 uid；只有 owner 才能編輯/刪除
    val name: String,
    val message: String = "",
    val color: String? = null,
    val photoUri: String? = null,
    val memberIds: List<Long> = emptyList(),     // 本機已映射的 Friend.id（可能延遲）
    val memberUids: List<String> = emptyList(),  // 雲端權威：已加入的成員 uid（不含 owner）
    val pendingUids: List<String> = emptyList()  // 待邀請的 uid（被邀請者同意後會搬到 memberUids）
)
