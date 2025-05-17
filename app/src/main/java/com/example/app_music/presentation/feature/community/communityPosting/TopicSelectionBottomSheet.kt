package com.example.app_music.presentation.feature.community.communityPosting

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import com.example.app_music.R
import com.example.app_music.domain.model.Topic
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TopicSelectionBottomSheet : BottomSheetDialogFragment() {

    override fun getTheme(): Int {
        return R.style.BottomSheetDialogTheme
    }

    private lateinit var topicsContainer: LinearLayout
    private lateinit var btnSelect: Button

    // Lưu trữ các topic được chọn
    private val selectedTopicIds = mutableSetOf<Long>()
    private var allTopics: List<Topic> = emptyList()

    // Interface cho callback đến parent fragment
    interface TopicSelectionListener {
        fun onTopicsSelected(selectedTopics: List<Topic>)
    }

    private var listener: TopicSelectionListener? = null

    fun setTopicSelectionListener(listener: TopicSelectionListener) {
        this.listener = listener
    }

    // Phương thức để thiết lập danh sách topic và những topic nào đã được chọn sẵn
    fun setTopics(topics: List<Topic>, preSelectedTopicIds: Set<Long> = emptySet()) {
        allTopics = topics
        selectedTopicIds.clear()
        selectedTopicIds.addAll(preSelectedTopicIds)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Tạo view từ layout
        val view = inflater.inflate(R.layout.layout_topic_selection, container, false)

        // Thêm background cho dialog
        view.setBackgroundResource(R.drawable.topic_dialog_background)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        topicsContainer = view.findViewById(R.id.topicsContainer)
        btnSelect = view.findViewById(R.id.btnSelectTopic)

        // Thiết lập các topic
        setupTopics()

        // Xử lý click vào nút chọn
        btnSelect.setOnClickListener {
            // Lấy danh sách các topic đã chọn
            val selectedTopics = allTopics.filter { it.id in selectedTopicIds }

            // Gọi callback
            listener?.onTopicsSelected(selectedTopics)
            dismiss()
        }
    }

    private fun setupTopics() {
        // Xóa tất cả checkbox hiện tại
        topicsContainer.removeAllViews()

        // Thêm checkbox cho mỗi topic
        allTopics.forEach { topic ->
            val checkBox = CheckBox(requireContext()).apply {
                text = topic.name
                isChecked = topic.id in selectedTopicIds
                id = View.generateViewId()

                // Style cho checkbox
                textSize = 16f
                setPadding(16, 16, 16, 16)

                // Xử lý khi checkbox được click
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedTopicIds.add(topic.id)
                    } else {
                        selectedTopicIds.remove(topic.id)
                    }
                }
            }

            // Thêm padding và margin
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 8, 8, 8)
            }

            // Thêm vào container
            topicsContainer.addView(checkBox, layoutParams)
        }

        // Nếu không có topic nào, hiển thị thông báo
        if (allTopics.isEmpty()) {
            val message = TextView(requireContext()).apply {
                text = "Không có chủ đề nào"
                gravity = Gravity.CENTER
                textSize = 16f
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            topicsContainer.addView(message)
        }
    }

    companion object {
        const val TAG = "TopicSelectionBottomSheet"

        fun newInstance(): TopicSelectionBottomSheet {
            return TopicSelectionBottomSheet()
        }
    }
}