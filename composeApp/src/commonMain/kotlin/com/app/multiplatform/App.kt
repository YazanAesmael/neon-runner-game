package com.app.multiplatform

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.multiplatform.engine.GameConfig
import com.app.multiplatform.engine.GameState
import com.app.multiplatform.viewmodel.GameViewModel

@Composable
fun App() {
    MaterialTheme {
        // Initialize the ViewModel
        val viewModel: GameViewModel = viewModel()
        NeonRunnerScreen(viewModel)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NeonRunnerScreen(viewModel: GameViewModel) {
    // Collect the observable state
    val gameState by viewModel.gameState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    // Game Loop Driver
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        while (true) {
            // If game is running, use high-precision Nano timer
            if (!gameState.isGameOver) {
                withFrameNanos { time ->
                    viewModel.onGameTick(time)
                }
            } else {
                // Low power mode when dead
                withInfiniteAnimationFrameMillis { 
                     // Just to keep background animating slowly if we wanted
                }
            }
        }
    }

    // --- UI ROOT ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF121212), Color(0xFF2D2D44))))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding() // Fixes status bar overlap
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { viewModel.onJump() }
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent {
                    if (it.key == Key.Spacebar || it.key == Key.DirectionUp) {
                        viewModel.onJump()
                        true
                    } else false
                }
        ) {
            val screenW = maxWidth.value * 2.5f
            val screenH = maxHeight.value * 2.5f
            val groundY = screenH * GameConfig.GROUND_HEIGHT_RATIO

            // --- RENDERER ---
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawBackground(screenW, screenH, groundY, gameState.worldTick)
                drawPlayer(gameState, groundY)
                drawObstacles(gameState, groundY)
            }

            // --- HUD ---
            GameHUD(gameState)

            // --- OVERLAYS ---
            if (gameState.isGameOver) {
                GameOverOverlay()
            }
        }
    }
}

@Composable
fun GameHUD(state: GameState) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Text(
                text = "SCORE: ${state.score}",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "SPEED: ${state.currentSpeed.toInt()}",
                color = Color(0xFF00E5FF),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun GameOverOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "GAME OVER",
                color = Color(0xFFFF0055),
                fontSize = 48.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Tap to Retry",
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

// --- DRAWING IMPLEMENTATION ---

fun DrawScope.drawBackground(w: Float, h: Float, groundY: Float, tick: Long) {
    // Neon Ground
    drawRect(
        color = Color(0xFF00E5FF),
        topLeft = Offset(0f, groundY),
        size = Size(w, 5f)
    )

    // Retro Grid Effect
    val scrollOffset = (tick / 1_000_000 / 2) % 400 
    for (i in 0..25) {
        val x = -400 + (i * 200) + scrollOffset
        drawLine(
            color = Color(0x3300E5FF),
            start = Offset(x.toFloat(), groundY),
            end = Offset(x.toFloat() - 300, h),
            strokeWidth = 2f
        )
    }
}

fun DrawScope.drawPlayer(state: GameState, groundY: Float) {
    val size = GameConfig.PLAYER_SIZE
    val x = 150f
    val y = groundY - size - state.playerY

    // Glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x88FF00AA), Color.Transparent),
            center = Offset(x + size/2, y + size/2),
            radius = size * 1.5f
        ),
        center = Offset(x + size/2, y + size/2),
        radius = size * 1.5f
    )

    // Body
    drawRect(
        color = Color(0xFFFF00AA),
        topLeft = Offset(x, y),
        size = Size(size, size)
    )
}

fun DrawScope.drawObstacles(state: GameState, groundY: Float) {
    state.obstacles.forEach { obs ->
        val obsY = groundY - obs.height
        val path = Path().apply {
            moveTo(obs.x, groundY)
            lineTo(obs.x + obs.width / 2, obsY)
            lineTo(obs.x + obs.width, groundY)
            close()
        }
        drawPath(path = path, color = obs.color, style = Fill)
    }
}