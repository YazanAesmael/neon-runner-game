package com.app.multiplatform.engine

import androidx.compose.ui.graphics.Color

// Constants for Physics configuration
object GameConfig {
    const val GRAVITY = 1.6f
    const val JUMP_FORCE = 32f
    const val GROUND_HEIGHT_RATIO = 0.75f
    const val START_SPEED = 15f
    const val OBSTACLE_GAP_MIN = 600f
    const val PLAYER_SIZE = 60f
    const val SPAWN_CHANCE = 0.02f
}

// Immutable State
data class GameState(
    val playerY: Float = 0f,    // Height from ground (0 is ground)
    val velocityY: Float = 0f,
    val obstacles: List<Obstacle> = emptyList(),
    val isGameOver: Boolean = false,
    val score: Long = 0,
    val currentSpeed: Float = GameConfig.START_SPEED,
    val worldTick: Long = 0L // Used for animation sync
)

data class Obstacle(
    val id: Long, // Unique ID to track identity
    val x: Float,
    val width: Float,
    val height: Float,
    val color: Color = Color(0xFF00FF00)
)