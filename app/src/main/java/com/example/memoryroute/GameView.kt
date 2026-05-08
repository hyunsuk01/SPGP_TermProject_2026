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
        DEATH,
        CLIMB
    }

    private val background =
        BitmapFactory.decodeResource(resources, R.drawable.city1)

    private val idleBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.idle)

    private val runBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.run)

    private val deathBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.death)

    private val climbBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.climb)

    private val shadowIdleBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.shadow_idle)

    private val shadowRunBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.shadow_run)

    private val shadowDeathBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.shadow_death)

    private val shadowClimbBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.shadow_climb)

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

    private val flagBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.flag)

    private val floorBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.floor)

    private val ladderBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.ladder)

    private val buttonBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.button)

    private val buttonFloorBitmap =
        BitmapFactory.decodeResource(resources, R.drawable.button_floor)

    private val paint = Paint().apply {
        isFilterBitmap = false
    }

    private val characterSize = 150f

    private var tileSizeX = 0f
    private var tileSizeY = 150f

    private var startX = 300f
    private var startY = 300f
    private var visualFloorOffset = -100f

    private var playerX = 0f
    private var playerY = 0f

    private var targetX = 0f
    private var targetY = 0f

    private var isMoving = false

    private var facingRight = true

    private var state = State.IDLE

    private var shadowX = 0f
    private var shadowY = 0f

    private var shadowTargetX = 0f
    private var shadowTargetY = 0f

    private var shadowMoving = false

    private var shadowFacingRight = true

    private var shadowVisible = false

    private var shadowReady = false

    private var shadowState = State.IDLE

    private var currentFrame = 0

    private var shadowFrame = 0

    private var flagFrame = 0

    private var buttonFrame = 0

    private var frameTimer = 0L

    private val frameDelay = 120L

    private val moveHistory = mutableListOf<Direction>()

    private var replayIndex = 0

    private var isDead = false

    private var deathStartTime = 0L

    private val mapCols = 6
    private val mapRows = 4
    private var isSwitchPressed = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        tileSizeX = (w * 0.75f) / mapCols
        resetPosition()
    }

    private fun resetPosition() {
        playerX = startX
        playerY = startY
        targetX = playerX
        targetY = playerY
        shadowX = startX
        shadowY = startY
        shadowTargetX = shadowX
        shadowTargetY = shadowY
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawBackground(canvas)

        drawMapAssets(canvas)

        updateMovement()

        updateShadowMovement()

        updateAnimation()

        checkCollision()

        drawCharacter(canvas, playerX, playerY, state, currentFrame, facingRight, false)

        drawCharacter(canvas, shadowX, shadowY, shadowState, shadowFrame, shadowFacingRight, true)

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

    private fun drawMapAssets(canvas: Canvas) {
        for (i in 0 until 36) {
            val col = i % mapCols
            val row = i / mapCols
            val x = startX + col * tileSizeX
            val y = startY + row * tileSizeY

            val floorHeight = tileSizeY * 0.5f

            val floorTop = y + (tileSizeY - floorHeight) + visualFloorOffset * 2.5f

            val floorDst = RectF(x, floorTop, x + tileSizeX, floorTop + floorHeight)

            when (i + 1) {
                7, 8, 9, 10, 12, 19, 20, 21, 22, 23, 24, 31, 34 -> {
                    canvas.drawBitmap(floorBitmap, null, floorDst, paint)
                }
                11 -> {
                    val bFloor = if (isSwitchPressed) floorBitmap else buttonFloorBitmap
                    canvas.drawBitmap(bFloor, null, floorDst, paint)
                }
                6 -> {
                    val fWidth = flagBitmap.width / 9
                    val fSrc = Rect(flagFrame * fWidth, 0, (flagFrame + 1) * fWidth, flagBitmap.height)

                    val flagH = tileSizeY * 0.6f
                    val objectBottom = floorTop - visualFloorOffset * 1.5f

                    val flagDst = RectF(x, objectBottom - flagH, x + tileSizeX, objectBottom)
                    canvas.drawBitmap(flagBitmap, fSrc, flagDst, paint)
                }
                17 -> {
                    val bWidth = buttonBitmap.width / 4
                    val bSrc = Rect(buttonFrame * bWidth, 0, (buttonFrame + 1) * bWidth, buttonBitmap.height)

                    val btnW = tileSizeX * 0.4f
                    val btnH = tileSizeY * 0.4f
                    val objectBottom = floorTop - visualFloorOffset * 1.5f

                    val btnDst = RectF(x + (tileSizeX - btnW) / 2f, objectBottom - btnH, x + (tileSizeX + btnW) / 2f, objectBottom)
                    canvas.drawBitmap(buttonBitmap, bSrc, btnDst, paint)
                }
                14 -> {
                    val ladderW = tileSizeX * 0.4f
                    val objectBottom = floorTop - visualFloorOffset * 1.5f

                    val upperFloorTop = floorTop - tileSizeY
                    val ladderDst = RectF(x + (tileSizeX - ladderW) / 2f, upperFloorTop, x + (tileSizeX + ladderW) / 2f, objectBottom)
                    canvas.drawBitmap(ladderBitmap, null, ladderDst, paint)
                }
            }
        }
    }

    private fun drawCharacter(canvas: Canvas, x: Float, y: Float, curState: State, frame: Int, isRight: Boolean, isShadow: Boolean) {
        if (isShadow && !shadowVisible) return

        val bitmap = when (curState) {
            State.IDLE -> if (isShadow) shadowIdleBitmap else idleBitmap
            State.RUN -> if (isShadow) shadowRunBitmap else runBitmap
            State.DEATH -> if (isShadow) shadowDeathBitmap else deathBitmap
            State.CLIMB -> if (isShadow) shadowClimbBitmap else climbBitmap
        }

        val frameCount = when (curState) {
            State.DEATH -> 8
            State.IDLE, State.CLIMB -> 4
            else -> 6
        }

        val frameWidth = bitmap.width / frameCount
        val src = Rect(frame * frameWidth, 0, (frame + 1) * frameWidth, bitmap.height)

        val drawX = x + (tileSizeX - characterSize) / 2f
        val floorHeight = tileSizeY * 0.5f
        val floorTop = y + (tileSizeY - floorHeight) + visualFloorOffset
        val dst = RectF(drawX, floorTop - characterSize, drawX + characterSize, floorTop)

        if (isRight || curState == State.CLIMB) {
            canvas.drawBitmap(bitmap, src, dst, paint)
        } else {
            canvas.save()
            canvas.scale(-1f, 1f, drawX + characterSize / 2, floorTop - characterSize / 2)
            canvas.drawBitmap(bitmap, src, dst, paint)
            canvas.restore()
        }
    }

    private fun updateMovement() {

        if (!isMoving) return

        val speed = tileSizeX / 10f

        playerX = moveToward(playerX, targetX, speed)
        playerY = moveToward(playerY, targetY, speed)

        if (playerX == targetX && playerY == targetY) {

            isMoving = false

            if (state != State.DEATH) {
                state = State.IDLE
                currentFrame = 0
            }

            checkTileEffect()
        }

        checkBoundary()
    }

    private fun updateShadowMovement() {

        if (!shadowMoving) return

        val speed = tileSizeX / 10f

        shadowX = moveToward(shadowX, shadowTargetX, speed)
        shadowY = moveToward(shadowY, shadowTargetY, speed)

        if (shadowX == shadowTargetX && shadowY == shadowTargetY) {

            shadowMoving = false

            if (shadowState != State.DEATH) {
                shadowState = State.IDLE
                shadowFrame = 0
            }

            checkTileEffect()
        }

        checkBoundary()
    }

    private fun moveToward(current: Float, target: Float, speed: Float): Float {
        return if (current < target) Math.min(current + speed, target)
        else if (current > target) Math.max(current - speed, target)
        else target
    }

    private fun updateAnimation() {

        val currentTime = System.currentTimeMillis()

        if (currentTime - frameTimer > frameDelay) {

            frameTimer = currentTime

            currentFrame = (currentFrame + 1) % getFrameLimit(state)

            shadowFrame = (shadowFrame + 1) % getFrameLimit(shadowState)

            flagFrame = (flagFrame + 1) % 9

            if (isSwitchPressed) {
                if (buttonFrame < 3) buttonFrame++
            } else {
                if (buttonFrame > 0) buttonFrame--
            }
        }
    }

    private fun getFrameLimit(s: State) = when (s) {
        State.IDLE, State.CLIMB -> 4
        State.RUN -> 6
        State.DEATH -> 8
    }

    private fun drawMoveSlots(canvas: Canvas) {

        val slotSize = 120f

        val totalWidth =
            currentStage.moveLimit * slotSize

        val startXPos =
            (width - totalWidth) / 2f

        val y = 40f

        for (i in 0 until currentStage.moveLimit) {

            val left = startXPos + i * slotSize

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
                paint)

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
                    paint)
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

                shadowTargetX += tileSizeX

                shadowFacingRight = true

                shadowState = State.RUN
            }

            Direction.LEFT -> {

                shadowTargetX -= tileSizeX

                shadowFacingRight = false

                shadowState = State.RUN
            }

            Direction.UP -> {
                val idx = getCurrentIndex(shadowX, shadowY) + 1
                if (idx == 14) {
                    shadowTargetY -= tileSizeY * 2
                    shadowState = State.CLIMB
                }
            }

            Direction.DOWN -> {
                val idx = getCurrentIndex(shadowX, shadowY) + 1
                if (idx == 2) {
                    shadowTargetY += tileSizeY * 2
                    shadowState = State.CLIMB
                }
            }
        }

        shadowMoving = true

        shadowFrame = 0

        replayIndex++
    }

    private fun checkBoundary() {

        if (isDead) return

        if (
            playerX < 0f ||
            playerY < 0f ||
            playerY + tileSizeY > height
        ) {
            startDeath()
        }

        if (
            shadowVisible &&
            (
                    shadowX < 0f ||
                            shadowY < 0f ||
                            shadowY + tileSizeY > height
                    )
        ) {
            startDeath()
        }
    }

    private fun checkCollision() {

        if (!shadowVisible) return

        if (isDead) return

        if (Math.abs(playerX - shadowX) < tileSizeX * 0.4f && Math.abs(playerY - shadowY) < tileSizeY * 0.4f) {
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

        resetPosition()

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

        isSwitchPressed = false

        buttonFrame = 0
    }

    private fun getCurrentIndex(x: Float, y: Float): Int {
        val col = Math.round((x - startX) / tileSizeX)
        val row = Math.round((y - startY) / tileSizeY)

        if (col < 0 || col >= mapCols || row < 0 || row >= mapRows) return -1

        return row * mapCols + col
    }

    private fun checkTileEffect() {
        val pIdx = getCurrentIndex(playerX, playerY) + 1
        val sIdx = if (shadowVisible) getCurrentIndex(shadowX, shadowY) + 1 else -1

        isSwitchPressed = (pIdx == 17 || sIdx == 17)

        val colP = Math.round((playerX - startX) / tileSizeX).toInt()
        val rowP = Math.round((playerY - startY) / tileSizeY).toInt()

        if (playerX < startX - 10f || colP >= mapCols || pIdx == -1) {
            targetY = height.toFloat() + characterSize
            isMoving = true
            state = State.RUN
        }

        if (shadowVisible) {
            val colS = Math.round((shadowX - startX) / tileSizeX).toInt()
            if (shadowX < startX - 10f || colS >= mapCols || sIdx == -1) {
                shadowTargetY = height.toFloat() + characterSize
                shadowMoving = true
                shadowState = State.RUN
            }
        }

        if (pIdx == 5 && !isSwitchPressed) {
            targetY = startY + ((rowP + 1) * tileSizeY)
            isMoving = true
            state = State.RUN
        }

        if (shadowVisible && sIdx == 5 && !isSwitchPressed) {
            val rowS = Math.round((shadowY - startY) / tileSizeY).toInt()
            shadowTargetY = startY + ((rowS + 1) * tileSizeY)
            shadowMoving = true
            shadowState = State.RUN
        }

        if (pIdx == 11 && !isSwitchPressed) {
            targetY = startY + ((rowP + 1) * tileSizeY)
            isMoving = true
            state = State.RUN
        }

        if (shadowVisible && sIdx == 11 && !isSwitchPressed) {
            val rowS = Math.round((shadowY - startY) / tileSizeY).toInt()
            shadowTargetY = startY + ((rowS + 1) * tileSizeY)
            shadowMoving = true
            shadowState = State.RUN
        }

        if (pIdx == 6) {
            resetStage()
        }
    }

    fun moveRight() {

        if (isMoving || isDead) return

        recordMove(Direction.RIGHT)

        facingRight = true

        targetX += tileSizeX

        state = State.RUN

        startMove()
    }

    fun moveLeft() {

        if (isMoving || isDead) return

        recordMove(Direction.LEFT)

        facingRight = false

        targetX -= tileSizeX

        state = State.RUN

        startMove()
    }

    fun moveUp() {

        if (isMoving || isDead) return

        val idx = getCurrentIndex(playerX, playerY) + 1
        if (idx != 14) return

        recordMove(Direction.UP)

        targetY -= tileSizeY * 2

        state = State.CLIMB

        startMove()
    }

    fun moveDown() {

        if (isMoving || isDead) return

        val idx = getCurrentIndex(playerX, playerY) + 1
        if (idx != 2) return

        recordMove(Direction.DOWN)

        targetY += tileSizeY * 2

        state = State.CLIMB

        startMove()
    }

    private fun startMove() {
        moveShadow()
        isMoving = true
        currentFrame = 0
    }
}