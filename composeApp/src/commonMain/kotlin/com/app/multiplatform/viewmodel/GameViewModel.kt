package com.app.multiplatform.viewmodel

import androidx.lifecycle.ViewModel
import com.app.multiplatform.engine.GameEngine
import com.app.multiplatform.engine.NeonPhysicsEngine

class GameViewModel : ViewModel() {
    private val engine: GameEngine = NeonPhysicsEngine()
    val gameState = engine.gameState

    fun onPressJump() = engine.jumpStart()
    fun onReleaseJump() = engine.jumpEnd()
    fun onPressDown() = engine.fastFall()
    fun onGameTick(nanos: Long) = engine.tick(nanos)
}