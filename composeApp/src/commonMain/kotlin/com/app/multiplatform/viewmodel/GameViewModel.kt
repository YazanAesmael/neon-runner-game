package com.app.multiplatform.viewmodel

import androidx.lifecycle.ViewModel
import com.app.multiplatform.engine.GameEngine
import com.app.multiplatform.engine.GameState
import com.app.multiplatform.engine.NeonPhysicsEngine
import kotlinx.coroutines.flow.StateFlow

class GameViewModel : ViewModel() {
    private val engine: GameEngine = NeonPhysicsEngine()

    val gameState: StateFlow<GameState> = engine.gameState

    fun onJump() {
        val currentState = gameState.value
        if (currentState.isGameOver) {
            engine.reset()
        } else {
            engine.jump()
        }
    }

    fun onGameTick(nanos: Long) {
        engine.tick(nanos)
    }
}