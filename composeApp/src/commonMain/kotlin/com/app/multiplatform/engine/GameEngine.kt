package com.app.multiplatform.engine

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

interface GameEngine {
    val gameState: StateFlow<GameState>
    fun jumpStart()
    fun jumpEnd() // For variable jump height
    fun fastFall()
    fun reset()
    fun tick(nanos: Long)
}

class NeonPhysicsEngine : GameEngine {
    private val _state = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState> = _state.asStateFlow()

    private var lastTime = 0L
    private var entityIdCounter = 0L
    private var holdingJump = false

    override fun jumpStart() {
        if (_state.value.isGameOver) {
            reset()
            return
        }
        _state.update { s ->
            if (s.isGrounded) {
                // Spawn jump particles
                val parts = s.particles + createExplosion(s.playerY, Color.White, 5)
                s.copy(
                    velocityY = -GameConfig.JUMP_VELOCITY, // Up is Negative in this coord system?
                    // Let's stick to standard: 0 is Top, 1000 is Bottom.
                    // So Ground is 750. Jump means decreasing Y.
                    isGrounded = false,
                    particles = parts
                )
            } else s
        }
        holdingJump = true
    }

    override fun jumpEnd() {
        holdingJump = false
        _state.update { s ->
            // If releasing jump early while moving up, cut velocity (Variable Jump Height)
            if (s.velocityY < -300f) {
                s.copy(velocityY = s.velocityY * 0.5f)
            } else s
        }
    }

    override fun fastFall() {
        _state.update { s ->
            if (!s.isGrounded) s.copy(velocityY = s.velocityY + 800f) else s
        }
    }

    override fun reset() {
        _state.value = GameState()
        lastTime = 0L
    }

    override fun tick(nanos: Long) {
        if (lastTime == 0L) { lastTime = nanos; return }

        // Calculate Delta Time in Seconds (Essential for smooth physics)
        val dt = ((nanos - lastTime) / 1_000_000_000f).coerceAtMost(0.1f)
        lastTime = nanos

        _state.update { s ->
            if (s.isGameOver) return@update s.copy(cameraShake = (s.cameraShake - dt * 5).coerceAtLeast(0f))

            // 1. Update Speed
            val newSpeed = (s.currentSpeed + GameConfig.ACCELERATION * dt).coerceAtMost(GameConfig.MAX_SPEED)

            // 2. Physics (Player)
            var vy = s.velocityY + (GameConfig.GRAVITY * dt)
            // Lighter gravity if holding jump (floaty feel)
            if (holdingJump && vy < 0) vy -= (GameConfig.GRAVITY * 0.3f * dt)

            var py = s.playerY + (vy * dt)
            var grounded = false

            // Ground Collision
            if (py >= GameConfig.GROUND_Y) {
                py = GameConfig.GROUND_Y
                vy = 0f
                grounded = true

                // Land particles (only if just landed)
                if (!s.isGrounded) {
                    // Could add landing puff here
                }
            }

            // 3. Move Entities
            val nextObstacles = s.obstacles.mapNotNull { obs ->
                val nx = obs.x - (newSpeed * dt)
                if (nx < -200f) null else obs.copy(x = nx)
            }.toMutableList()

            val nextOrbs = s.orbs.mapNotNull { orb ->
                val nx = orb.x - (newSpeed * dt)
                if (nx < -200f || orb.isCollected) null else orb.copy(x = nx)
            }.toMutableList()

            // 4. Particles Animation
            val nextParticles = s.particles.mapNotNull { p ->
                val life = p.life - dt * 2f // Fade out speed
                if (life <= 0) null else p.copy(
                    x = p.x + p.vx * dt,
                    y = p.y + p.vy * dt,
                    life = life
                )
            }.toMutableList()

            // Add trail particles
            if (!grounded && s.worldTick % 5 == 0L) {
                nextParticles.add(createParticle(GameConfig.PLAYER_X + 30f, py + 30f, Color(0xFFFF00AA)))
            }

            // 5. Spawning
            spawnManager(s, nextObstacles, nextOrbs)

            // 6. Collision & Scoring
            var shake = (s.cameraShake - dt * 5).coerceAtLeast(0f)
            var gameOver = false
            var score = s.score + (1 * (newSpeed/1000f)).toLong() // Score based on distance/speed

            // AABB Player Box
            val pl = GameConfig.PLAYER_X + 10f
            val pr = GameConfig.PLAYER_X + GameConfig.PLAYER_SIZE - 10f
            val pt = py - GameConfig.PLAYER_SIZE + 10f
            val pb = py - 5f // Feet slightly higher

            // Check Obstacles
            for (obs in nextObstacles) {
                val ol = obs.x; val or = obs.x + obs.width
                val ot = GameConfig.GROUND_Y - obs.height

                // Float obstacles have a bottom
                val ob = if (obs.type == ObstacleType.FLOATING) ot + obs.height else GameConfig.GROUND_Y

                if (pr > ol && pl < or && pb > ot && pt < ob) {
                    gameOver = true
                    shake = 1.0f // Heavy impact
                    // Explosion of particles
                    nextParticles.addAll(createExplosion(py, Color(0xFFFF00AA), 20))
                    break
                }
            }

            // Check Orbs
            val it = nextOrbs.iterator()
            while(it.hasNext()) {
                val orb = it.next()
                val ox = orb.x; val oy = orb.y
                // Circle collision approx
                if (kotlin.math.abs(pl - ox) < 60f && kotlin.math.abs(pt - oy) < 60f) {
                    score += 500
                    nextParticles.addAll(createExplosion(oy, Color.Yellow, 10))
                    it.remove()
                }
            }

            s.copy(
                playerY = py, velocityY = vy, isGrounded = grounded,
                obstacles = nextObstacles, particles = nextParticles, orbs = nextOrbs,
                isGameOver = gameOver, score = score, currentSpeed = newSpeed,
                worldTick = nanos, cameraShake = shake
            )
        }
    }

    private fun spawnManager(s: GameState, obstacles: MutableList<Obstacle>, orbs: MutableList<Orb>) {
        val lastObsX = obstacles.lastOrNull()?.x ?: 0f
        // Spawn Horizon is off-screen (approx width 2000 units usually)
        if (lastObsX < 2000f && Random.nextFloat() < 0.02f) {
            val type = ObstacleType.entries.toTypedArray().random()
            val width = if (type == ObstacleType.BLOCK) 80f else 50f
            val height = if (type == ObstacleType.FLOATING) 80f else Random.nextFloat() * 60f + 60f

            obstacles.add(Obstacle(
                id = entityIdCounter++, x = 2500f, width = width, height = height, type = type
            ))

            // Maybe spawn an orb above it
            if (Random.nextBoolean()) {
                orbs.add(Orb(entityIdCounter++, 2500f + 20f, GameConfig.GROUND_Y - height - 150f))
            }
        }
    }

    private fun createExplosion(y: Float, color: Color, count: Int): List<Particle> {
        return List(count) {
            createParticle(GameConfig.PLAYER_X + 30f, y - 30f, color, true)
        }
    }

    private fun createParticle(x: Float, y: Float, color: Color, burst: Boolean = false): Particle {
        val vx = if (burst) (Random.nextFloat() - 0.5f) * 600f else -200f
        val vy = if (burst) (Random.nextFloat() - 0.5f) * 600f else (Random.nextFloat() - 0.5f) * 50f
        return Particle(entityIdCounter++, x, y, vx, vy, 1.0f, color, if (burst) 8f else 4f)
    }
}