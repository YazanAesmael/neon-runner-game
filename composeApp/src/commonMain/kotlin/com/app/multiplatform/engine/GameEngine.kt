package com.app.multiplatform.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

interface GameEngine {
    val gameState: StateFlow<GameState>
    fun jump()
    fun reset()
    fun tick(nanos: Long)
}

class NeonPhysicsEngine : GameEngine {
    private val _state = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState> = _state.asStateFlow()

    private var obstacleIdCounter = 0L

    override fun jump() {
        _state.update { current ->
            if (current.isGameOver) return
            // Only jump if effectively grounded
            if (current.playerY <= 0) {
                current.copy(velocityY = GameConfig.JUMP_FORCE)
            } else current
        }
    }

    override fun reset() {
        _state.value = GameState()
    }

    override fun tick(nanos: Long) {
        _state.update { current ->
            if (current.isGameOver) return@update current

            // --- 1. Physics Calculations ---
            var newY = current.playerY + current.velocityY
            var newVel = current.velocityY - GameConfig.GRAVITY

            // Ground Clamp
            if (newY < 0) {
                newY = 0f
                newVel = 0f
            }

            // --- 2. Obstacle Logic ---
            val nextObstacles = ArrayList<Obstacle>(current.obstacles.size + 1)
            var scoreIncrement = 0L
            var speedIncrement = 0f

            for (obs in current.obstacles) {
                val newX = obs.x - current.currentSpeed
                
                // If off screen (allow buffer), discard and score
                if (newX < -200f) {
                    scoreIncrement++
                    if ((current.score + scoreIncrement) % 5 == 0L) {
                        speedIncrement += 0.5f
                    }
                } else {
                    nextObstacles.add(obs.copy(x = newX))
                }
            }

            // --- 3. Collision Detection (AABB) ---
            // Player Box (Relative to 0,0 ground)
            val pL = 150f + 15f // +15 padding
            val pR = 150f + GameConfig.PLAYER_SIZE - 15f
            val pB = newY

            var isHit = false
            for (obs in nextObstacles) {
                val oL = obs.x
                val oR = obs.x + obs.width
                val oT = obs.height // Obstacle height from ground up

                // Horizontal Overlap && Vertical Overlap
                if (pR > oL && pL < oR && pB < oT - 10f) {
                    isHit = true
                    break
                }
            }

            // --- 4. Spawning System ---
            val lastObs = nextObstacles.lastOrNull()
            val shouldSpawn = lastObs == null || 
                (2500f - lastObs.x > GameConfig.OBSTACLE_GAP_MIN + (current.currentSpeed * 10))
            
            if (shouldSpawn && Random.nextFloat() < GameConfig.SPAWN_CHANCE) {
                nextObstacles.add(
                    Obstacle(
                        id = obstacleIdCounter++,
                        x = 2500f + Random.nextFloat() * 500,
                        width = 50f + Random.nextFloat() * 30,
                        height = 70f + Random.nextFloat() * 50
                    )
                )
            }

            // --- 5. Return New State ---
            if (isHit) {
                current.copy(
                    playerY = newY,
                    obstacles = nextObstacles,
                    isGameOver = true
                )
            } else {
                current.copy(
                    playerY = newY,
                    velocityY = newVel,
                    obstacles = nextObstacles,
                    score = current.score + scoreIncrement,
                    currentSpeed = current.currentSpeed + speedIncrement,
                    worldTick = nanos
                )
            }
        }
    }
}