package com.app.multiplatform.engine

import androidx.compose.ui.graphics.Color

object GameConfig {
    // LOGICAL UNITS
    const val WORLD_HEIGHT = 1000f
    const val GROUND_Y = 750f

    // Physics
    const val GRAVITY = 2500f
    const val JUMP_VELOCITY = 1100f

    // Player
    const val PLAYER_SIZE = 60f
    // CHANGED: Reduced from 150f to 85f so it sits ~15-20% left on phones
    const val PLAYER_X = 85f

    // Gameplay
    const val START_SPEED = 600f
    const val MAX_SPEED = 1500f
    const val ACCELERATION = 10f
}

// Immutable State (The Truth)
data class GameState(
    val playerY: Float = GameConfig.GROUND_Y, // Position in World Units
    val velocityY: Float = 0f,
    val isGrounded: Boolean = true,

    // Entities
    val obstacles: List<Obstacle> = emptyList(),
    val particles: List<Particle> = emptyList(),
    val orbs: List<Orb> = emptyList(),

    // Meta
    val isGameOver: Boolean = false,
    val score: Long = 0,
    val currentSpeed: Float = GameConfig.START_SPEED,
    val worldTick: Long = 0L,
    val cameraShake: Float = 0f // >0 means shake screen
)

data class Obstacle(
    val id: Long,
    val x: Float,
    val width: Float,
    val height: Float,
    val type: ObstacleType
)

enum class ObstacleType { SPIKE, BLOCK, FLOATING }

data class Particle(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val life: Float, // 1.0 (fresh) to 0.0 (dead)
    val color: Color,
    val size: Float
)

data class Orb(
    val id: Long,
    val x: Float,
    val y: Float,
    val isCollected: Boolean = false
)