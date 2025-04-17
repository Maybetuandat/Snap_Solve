package com.example.app_music.presentation.communityPosting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import com.example.app_music.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TopicSelectionBottomSheet : BottomSheetDialogFragment() {

    override fun getTheme(): Int {
        return R.style.BottomSheetDialogTheme
    }

    private lateinit var radioGroupTopics: RadioGroup
    private lateinit var btnSelect: Button

    // Interface for callback to the parent fragment
    interface TopicSelectionListener {
        fun onTopicSelected(topic: String)
    }

    private var listener: TopicSelectionListener? = null

    fun setTopicSelectionListener(listener: TopicSelectionListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_topic_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        radioGroupTopics = view.findViewById(R.id.radioGroupTopics)
        btnSelect = view.findViewById(R.id.btnSelectTopic)

        // Set up the topics (these could come from resources or as arguments)
        setupTopics()

        // Handle select button click
        btnSelect.setOnClickListener {
            val selectedId = radioGroupTopics.checkedRadioButtonId
            if (selectedId != -1) {
                val radioButton = view.findViewById<RadioButton>(selectedId)
                listener?.onTopicSelected(radioButton.text.toString())
                dismiss()
            }
        }
    }

    private fun setupTopics() {
        // Load topics from string resources
        val topics = listOf(
            getString(R.string.topic_general),
            getString(R.string.topic_questions),
            getString(R.string.topic_news)
        )

        // Access individual radio buttons
        val rbTopic1 = view?.findViewById<RadioButton>(R.id.rbTopic1)
        val rbTopic2 = view?.findViewById<RadioButton>(R.id.rbTopic2)
        val rbTopic3 = view?.findViewById<RadioButton>(R.id.rbTopic3)

        // Set text for each radio button
        rbTopic1?.text = topics[0]
        rbTopic2?.text = topics[1]
        rbTopic3?.text = topics[2]

        // Preselect the first option
        rbTopic1?.isChecked = true
    }

    companion object {
        const val TAG = "TopicSelectionBottomSheet"

        fun newInstance(): TopicSelectionBottomSheet {
            return TopicSelectionBottomSheet()
        }
    }
}