// Create a new file: app/src/main/java/com/example/app_music/presentation/feature/noteScene/views/RealtimeDrawingManager.kt
package com.example.app_music.presentation.feature.noteScene.views

import android.graphics.Color
import android.util.Log
import com.example.app_music.data.collaboration.CollaborationManager
import com.example.app_music.presentation.feature.noteScene.views.DrawingView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

class RealtimeDrawingManager(
    private val drawingView: DrawingView,
    private val collaborationManager: CollaborationManager,
    private val scope: CoroutineScope,
    private val currentPageId: String
) {
    private val TAG = "RealtimeDrawingManager"
    private var isLocalStroke = false
    private var activeStrokeIds = HashSet<String>() // Track IDs for current page

    init {
        // Listen for remote strokes
        listenForRemoteDrawingActions()

        // Set listener for local strokes
        drawingView.setOnDrawCompletedListener(object : DrawingView.OnDrawCompletedListener {
            override fun onDrawCompleted() {
                // Get the last stroke drawn and sync it
                val lastStroke = drawingView.getLastStroke()
                if (lastStroke != null) {
                    isLocalStroke = true

                    // Add stroke ID to the tracking set
                    activeStrokeIds.add(lastStroke.id)

                    // Convert to drawing action and send to server
                    val drawingAction = collaborationManager.strokeToDrawingAction(lastStroke)
                    // Add page ID to the action to distinguish between pages
                    val actionWithPageId = drawingAction.copy(
                        pageId = currentPageId,
                        timestamp = System.currentTimeMillis()
                    )
                    collaborationManager.saveDrawingAction(actionWithPageId)
                    isLocalStroke = false
                }
            }
        })
    }

    // Clear existing strokes from view when page changes
    // In RealtimeDrawingManager.kt
    fun clearForPageChange() {
        // Make sure we're not listening to events during the clear
        isLocalStroke = true

        // Clear the drawing view completely
        drawingView.clearDrawing(false)

        // Clear our tracking of stroke IDs
        activeStrokeIds.clear()

        // Reset local stroke flag
        isLocalStroke = false
    }

    private fun listenForRemoteDrawingActions() {
        scope.launch(Dispatchers.IO) {
            try {
                // Load existing strokes for this page first
                val existingActions = collaborationManager.getDrawingActionsForPage(currentPageId)
                if (existingActions.isNotEmpty()) {
                    scope.launch(Dispatchers.Main) {
                        // Clear view before applying loaded strokes
                        drawingView.clearDrawing(false)

                        // Apply existing strokes in order
                        for (action in existingActions) {
                            Log.d(TAG, "Loading existing stroke: ${action.actionId} for page $currentPageId")
                            val stroke = convertToStroke(action)

                            // Add to tracked IDs
                            activeStrokeIds.add(stroke.id)

                            // Add to drawing
                            drawingView.addRemoteStroke(stroke)
                        }
                    }
                }

                // Now listen for real-time updates
                collaborationManager.getDrawingActionsFlow().collectLatest { action ->
                    // Only process if this action is for our current page
                    if (action.pageId == currentPageId) {
                        Log.d(TAG, "Received real-time drawing action: ${action.actionId} for page $currentPageId")

                        // Skip if we've already seen this stroke ID
                        if (action.userId != collaborationManager.getCurrentUserId() &&
                            !activeStrokeIds.contains(action.actionId)) {

                            scope.launch(Dispatchers.Main) {
                                when (action.type) {
                                    CollaborationManager.ActionType.DRAW -> {
                                        // Add the stroke ID to our tracking
                                        activeStrokeIds.add(action.actionId)

                                        // Apply the stroke
                                        val remoteStroke = convertToStroke(action)
                                        drawingView.addRemoteStroke(remoteStroke)
                                    }
                                    CollaborationManager.ActionType.ERASE -> {
                                        val strokeToErase = drawingView.findStrokeById(action.targetStrokeId)
                                        if (strokeToErase != null) {
                                            // Remove from tracking
                                            activeStrokeIds.remove(action.targetStrokeId)

                                            // Remove from drawing
                                            drawingView.removeStroke(strokeToErase)
                                        }
                                    }
                                    CollaborationManager.ActionType.CLEAR -> {
                                        // Handle clear all for this page
                                        activeStrokeIds.clear()
                                        drawingView.clearDrawing(false)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in real-time drawing updates", e)
            }
        }
    }
    private fun convertToStroke(action: CollaborationManager.DrawingAction): DrawingView.Stroke {
        // Convert drawing action back to a stroke
        val points = action.path.map { point ->
            DrawingView.StrokePoint(
                x = point.x,
                y = point.y,
                type = when (point.operation) {
                    CollaborationManager.PathOperation.MOVE_TO -> android.view.MotionEvent.ACTION_DOWN
                    CollaborationManager.PathOperation.LINE_TO -> android.view.MotionEvent.ACTION_MOVE
                    CollaborationManager.PathOperation.QUAD_TO -> android.view.MotionEvent.ACTION_MOVE
                }
            )
        }.toMutableList()
        
        return DrawingView.Stroke(
            id = action.actionId,
            color = action.color,
            strokeWidth = action.strokeWidth,
            isEraser = action.type == CollaborationManager.ActionType.ERASE,
            points = points
        )
    }

}