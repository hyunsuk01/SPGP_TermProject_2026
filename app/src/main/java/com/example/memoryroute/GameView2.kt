package com.example.memoryroute

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class GameView2 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Stage(val moveLimit: Int)

    private val stages = listOf(Stage(5))
    private var stageIndex = 0
    private var currentStage = stages[stageIndex]

    enum class Direction { UP, DOWN, LEFT, RIGHT }
    private enum class State { IDLE, RUN, DEATH, CLIMB }

    private val background = BitmapFactory.decodeResource(resources, R.drawable.city1)
    private val idleBitmap = BitmapFactory.decodeResource(resources, R.drawable.idle)
    private val runBitmap = BitmapFactory.decodeResource(resources, R.drawable.run)
    private val deathBitmap = BitmapFactory.decodeResource(resources, R.drawable.death)
    private val climbBitmap = BitmapFactory.decodeResource(resources, R.drawable.climb)

    private val shadowIdleBitmap = BitmapFactory.decodeResource(resources, R.drawable.shadow_idle)
    private val shadowRunBitmap = BitmapFactory.decodeResource(resources, R.drawable.shadow_run)
    private val shadowDeathBitmap = BitmapFactory.decodeResource(resources, R.drawable.shadow_death)
    private val shadowClimbBitmap = BitmapFactory.decodeResource(resources, R.drawable.shadow_climb)

    private val emptySlotBitmap = BitmapFactory.decodeResource(resources, R.drawable.empty_slot)
    private val upIconBitmap = BitmapFactory.decodeResource(resources, R.drawable.up)
    private val downIconBitmap = BitmapFactory.decodeResource(resources, R.drawable.down)
    private val leftIconBitmap = BitmapFactory.decodeResource(resources, R.drawable.left)
    private val rightIconBitmap = BitmapFactory.decodeResource(resources, R.drawable.right)

    private val flagBitmap = BitmapFactory.decodeResource(resources, R.drawable.flag)
    private val floorBitmap = BitmapFactory.decodeResource(resources, R.drawable.floor)
    private val ladderBitmap = BitmapFactory.decodeResource(resources, R.drawable.ladder)
    private val buttonBitmap = BitmapFactory.decodeResource(resources, R.drawable.button)
    private val buttonFloorBitmap = BitmapFactory.decodeResource(resources, R.drawable.button_floor)
    private val buttonFloor1Bitmap = BitmapFactory.decodeResource(resources, R.drawable.button_floor1)

    private val paint = Paint().apply { isFilterBitmap = false }

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

    private val mapCols = 5
    private val mapRows = 4
    private var isSwitchPressed = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        tileSizeX = (w * 0.75f) / mapCols
        resetPosition()
    }

    private fun resetPosition() {
        playerX = startX + (2 * tileSizeX)
        playerY = startY + (0 * tileSizeY)
        targetX = playerX
        targetY = playerY

        shadowX = playerX
        shadowY = playerY
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
        val dst = Rect(0, 0, width, scaledHeight)
        canvas.drawBitmap(background, null, dst, paint)
    }

    private fun drawMapAssets(canvas: Canvas) {
        for (i in 0 until 20) {
            val col = i % mapCols
            val row = i / mapCols
            val x = startX + col * tileSizeX
            val y = startY + row * tileSizeY

            val floorHeight = tileSizeY * 0.5f
            val floorTop = y + (tileSizeY - floorHeight) + visualFloorOffset * 0.5f
            val floorDst = RectF(x, floorTop, x + tileSizeX, floorTop + floorHeight)

            val tileNum = i + 1

            when (tileNum) {
                3, 5, 7, 10, 12, 13, 14, 15, 16 -> {
                    canvas.drawBitmap(floorBitmap, null, floorDst, paint)
                }
                4, 11 -> {
                    val bFloor = if (!isSwitchPressed) buttonFloor1Bitmap else buttonFloorBitmap
                    canvas.drawBitmap(bFloor, null, floorDst, paint)
                }
            }

            when (tileNum) {
                16 -> {
                    val fWidth = flagBitmap.width / 9
                    val fSrc = Rect(flagFrame * fWidth, 0, (flagFrame + 1) * fWidth, flagBitmap.height)
                    val flagH = tileSizeY * 0.7f
                    val flagDst = RectF(x, floorTop - flagH, x + tileSizeX, floorTop)
                    canvas.drawBitmap(flagBitmap, fSrc, flagDst, paint)
                }
                12 -> {
                    val bWidth = buttonBitmap.width / 4
                    val bSrc = Rect(buttonFrame * bWidth, 0, (buttonFrame + 1) * bWidth, buttonBitmap.height)
                    val btnW = tileSizeX * 0.5f
                    val btnH = tileSizeY * 0.3f
                    val btnDst = RectF(x + (tileSizeX - btnW) / 2f, floorTop - btnH, x + (tileSizeX + btnW) / 2f, floorTop)
                    canvas.drawBitmap(buttonBitmap, bSrc, btnDst, paint)
                }
            }

            if (tileNum == 7) {
                val ladderW = tileSizeX * 0.4f
                val ladderDst = RectF(
                    x + (tileSizeX - ladderW) / 2f,
                    floorTop,
                    x + (tileSizeX + ladderW) / 2f,
                    floorTop + tileSizeY
                )
                canvas.drawBitmap(ladderBitmap, null, ladderDst, paint)
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
        val characterFloorTop = y + (tileSizeY - floorHeight) + visualFloorOffset * 0.5f
        val dst = RectF(drawX, characterFloorTop - characterSize, drawX + characterSize, characterFloorTop)

        if (isRight || curState == State.CLIMB) {
            canvas.drawBitmap(bitmap, src, dst, paint)
        } else {
            canvas.save()
            canvas.scale(-1f, 1f, drawX + characterSize / 2, characterFloorTop - characterSize / 2)
            canvas.drawBitmap(bitmap, src, dst, paint)
            canvas.restore()
        }
    }

    private fun updateMovement() {
        if (!isMoving) return

        val speed = tileSizeX / 10f
        playerX = moveToward(playerX, targetX, speed)

        if (state == State.IDLE && playerY != targetY) {
            val pIdx = getCurrentIndex(playerX, playerY) + 1
            val permanentTiles = listOf(3, 5, 7, 10, 12, 13, 14, 15, 16)
            val isButtonFloorValid = !isSwitchPressed && (pIdx == 4 || pIdx == 11)

            if (permanentTiles.contains(pIdx) || isButtonFloorValid) {
                targetY = startY + (Math.round((playerY - startY) / tileSizeY) * tileSizeY)
                playerY = targetY
                isMoving = false
                checkTileEffect()
                return
            }
        }

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

        if (shadowState == State.IDLE && shadowY != shadowTargetY) {
            val sIdx = getCurrentIndex(shadowX, shadowY) + 1
            val permanentTiles = listOf(3, 5, 7, 10, 12, 13, 14, 15, 16)
            val isButtonFloorValid = !isSwitchPressed && (sIdx == 4 || sIdx == 11)

            if (permanentTiles.contains(sIdx) || isButtonFloorValid) {
                shadowTargetY = startY + (Math.round((shadowY - startY) / tileSizeY) * tileSizeY)
                shadowY = shadowTargetY
                shadowMoving = false
                checkTileEffect()
                return
            }
        }

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
        val totalWidth = currentStage.moveLimit * slotSize
        val startXPos = (width - totalWidth) / 2f
        val y = 40f

        for (i in 0 until currentStage.moveLimit) {
            val left = startXPos + i * slotSize
            val dst = RectF(left, y, left + 110f, y + 110f)

            canvas.drawBitmap(emptySlotBitmap, null, dst, paint)

            if (i < moveHistory.size) {
                val iconBitmap = when (moveHistory[i]) {
                    Direction.UP -> upIconBitmap
                    Direction.DOWN -> downIconBitmap
                    Direction.LEFT -> leftIconBitmap
                    Direction.RIGHT -> rightIconBitmap
                }
                canvas.drawBitmap(iconBitmap, null, dst, paint)
            }
        }
    }

    private fun recordMove(direction: Direction) {
        if (moveHistory.size >= currentStage.moveLimit) return
        moveHistory.add(direction)

        if (moveHistory.size == currentStage.moveLimit && !shadowVisible) {
            shadowVisible = true
            shadowReady = true

            shadowX = startX + (2 * tileSizeX)
            shadowY = startY + (0 * tileSizeY)
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
        if (replayIndex >= moveHistory.size) return

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
                if (idx == 12) {
                    shadowTargetY -= tileSizeY
                    shadowState = State.CLIMB
                }
            }
            Direction.DOWN -> {
                val idx = getCurrentIndex(shadowX, shadowY) + 1
                if (idx == 7) {
                    shadowTargetY += tileSizeY
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
        if (playerX < 0f || playerY < 0f || playerY + tileSizeY > height) {
            startDeath()
        }
        if (shadowVisible && (shadowX < 0f || shadowY < 0f || shadowY + tileSizeY > height)) {
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

        isSwitchPressed = (pIdx == 12 || sIdx == 12)

        val permanentTiles = listOf(3, 5, 7, 10, 12, 13, 14, 15, 16)

        val isPlayerOnValidTile = permanentTiles.contains(pIdx) || (!isSwitchPressed && (pIdx == 4 || pIdx == 11))

        if (pIdx == -1 || !isPlayerOnValidTile) {

            val currentCol = Math.round((playerX - startX) / tileSizeX).toInt()
            var currentRow = Math.round((playerY - startY) / tileSizeY).toInt()

            var foundLandingRow = -1
            var isFatalFall = true

            for (nextRow in (currentRow + 1) until mapRows) {
                val checkIdx = (nextRow * mapCols + currentCol) + 1

                val isValidLanding = permanentTiles.contains(checkIdx) || (!isSwitchPressed && (checkIdx == 4 || checkIdx == 11))

                if (isValidLanding) {
                    foundLandingRow = nextRow
                    isFatalFall = false
                    break
                }
            }

            if (!isFatalFall && foundLandingRow != -1) {
                playerX = startX + (currentCol * tileSizeX)

                val rowDiff = foundLandingRow - currentRow
                targetY += (tileSizeY * rowDiff)
                isMoving = true
                state = State.IDLE
            } else {
                triggerFall(false, true)
            }
        }

        if (shadowVisible) {
            val isShadowOnValidTile = permanentTiles.contains(sIdx) || (!isSwitchPressed && (sIdx == 4 || sIdx == 11))

            if (sIdx == -1 || !isShadowOnValidTile) {
                val currentColS = Math.round((shadowX - startX) / tileSizeX).toInt()
                var currentRowS = Math.round((shadowY - startY) / tileSizeY).toInt()

                var foundLandingRowS = -1
                var isFatalFallS = true

                for (nextRowS in (currentRowS + 1) until mapRows) {
                    val checkIdxS = (nextRowS * mapCols + currentColS) + 1
                    val isValidLandingS = permanentTiles.contains(checkIdxS) || (!isSwitchPressed && (checkIdxS == 4 || checkIdxS == 11))

                    if (isValidLandingS) {
                        foundLandingRowS = nextRowS
                        isFatalFallS = false
                        break
                    }
                }

                if (!isFatalFallS && foundLandingRowS != -1) {
                    shadowX = startX + (currentColS * tileSizeX)
                    val rowDiffS = foundLandingRowS - currentRowS
                    shadowTargetY += (tileSizeY * rowDiffS)
                    shadowMoving = true
                    shadowState = State.IDLE
                } else {
                    triggerFall(true, true)
                }
            }
        }

        if (pIdx == 16) {
            resetStage()
        }
    }

    private fun triggerFall(isShadowEntity: Boolean, isFatal: Boolean) {
        if (!isShadowEntity) {
            if (isFatal) {
                targetY = height.toFloat() + characterSize
            } else {
                targetY += tileSizeY
            }
            isMoving = true
            state = if (isFatal) State.RUN else State.IDLE
        } else {
            if (isFatal) {
                shadowTargetY = height.toFloat() + characterSize
            } else {
                shadowTargetY += tileSizeY
            }
            shadowMoving = true
            shadowState = if (isFatal) State.RUN else State.IDLE
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
        if (idx != 12) return

        recordMove(Direction.UP)
        targetY -= tileSizeY
        state = State.CLIMB
        startMove()
    }

    fun moveDown() {
        if (isMoving || isDead) return
        val idx = getCurrentIndex(playerX, playerY) + 1
        if (idx != 7) return

        recordMove(Direction.DOWN)
        targetY += tileSizeY
        state = State.CLIMB
        startMove()
    }

    private fun startMove() {
        moveShadow()
        isMoving = true
        currentFrame = 0
    }
}