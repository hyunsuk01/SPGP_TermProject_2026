package com.example.memoryroute

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Stage(
        val moveLimit: Int
    )

    private val stages = listOf(
        Stage(4)
    )

    private var stageIndex = 0

    private var currentStage = stages[stageIndex]

    enum class Direction {
        UP,
        DOWN,
        LEFT,
        RIGHT
    }

    private enum class State {
        IDLE,
        RUN,
        DEATH
    }

    private val background =
        BitmapFactory.decodeResource(resources, R.drawable.city1)

    private val idleBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.idle)

    private val runBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.run)

    private val deathBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.death)

    private val shadowIdleBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.shadow_idle)

    private val shadowRunBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.shadow_run)

    private val shadowDeathBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.shadow_death)

    private val emptySlotBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.empty_slot)

    private val upIconBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.up)

    private val downIconBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.down)

    private val leftIconBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.left)

    private val rightIconBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.right)

    private val paint = Paint().apply {
        isFilterBitmap = false
    }

    private val tileSize = 150f

    private val startX = 300f
    private val startY = 300f

    private var playerX = startX
    private var playerY = startY

    private var targetX = playerX
    private var targetY = playerY

    private var isMoving = false

    private var facingRight = true

    private var state = State.IDLE

    private var shadowX = startX
    private var shadowY = startY

    private var shadowTargetX = shadowX
    private var shadowTargetY = shadowY

    private var shadowMoving = false

    private var shadowFacingRight = true

    private var shadowVisible = false

    private var shadowReady = false

    private var shadowState = State.IDLE

    private var currentFrame = 0

    private var shadowFrame = 0

    private var frameTimer = 0L

    private val frameDelay = 120L

    private val moveHistory = mutableListOf<Direction>()

    private var replayIndex = 0

    private var isDead = false

    private var deathStartTime = 0L

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawBackground(canvas)

        updateMovement()

        updateShadowMovement()

        updateAnimation()

        checkCollision()

        drawPlayer(canvas)

        drawShadow(canvas)

        drawMoveSlots(canvas)

        updateDeath()

        invalidate()
    }

    private fun drawBackground(canvas: Canvas) {

        val scale = width.toFloat() / background.width

        val scaledHeight = (background.height * scale).toInt()

        val dst = Rect(
            0,
            0,
            width,
            scaledHeight
        )

        canvas.drawBitmap(background, null, dst, paint)
    }

    private fun updateMovement() {

        if (!isMoving) return

        val speed = 8f

        if (playerX < targetX) {
            playerX += speed
            if (playerX >= targetX) {
                playerX = targetX
            }
        }

        if (playerX > targetX) {
            playerX -= speed
            if (playerX <= targetX) {
                playerX = targetX
            }
        }

        if (playerY < targetY) {
            playerY += speed
            if (playerY >= targetY) {
                playerY = targetY
            }
        }

        if (playerY > targetY) {
            playerY -= speed
            if (playerY <= targetY) {
                playerY = targetY
            }
        }

        if (playerX == targetX && playerY == targetY) {

            isMoving = false

            if (state != State.DEATH) {
                state = State.IDLE
                currentFrame = 0
            }
        }

        checkBoundary()
    }

    private fun updateShadowMovement() {

        if (!shadowMoving) return

        val speed = 8f

        if (shadowX < shadowTargetX) {
            shadowX += speed
            if (shadowX >= shadowTargetX) {
                shadowX = shadowTargetX
            }
        }

        if (shadowX > shadowTargetX) {
            shadowX -= speed
            if (shadowX <= shadowTargetX) {
                shadowX = shadowTargetX
            }
        }

        if (shadowY < shadowTargetY) {
            shadowY += speed
            if (shadowY >= shadowTargetY) {
                shadowY = shadowTargetY
            }
        }

        if (shadowY > shadowTargetY) {
            shadowY -= speed
            if (shadowY <= shadowTargetY) {
                shadowY = shadowTargetY
            }
        }

        if (shadowX == shadowTargetX && shadowY == shadowTargetY) {

            shadowMoving = false

            if (shadowState != State.DEATH) {
                shadowState = State.IDLE
                shadowFrame = 0
            }
        }

        checkBoundary()
    }

    private fun updateAnimation() {

        val currentTime = System.currentTimeMillis()

        if (currentTime - frameTimer > frameDelay) {

            frameTimer = currentTime

            val playerFrameCount =
                when (state) {
                    State.IDLE -> 4
                    State.RUN -> 6
                    State.DEATH -> 8
                }

            currentFrame =
                (currentFrame + 1) % playerFrameCount

            val shadowFrameCount =
                when (shadowState) {
                    State.IDLE -> 4
                    State.RUN -> 6
                    State.DEATH -> 8
                }

            shadowFrame =
                (shadowFrame + 1) % shadowFrameCount
        }
    }

    private fun drawPlayer(canvas: Canvas) {

        val bitmap =
            when (state) {
                State.IDLE -> idleBitmap
                State.RUN -> runBitmap
                State.DEATH -> deathBitmap
            }

        val frameCount =
            when (state) {
                State.IDLE -> 4
                State.RUN -> 6
                State.DEATH -> 8
            }

        val frameWidth = bitmap.width / frameCount

        val frameHeight = bitmap.height

        val src = Rect(
            currentFrame * frameWidth,
            0,
            (currentFrame + 1) * frameWidth,
            frameHeight
        )

        val dst = RectF(
            playerX,
            playerY,
            playerX + 150f,
            playerY + 150f
        )

        if (facingRight) {

            canvas.drawBitmap(bitmap, src, dst, paint)

        } else {

            canvas.save()

            canvas.scale(
                -1f,
                1f,
                playerX + 75f,
                playerY + 75f
            )

            canvas.drawBitmap(bitmap, src, dst, paint)

            canvas.restore()
        }
    }

    private fun drawShadow(canvas: Canvas) {

        if (!shadowVisible) return

        val bitmap =
            when (shadowState) {
                State.IDLE -> shadowIdleBitmap
                State.RUN -> shadowRunBitmap
                State.DEATH -> shadowDeathBitmap
            }

        val frameCount =
            when (shadowState) {
                State.IDLE -> 4
                State.RUN -> 6
                State.DEATH -> 8
            }

        val frameWidth = bitmap.width / frameCount

        val frameHeight = bitmap.height

        val src = Rect(
            shadowFrame * frameWidth,
            0,
            (shadowFrame + 1) * frameWidth,
            frameHeight
        )

        val dst = RectF(
            shadowX,
            shadowY,
            shadowX + 150f,
            shadowY + 150f
        )

        if (shadowFacingRight) {

            canvas.drawBitmap(bitmap, src, dst, paint)

        } else {

            canvas.save()

            canvas.scale(
                -1f,
                1f,
                shadowX + 75f,
                shadowY + 75f
            )

            canvas.drawBitmap(bitmap, src, dst, paint)

            canvas.restore()
        }
    }

    private fun drawMoveSlots(canvas: Canvas) {

        val slotSize = 120f

        val totalWidth =
            currentStage.moveLimit * slotSize

        val startX =
            (width - totalWidth) / 2f

        val y = 40f

        for (i in 0 until currentStage.moveLimit) {

            val left = startX + i * slotSize

            val dst = RectF(
                left,
                y,
                left + 110f,
                y + 110f
            )

            canvas.drawBitmap(
                emptySlotBitmap,
                null,
                dst,
                paint
            )

            if (i < moveHistory.size) {

                val iconBitmap =
                    when (moveHistory[i]) {

                        Direction.UP -> upIconBitmap

                        Direction.DOWN -> downIconBitmap

                        Direction.LEFT -> leftIconBitmap

                        Direction.RIGHT -> rightIconBitmap
                    }

                canvas.drawBitmap(
                    iconBitmap,
                    null,
                    dst,
                    paint
                )
            }
        }
    }

    private fun recordMove(direction: Direction) {

        if (moveHistory.size >= currentStage.moveLimit) {
            return
        }

        moveHistory.add(direction)

        if (
            moveHistory.size == currentStage.moveLimit &&
            !shadowVisible
        ) {

            shadowVisible = true

            shadowReady = true

            shadowX = startX
            shadowY = startY

            shadowTargetX = shadowX
            shadowTargetY = shadowY
        }
    }

    private fun moveShadow() {

        if (!shadowVisible) return

        if (shadowReady) {

            shadowReady = false

            return
        }

        if (replayIndex >= moveHistory.size) {
            return
        }

        when (moveHistory[replayIndex]) {

            Direction.RIGHT -> {

                shadowTargetX += tileSize

                shadowFacingRight = true
            }

            Direction.LEFT -> {

                shadowTargetX -= tileSize

                shadowFacingRight = false
            }

            Direction.UP -> {
                shadowTargetY -= tileSize
            }

            Direction.DOWN -> {
                shadowTargetY += tileSize
            }
        }

        shadowMoving = true

        shadowState = State.RUN

        shadowFrame = 0

        replayIndex++
    }

    private fun checkBoundary() {

        if (isDead) return

        if (
            playerX < 0f ||
            playerY < 0f ||
            playerX + 150f > width ||
            playerY + 150f > height
        ) {
            startDeath()
        }

        if (
            shadowVisible &&
            (
                    shadowX < 0f ||
                            shadowY < 0f ||
                            shadowX + 150f > width ||
                            shadowY + 150f > height
                    )
        ) {
            startDeath()
        }
    }

    private fun checkCollision() {

        if (!shadowVisible) return

        if (isDead) return

        val distanceX = kotlin.math.abs(playerX - shadowX)
        val distanceY = kotlin.math.abs(playerY - shadowY)

        if (distanceX < 80f && distanceY < 80f) {
            startDeath()
        }
    }

    private fun startDeath() {

        isDead = true

        deathStartTime = System.currentTimeMillis()

        state = State.DEATH
        shadowState = State.DEATH

        currentFrame = 0
        shadowFrame = 0

        isMoving = false
        shadowMoving = false
    }

    private fun updateDeath() {

        if (!isDead) return

        val currentTime = System.currentTimeMillis()

        if (currentTime - deathStartTime > 1000L) {
            resetStage()
        }
    }

    private fun resetStage() {

        playerX = startX
        playerY = startY

        targetX = playerX
        targetY = playerY

        shadowX = startX
        shadowY = startY

        shadowTargetX = shadowX
        shadowTargetY = shadowY

        moveHistory.clear()

        replayIndex = 0

        shadowVisible = false

        shadowReady = false

        isMoving = false
        shadowMoving = false

        state = State.IDLE
        shadowState = State.IDLE

        currentFrame = 0
        shadowFrame = 0

        isDead = false
    }

    fun moveRight() {

        if (isMoving || isDead) return

        recordMove(Direction.RIGHT)

        facingRight = true

        targetX += tileSize

        moveShadow()

        isMoving = true

        state = State.RUN

        currentFrame = 0
    }

    fun moveLeft() {

        if (isMoving || isDead) return

        recordMove(Direction.LEFT)

        facingRight = false

        targetX -= tileSize

        moveShadow()

        isMoving = true

        state = State.RUN

        currentFrame = 0
    }

    fun moveUp() {

        if (isMoving || isDead) return

        recordMove(Direction.UP)

        targetY -= tileSize

        moveShadow()

        isMoving = true

        state = State.RUN

        currentFrame = 0
    }

    fun moveDown() {

        if (isMoving || isDead) return

        recordMove(Direction.DOWN)

        targetY += tileSize

        moveShadow()

        isMoving = true

        state = State.RUN

        currentFrame = 0
    }
}