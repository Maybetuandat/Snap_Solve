package com.example.app_music.data.model

import android.graphics.Color
import android.view.MotionEvent
import java.util.UUID

data class StrokePoint(
    val x: Float = 0f,
    val y: Float = 0f,
    val type: Int = MotionEvent.ACTION_MOVE // ACTION_DOWN, ACTION_MOVE, ACTION_UP
)

data class StrokeData(
    val id: String = UUID.randomUUID().toString(),
    val color: Int = Color.BLACK,
    val strokeWidth: Float = 5f,
    val isEraser: Boolean = false,
    val points: List<StrokePoint> = emptyList()
)

data class DrawingData(
    val strokes: List<StrokeData> = emptyList(),
    val width: Int = 0,
    val height: Int = 0
)