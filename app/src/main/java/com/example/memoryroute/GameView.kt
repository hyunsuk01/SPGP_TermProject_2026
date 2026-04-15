package com.example.memoryroute

import android.content.Context
import android.graphics.Bitmap
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
        Stage(4),
        Stage(5),
        Stage(7)
    )

    private var stageIndex = 0

    private var currentStage = stages[stageIndex]

    enum class Direction {
        UP,
        DOWN,
        LEFT,
        RIGHT
    }

    private val background =
        BitmapFactory.decodeResource(resources, R.drawable.city1)

    private val idleBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.idle)

    private val runBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.run)

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

    private var playerX = 300f
    private var playerY = 300f

    private var targetX = playerX
    private var targetY = playerY

    private var isMoving = false
    private var facingRight = true

    private enum class State {
        IDLE,
        RUN
    }

    private var state = State.IDLE

    private var currentFrame = 0
    private var frameTimer = 0L

    private val frameDelay = 120L

    private val moveHistory = mutableListOf<Direction>()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawBackground(canvas)

        updateMovement()
        updateAnimation()

        drawPlayer(canvas)

        drawMoveSlots(canvas)

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

            state = State.IDLE
        }
    }

    private fun updateAnimation() {

        val currentTime = System.currentTimeMillis()

        if (currentTime - frameTimer > frameDelay) {

            frameTimer = currentTime

            val frameCount =
                if (state == State.IDLE) 4 else 6

            currentFrame =
                (currentFrame + 1) % frameCount
        }
    }

    private fun drawPlayer(canvas: Canvas) {

        val bitmap =
            if (state == State.IDLE) idleBitmap else runBitmap

        val frameCount =
            if (state == State.IDLE) 4 else 6

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

                val iconBitmap = when (moveHistory[i]) {

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
    }

    fun moveRight() {

        if (isMoving) return

        recordMove(Direction.RIGHT)

        facingRight = true

        targetX += tileSize

        isMoving = true

        state = State.RUN
    }

    fun moveLeft() {

        if (isMoving) return

        recordMove(Direction.LEFT)

        facingRight = false

        targetX -= tileSize

        isMoving = true

        state = State.RUN
    }

    fun moveUp() {

        if (isMoving) return

        recordMove(Direction.UP)

        targetY -= tileSize

        isMoving = true

        state = State.RUN
    }

    fun moveDown() {

        if (isMoving) return

        recordMove(Direction.DOWN)

        targetY += tileSize

        isMoving = true

        state = State.RUN
    }
}