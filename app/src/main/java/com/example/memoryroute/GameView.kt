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

    private val background =
        BitmapFactory.decodeResource(resources, R.drawable.city1)

    private val idleBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.idle)

    private val runBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.run)

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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawBackground(canvas)

        updateMovement()
        updateAnimation()

        drawPlayer(canvas)

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
                playerX + 48f,
                playerY + 48f
            )

            canvas.drawBitmap(bitmap, src, dst, paint)

            canvas.restore()
        }
    }

    fun moveRight() {

        if (isMoving) return

        facingRight = true

        targetX += tileSize

        isMoving = true
        state = State.RUN
    }

    fun moveLeft() {

        if (isMoving) return

        facingRight = false

        targetX -= tileSize

        isMoving = true
        state = State.RUN
    }

    fun moveUp() {

        if (isMoving) return

        targetY -= tileSize

        isMoving = true
        state = State.RUN
    }

    fun moveDown() {

        if (isMoving) return

        targetY += tileSize

        isMoving = true
        state = State.RUN
    }
}