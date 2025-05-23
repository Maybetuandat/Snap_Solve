package com.example.app_music.presentation.feature.noteScene.noteAdapter

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.example.app_music.R
import com.example.app_music.data.repository.FirebaseNoteRepository
import com.example.app_music.presentation.feature.noteScene.NoteDetailActivity
import com.example.app_music.presentation.feature.noteScene.model.NoteItem
import com.example.app_music.utils.StorageManager
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.tasks.await
import java.io.File

class NotesAdapter(
    private val context: Context,
    private val notesList: List<NoteItem>,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val onNewItemClick: (View?) -> Unit,
    private val onItemOptionsClick: (View, NoteItem) -> Unit,
    private val onFolderClick: (NoteItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_NEW = 0
        private const val TYPE_NOTE = 1
        private const val TYPE_FOLDER = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position == 0 -> TYPE_NEW // First item is always the NEW item
            notesList[position - 1].isFolder -> TYPE_FOLDER
            else -> TYPE_NOTE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_NEW -> {
                val view = LayoutInflater.from(context).inflate(R.layout.item_new_recycleview, parent, false)
                NewItemViewHolder(view)
            }
            TYPE_FOLDER -> {
                val view = LayoutInflater.from(context).inflate(R.layout.item_folder_recycleview, parent, false)
                FolderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(context).inflate(R.layout.item_note_recycleview, parent, false)
                NoteViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is NewItemViewHolder -> holder.bind()
            is FolderViewHolder, is NoteViewHolder -> {
                val adjustedPosition = position - 1
                if (adjustedPosition >= 0 && adjustedPosition < notesList.size) {
                    when (holder) {
                        is FolderViewHolder -> holder.bind(notesList[adjustedPosition])
                        is NoteViewHolder -> holder.bind(notesList[adjustedPosition])
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = notesList.size + 1

    inner class NewItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val newFolderButton: LinearLayout = itemView.findViewById(R.id.newFolderButton)
        private val textNewTitle: TextView = itemView.findViewById(R.id.textNewTitle)

        fun bind() {
            newFolderButton.setOnClickListener {
                onNewItemClick(it)
            }

            textNewTitle.setOnClickListener {
                onNewItemClick(it)
            }
        }
    }

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imagePreview: ImageView = itemView.findViewById(R.id.imagePreview)
        private val textTitle: TextView = itemView.findViewById(R.id.textTitleNote)
        private val textDate: TextView = itemView.findViewById(R.id.textDateNote)
        private val expandButton: Button = itemView.findViewById(R.id.note_expand_button)

        fun bind(note: NoteItem) {
            textTitle.text = note.title
            textDate.text = note.date

            // Đặt ảnh mặc định
            imagePreview.setImageResource(R.drawable.ic_note)

            // Hủy job tải ảnh cũ
            val tag = imagePreview.tag
            if (tag is Job) {
                tag.cancel()
            }

            // Sử dụng ảnh đã có trong bộ nhớ
            if (note.hasImage() && note.imagePreview != null) {
                imagePreview.setImageBitmap(note.imagePreview)
            } else {
                // Tạo một job mới để tải ảnh
                val job = lifecycleScope.launch {
                    try {
                        // Lấy thông tin note từ repository
                        val repo = FirebaseNoteRepository()
                        val noteResult = repo.getNote(note.id)

                        if (noteResult.isSuccess) {
                            val fullNote = noteResult.getOrNull()!!

                            // Nếu note có imagePath, sử dụng nó làm thumbnail
                            if (fullNote.imagePath != null) {

                                val bitmap = withContext(Dispatchers.IO) {
                                    val storage = FirebaseStorage.getInstance()
                                    val imageRef = storage.reference.child(fullNote.imagePath!!)

                                    try {
                                        // Tạo file tạm để lưu ảnh
                                        val localFile = File.createTempFile("thumbnail", "jpg")
                                        imageRef.getFile(localFile).await()

                                        // Giải mã file thành bitmap
                                        val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)

                                        // Xóa file tạm
                                        localFile.delete()

                                        bitmap
                                    } catch (e: Exception) {
                                        Log.e("NotesAdapter", "Error loading image: ${e.message}")
                                        null
                                    }
                                }

                                // Kiểm tra xem item có còn hiển thị không
                                if (isActive && bitmap != null) {
                                    // Lưu bitmap vào note để tái sử dụng
                                    note.imagePreview = bitmap
                                    imagePreview.setImageBitmap(bitmap)
                                    Log.d("NotesAdapter", "Thumbnail loaded successfully")
                                }
                            } else {
                                Log.d("NotesAdapter", "Note has no imagePath for thumbnail")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("NotesAdapter", "Error loading thumbnail: ${e.message}")
                    }
                }
                //lưu lại job để có thể hủy sau này
                imagePreview.tag = job
            }
            expandButton.setOnClickListener {
                onItemOptionsClick(it, note)
            }

            // Xử lý click vào note
            val clickListener = View.OnClickListener {
                val intent = Intent(context, NoteDetailActivity::class.java).apply {
                    putExtra("note_id", note.id)
                    putExtra("note_title", note.title)
                }
                context.startActivity(intent)
            }

            imagePreview.setOnClickListener(clickListener)
            textTitle.setOnClickListener(clickListener)
        }
    }

    inner class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val folderIcon: ImageButton = itemView.findViewById(R.id.folderButton)
        private val textTitle: TextView = itemView.findViewById(R.id.textTitle)
        private val textDate: TextView = itemView.findViewById(R.id.textDate)
        private val iconExpand: Button = itemView.findViewById(R.id.button_expand_folder)

        fun bind(folder: NoteItem) {
            textTitle.text = folder.title
            textDate.text = folder.date

            folderIcon.setOnClickListener {
                onFolderClick(folder)
            }

            textTitle.setOnClickListener {
                onFolderClick(folder)
            }

            iconExpand.setOnClickListener {
                onItemOptionsClick(it, folder)
            }
        }
    }
}