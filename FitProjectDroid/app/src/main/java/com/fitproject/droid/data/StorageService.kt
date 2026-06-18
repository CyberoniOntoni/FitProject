package com.fitproject.droid.data

import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await

class StorageService private constructor() {
    private val storage = Firebase.storage

    suspend fun uploadProgressPhoto(
        userId: String,
        sessionId: String,
        poseType: String,
        data: ByteArray,
        contentType: String,
        fileExtension: String
    ): String {
        val objectPath = "progressPictures/$userId/session_${sessionId}_$poseType.$fileExtension"
        val ref = storage.reference.child(objectPath)
        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType(contentType)
            .build()
        ref.putBytes(data, metadata).await()
        return ref.downloadUrl.await().toString()
    }

    companion object {
        val shared: StorageService by lazy { StorageService() }
    }
}