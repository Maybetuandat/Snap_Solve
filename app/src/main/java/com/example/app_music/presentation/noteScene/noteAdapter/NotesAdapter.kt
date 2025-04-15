package com.example.app_music.presentation.noteScene.noteAdapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app_music.R
import com.example.app_music.presentation.noteScene.model.NoteItem

class NotesAdapter(
    private val context: Context,
    private val notesList: List<NoteItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_NOTE = 0
        private const val TYPE_FOLDER = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (notesList[position].isFolder) TYPE_FOLDER else TYPE_NOTE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_FOLDER) {
            val view = LayoutInflater.from(context).inflate(R.layout.item_folder_recycleview, parent, false)
            FolderViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.item_note_recycleview, parent, false)
            NoteViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = notesList[position]

        when (holder) {
            is FolderViewHolder -> holder.bind(item)
            is NoteViewHolder -> holder.bind(item)
        }
    }

    override fun getItemCount(): Int = notesList.size

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imagePreview: ImageView = itemView.findViewById(R.id.imagePreview)
        private val textTitle: TextView = itemView.findViewById(R.id.textTitleNote)
        private val textDate: TextView = itemView.findViewById(R.id.textDateNote)

        fun bind(note: NoteItem) {
            textTitle.text = note.title
            textDate.text = note.date

            if (note.hasImage()) {
                imagePreview.visibility = View.VISIBLE
                imagePreview.setImageBitmap(note.imagePreview)
            } else {
                imagePreview.visibility = View.GONE
            }
        }
    }

    inner class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val folderIcon: ImageButton = itemView.findViewById(R.id.folderButton)
        private val textTitle: TextView = itemView.findViewById(R.id.textTitle)
        private val textDate: TextView = itemView.findViewById(R.id.textDate)
        private val iconExpand: Button = itemView.findViewById(R.id.button_expand_folder)
//        private val nestedRecyclerView: RecyclerView = itemView.findViewById(R.id.nestedRecyclerView)
//        private val containerLayout: LinearLayout = itemView.findViewById(R.id.containerLayout)

        fun bind(folder: NoteItem) {
            textTitle.text = folder.title
            textDate.text = folder.date

            // Set up nested RecyclerView
//            nestedRecyclerView.layoutManager = LinearLayoutManager(context)
//            val nestedAdapter = NotesAdapter(context, folder.getChildNotes())
//            nestedRecyclerView.adapter = nestedAdapter
//
//            // Show/hide based on expanded state
//            nestedRecyclerView.visibility = if (folder.isExpanded) View.VISIBLE else View.GONE
//
//            // Rotate icon based on expanded state
//            iconExpand.rotation = if (folder.isExpanded) 180f else 0f
//
//            // Click listener for expanding/collapsing
//            containerLayout.setOnClickListener {
//                folder.isExpanded = !folder.isExpanded
//                nestedRecyclerView.visibility = if (folder.isExpanded) View.VISIBLE else View.GONE
//                iconExpand.rotation = if (folder.isExpanded) 180f else 0f
//            }
        }
    }
}