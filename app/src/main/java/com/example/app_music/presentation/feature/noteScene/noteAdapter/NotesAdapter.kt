package com.example.app_music.presentation.feature.noteScene.noteAdapter

import android.content.Context
import android.content.Intent
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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.app_music.R
import com.example.app_music.presentation.feature.noteScene.NoteDetailActivity
import com.example.app_music.presentation.feature.noteScene.model.NoteItem
import com.example.app_music.utils.StorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.isActive

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
                // Safer approach with bounds checking
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

    override fun getItemCount(): Int = notesList.size + 1 // +1 for the NEW item

    inner class NewItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val newFolderButton: LinearLayout = itemView.findViewById(R.id.newFolderButton)
        private val textNewTitle: TextView = itemView.findViewById(R.id.textNewTitle)

        fun bind() {
            // When clicking on the plus box
            newFolderButton.setOnClickListener {
                onNewItemClick(it)
            }

            // When clicking on the dropdown arrow
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

        // Trong NotesAdapter.kt - ViewHolder
        fun bind(note: NoteItem) {
            textTitle.text = note.title
            textDate.text = note.date

            // Đặt ảnh mặc định ngay lập tức
            imagePreview.setImageResource(R.drawable.ic_note)

            // Hủy job tải ảnh cũ nếu có
            val tag = imagePreview.tag
            if (tag is Job) {
                tag.cancel()
            }

            // Sử dụng ảnh đã có trong bộ nhớ nếu tồn tại
            if (note.hasImage() && note.imagePreview != null) {
                imagePreview.setImageBitmap(note.imagePreview)
            } else {
                // Tạo một job mới để tải ảnh
                val job = lifecycleScope.launch {
                    try {
                        // Tạo StorageManager và tải ảnh
                        val storageManager = StorageManager(context)
                        // loadThumbnail đã có logic load từ local trước, sau đó Firebase nếu cần
                        val thumbnail = withContext(Dispatchers.IO) {
                            storageManager.loadThumbnail(note.id)
                        }

                        // Kiểm tra xem item có còn hiển thị không
                        if (isActive && thumbnail != null) {
                            // Lưu bitmap vào note để tái sử dụng
                            note.imagePreview = thumbnail
                            imagePreview.setImageBitmap(thumbnail)
                            Log.d("NotesAdapter", "Thumbnail loaded successfully")
                        }

                    } catch (e: Exception) {
                        Log.e("NotesAdapter", "Error loading thumbnail: ${e.message}")
                    }
                }

                // Lưu job vào tag để có thể hủy sau này
                imagePreview.tag = job
            }

            // Handle click for arrow button
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

            // Handle folder click
            folderIcon.setOnClickListener {
                onFolderClick(folder)
            }

            // Handle title click
            textTitle.setOnClickListener {
                onFolderClick(folder)
            }

            // Handle arrow button click
            iconExpand.setOnClickListener {
                onItemOptionsClick(it, folder)
            }
        }
    }
}