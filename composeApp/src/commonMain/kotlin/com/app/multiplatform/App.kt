package com.app.multiplatform

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.multiplatform.engine.GameConfig
import com.app.multiplatform.engine.GameState
import com.app.multiplatform.engine.ObstacleType
import com.app.multiplatform.viewmodel.GameViewModel
import kotlin.random.Random

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App() {
    val viewModel: GameViewModel = viewModel()
    val gameState by viewModel.gameState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    // Desktop Key Handling
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Touch/Click Handling for Variable Jump
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> viewModel.onPressJump()
                is PressInteraction.Release -> viewModel.onReleaseJump()
                is PressInteraction.Cancel -> viewModel.onReleaseJump()
            }
        }
    }

    // Game Loop
    LaunchedEffect(Unit) {
        while (true) {
            if (!gameState.isGameOver) withFrameNanos { viewModel.onGameTick(it) }
            else withInfiniteAnimationFrameMillis { /* Idle */ }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Letterbox bars color
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .focusRequester(focusRequester)
                .focusable()
                .clickable(interactionSource = interactionSource, indication = null) {}
                .onKeyEvent {
                    if (it.type == KeyEventType.KeyDown) {
                        when (it.key) {
                            Key.Spacebar, Key.DirectionUp -> { viewModel.onPressJump(); true }
                            Key.DirectionDown -> { viewModel.onPressDown(); true }
                            else -> false
                        }
                    } else if (it.type == KeyEventType.KeyUp) {
                        if (it.key == Key.Spacebar || it.key == Key.DirectionUp) {
                            viewModel.onReleaseJump()
                            true
                        } else false
                    } else false
                }
        ) {
            // --- SCALING LOGIC ---
            // We map World Height (1000) to Screen Height.
            val scale = maxHeight.value / GameConfig.WORLD_HEIGHT * 2.5f // Density approx
            val shakeOffset = if (gameState.cameraShake > 0) Random.nextFloat() * 10f * gameState.cameraShake else 0f

            Canvas(modifier = Modifier.fillMaxSize()) {
                // Apply Camera Shake & Global Scaling
                withTransform({
                    translate(left = shakeOffset, top = shakeOffset)
                    scale(scale, scale, pivot = Offset.Zero)
                }) {
                    drawWorld(gameState)
                }
            }

            // HUD (Drawn on top, unscaled or scaled separately)
            GameHUD(gameState)

            if (gameState.isGameOver) GameOverOverlay()
        }
    }
}

// --- RENDERER ---
fun DrawScope.drawWorld(state: GameState) {
    // 1. Parallax Backgrounds
    drawParallaxLayer(state.worldTick, 0.2f, Color(0xFF1A1A2E)) // Distant
    drawParallaxLayer(state.worldTick, 0.5f, Color(0xFF16213E)) // Mid

    // 2. Ground
    val groundColor = Color(0xFF0F3460)
    drawRect(
        color = groundColor,
        topLeft = Offset(-100f, GameConfig.GROUND_Y), // Extend left/right for shake
        size = Size(10000f, 500f) // Infinite floor look
    )
    // Neon Line on top of ground
    drawLine(
        brush = Brush.horizontalGradient(listOf(Color(0xFFE94560), Color(0xFF533483))),
        start = Offset(0f, GameConfig.GROUND_Y),
        end = Offset(5000f, GameConfig.GROUND_Y),
        strokeWidth = 8f
    )

    // 3. Orbs (Behind obstacles)
    state.orbs.forEach { orb ->
        drawCircle(
            color = Color(0xFFFFD700),
            radius = 20f,
            center = Offset(orb.x, orb.y)
        )
        // Glow
        drawCircle(
            color = Color(0x55FFD700),
            radius = 35f,
            center = Offset(orb.x, orb.y)
        )
    }

    // 4. Obstacles
    state.obstacles.forEach { obs ->
        val obsY = if (obs.type == ObstacleType.FLOATING) GameConfig.GROUND_Y - obs.height - 150f else GameConfig.GROUND_Y - obs.height

        when (obs.type) {
            ObstacleType.SPIKE -> {
                val path = Path().apply {
                    moveTo(obs.x, GameConfig.GROUND_Y)
                    lineTo(obs.x + obs.width / 2, GameConfig.GROUND_Y - obs.height)
                    lineTo(obs.x + obs.width, GameConfig.GROUND_Y)
                    close()
                }
                drawPath(path, Color(0xFFE94560))
                drawPath(path, Color.White, style = Stroke(width = 3f))
            }
            ObstacleType.BLOCK -> {
                drawRect(Color(0xFF533483), topLeft = Offset(obs.x, GameConfig.GROUND_Y - obs.height), size = Size(obs.width, obs.height))
                drawRect(Color(0xFFE94560), topLeft = Offset(obs.x, GameConfig.GROUND_Y - obs.height), size = Size(obs.width, obs.height), style = Stroke(3f))
            }
            ObstacleType.FLOATING -> {
                drawRect(Color(0xFF0F3460), topLeft = Offset(obs.x, obsY), size = Size(obs.width, obs.height))
                drawRect(Color(0xFF00FF00), topLeft = Offset(obs.x, obsY), size = Size(obs.width, obs.height), style = Stroke(3f))
            }
        }
    }

    // 5. Player
    val playerY = state.playerY - GameConfig.PLAYER_SIZE
    // Trail Effect (Previous positions could be stored, but we use particles now)

    // Main Body
    drawRect(
        color = Color(0xFFE94560),
        topLeft = Offset(GameConfig.PLAYER_X, playerY),
        size = Size(GameConfig.PLAYER_SIZE, GameConfig.PLAYER_SIZE)
    )
    // Inner Glow
    drawRect(
        color = Color.White,
        topLeft = Offset(GameConfig.PLAYER_X + 10f, playerY + 10f),
        size = Size(GameConfig.PLAYER_SIZE - 20f, GameConfig.PLAYER_SIZE - 20f),
        alpha = 0.5f
    )

    // 6. Particles
    state.particles.forEach { p ->
        drawCircle(
            color = p.color.copy(alpha = p.life),
            radius = p.size * p.life,
            center = Offset(p.x, p.y)
        )
    }
}

fun DrawScope.drawParallaxLayer(tick: Long, speed: Float, color: Color) {
    val scroll = (tick / 1_000_000 * speed * 0.5f) % 1000f
    for (i in 0..10) {
        drawRect(
            color = color,
            topLeft = Offset((i * 400f) - scroll, 0f),
            size = Size(150f, GameConfig.GROUND_Y)
        )
    }
}

// Reuse the HUD and Overlay from previous steps (they are perfect)
@Composable
fun GameHUD(state: GameState) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Column {
            Text("SCORE: ${state.score}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("SPEED: ${state.currentSpeed.toInt()}", color = Color(0xFF00E5FF))
        }
    }
}

@Composable
fun GameOverOverlay() {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.8f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CRASHED", color = Color(0xFFE94560), fontSize = 50.sp, fontWeight = FontWeight.Black)
            Text("Tap to Retry", color = Color.White, fontSize = 20.sp)
        }
    }
}