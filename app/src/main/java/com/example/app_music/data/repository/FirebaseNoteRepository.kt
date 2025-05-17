package com.example.app_music.data.repository

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.app_music.data.model.FolderFirebaseModel
import com.example.app_music.data.model.NoteFirebaseModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.Date
import java.util.UUID

class FirebaseNoteRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val foldersCollection = db.collection("folders")
    private val notesCollection = db.collection("notes")
    private val storageRef = storage.reference.child("notes")
    
    // Current user ID
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ""
    
    // Folders operations
    init {
    }

    suspend fun createFolder(title: String): Result<FolderFirebaseModel> {
        return try {
            val folderId = UUID.randomUUID().toString()
            val folder = FolderFirebaseModel(
                id = folderId,
                title = title,
                createdAt = Date().time,
                updatedAt = Date().time,
                ownerId = currentUserId
            )
            
            foldersCollection.document(folderId).set(folder).await()
            Result.success(folder)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating folder", e)
            Result.failure(e)
        }
    }
    
    suspend fun getFolders(): Result<List<FolderFirebaseModel>> {
        return try {
            val snapshot = foldersCollection
                .whereEqualTo("ownerId", currentUserId)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()
                
            val folders = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FolderFirebaseModel::class.java)?.apply {
                    id = doc.id
                }
            }
            
            Result.success(folders)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting folders", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateFolder(folder: FolderFirebaseModel): Result<FolderFirebaseModel> {
        return try {
            folder.updatedAt = Date().time
            foldersCollection.document(folder.id).set(folder).await()
            Result.success(folder)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating folder", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteFolder(folderId: String): Result<Boolean> {
        return try {
            // First delete all notes in this folder
            val notesSnapshot = notesCollection
                .whereEqualTo("folderId", folderId)
                .get()
                .await()
                
            val batch = db.batch()
            notesSnapshot.documents.forEach { doc ->
                batch.delete(notesCollection.document(doc.id))
            }
            
            // Then delete the folder
            batch.delete(foldersCollection.document(folderId))
            batch.commit().await()
            
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting folder", e)
            Result.failure(e)
        }
    }
    
    // Notes operations
    
    suspend fun createNote(title: String, folderId: String, imageBitmap: Bitmap? = null): Result<NoteFirebaseModel> {
        return try {
            val noteId = UUID.randomUUID().toString()
            var imagePath: String? = null
            
            // Upload image if provided
            if (imageBitmap != null) {
                val baos = ByteArrayOutputStream()
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val imageRef = storageRef.child("$noteId.jpg")
                imageRef.putBytes(baos.toByteArray()).await()
                imagePath = imageRef.path
            }
            
            val note = NoteFirebaseModel(
                id = noteId,
                title = title,
                createdAt = Date().time,
                updatedAt = Date().time,
                ownerId = currentUserId,
                folderId = folderId,
                imagePath = imagePath
            )
            
            notesCollection.document(noteId).set(note).await()
            Result.success(note)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating note", e)
            Result.failure(e)
        }
    }

    suspend fun getNotes(folderId: String): Result<List<NoteFirebaseModel>> {
        return try {
            val snapshot = notesCollection
                .whereEqualTo("folderId", folderId)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val notes = snapshot.documents.mapNotNull { doc ->
                doc.toObject(NoteFirebaseModel::class.java)?.apply {
                    id = doc.id
                }
            }

            Result.success(notes)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting notes", e)
            Result.failure(e)
        }
    }

    suspend fun getNote(noteId: String): Result<NoteFirebaseModel> {
        return try {
            val doc = notesCollection.document(noteId).get().await()
            val note = doc.toObject(NoteFirebaseModel::class.java)?.apply {
                id = doc.id
            }

            if (note != null) {
                Result.success(note)
            } else {
                Result.failure(Exception("Note not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting note", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateNote(note: NoteFirebaseModel, newImageBitmap: Bitmap? = null): Result<NoteFirebaseModel> {
        return try {
            // Update image if provided
            if (newImageBitmap != null) {
                val baos = ByteArrayOutputStream()
                newImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val imageRef = storageRef.child("${note.id}.jpg")
                imageRef.putBytes(baos.toByteArray()).await()
                note.imagePath = imageRef.path
            }
            
            note.updatedAt = Date().time
            notesCollection.document(note.id).set(note).await()
            Result.success(note)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating note", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteNote(noteId: String): Result<Boolean> {
        return try {
            // First get the note to check if it has an image
            val noteDoc = notesCollection.document(noteId).get().await()
            val note = noteDoc.toObject(NoteFirebaseModel::class.java)
            
            // Delete image if exists
            note?.imagePath?.let { path ->
                storage.reference.child(path).delete().await()
            }
            
            // Delete note document
            notesCollection.document(noteId).delete().await()
            
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting note", e)
            Result.failure(e)
        }
    }
    
    // Collaboration operations
    
    suspend fun shareNoteWithUser(noteId: String, userEmail: String): Result<Boolean> {
        return try {
            // First find the user by email
            val userQuerySnapshot = db.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .await()
                
            if (userQuerySnapshot.documents.isEmpty()) {
                return Result.failure(Exception("User not found"))
            }
            
            val userId = userQuerySnapshot.documents[0].id
            
            // Add user to collaborators
            val noteRef = notesCollection.document(noteId)
            val note = noteRef.get().await().toObject(NoteFirebaseModel::class.java)
            
            if (note != null) {
                val collaborators = note.collaborators.toMutableList()
                if (!collaborators.contains(userId)) {
                    collaborators.add(userId)
                    noteRef.update("collaborators", collaborators).await()
                }
                Result.success(true)
            } else {
                Result.failure(Exception("Note not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing note", e)
            Result.failure(e)
        }
    }
    
    suspend fun getNoteUrl(noteId: String): String {
        // Generate a shareable URL with the noteId
        return "snapsolve://notes/$noteId"
    }
    
    suspend fun getImageBitmap(imagePath: String): Result<Uri> {
        return try {
            val imageRef = storage.reference.child(imagePath)
            val uri = imageRef.downloadUrl.await()
            Result.success(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting image", e)
            Result.failure(e)
        }
    }
    
    companion object {
        private const val TAG = "FirebaseNoteRepository"
    }
}