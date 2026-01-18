package com.android.t_cloud

data class UpdateResponse(val ok: Boolean, val result: List<Update>)

data class Update(val update_id: Long, val message: Message?)

data class Message(
    val message_id: Long,
    val chat: Chat?,
    val document: Document?,
    val photo: List<PhotoSize>?,
    val video: VideoSize?
)

data class Chat(val id: Long)

// UPDATE BAGIAN INI SAJA
data class Document(
    val file_id: String,
    val file_name: String? = "",
    val isFolder: Boolean = false,
    val file_size: Long? = 0,
    val parentId: String = "root" // Tambahkan ini (default di luar folder)
)

data class PhotoSize(val file_id: String)
data class VideoSize(val file_id: String)
data class FileResponse(val ok: Boolean, val result: FilePath?)
data class FilePath(val file_path: String)