package com.example.memoryroute

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private enum class Scene { START, ROUND_SELECT, GAME, PAUSE, CLEAR }

    var onSceneChangeListener: ((Boolean) -> Unit)? = null

    private var currentScene = Scene.START
        set(value) {
            field = value
            onSceneChangeListener?.invoke(value == Scene.GAME)
        }

    enum class Direction { UP, DOWN, LEFT, RIGHT }
    private enum class State { IDLE, RUN, DEATH, CLIMB }
    private enum class PortalState { IDLE, TELEPORT, TELEPORT_REVERSE }

    private val startBackground = BitmapFactory.decodeResource(resources, R.drawable.start_background)
    private val startGameBtnBitmap = BitmapFactory.decodeResource(resources, R.drawable.start_game)
    private val quitGameBtnBitmap = BitmapFactory.decodeResource(resources, R.drawable.quit)

    private val roundHeaderBitmap = BitmapFactory.decodeResource(resources, R.drawable.round)
    private val round1BtnBitmap = BitmapFactory.decodeResource(resources, R.drawable.one)
    private val round2BtnBitmap = BitmapFactory.decodeResource(resources, R.drawable.two)
    private val round3BtnBitmap = BitmapFactory.decodeResource(resources, R.drawable.three)
    private val round4BtnBitmap = BitmapFactory.decodeResource(resources, R.drawable.four)

    private val exitBtnBitmap = BitmapFactory.decodeResource(resources, R.drawable.exit_button)
    private val restartBtnBitmap = BitmapFactory.decodeResource(resources, R.drawable.restart_button)
    private val pauseBtnBitmap = BitmapFactory.decodeResource(resources, R.drawable.pause_button)
    private val pauseHeaderBitmap = BitmapFactory.decodeResource(resources, R.drawable.pause)
    private val resumeBtnBitmap = BitmapFactory.decodeResource(resources, R.drawable.resume)
    private val pauseExitBtnBitmap = BitmapFactory.decodeResource(resources, R.drawable.exit)
    private val clearBitmap = BitmapFactory.decodeResource(resources, R.drawable.clear)

    private val startGameBtnRect = RectF()
    private val quitGameBtnRect = RectF()
    private val round1BtnRect = RectF()
    private val round2BtnRect = RectF()
    private val round3BtnRect = RectF()
    private val round4BtnRect = RectF()
    private val roundSelectExitBtnRect = RectF()
    private val inGameRestartBtnRect = RectF()
    private val inGamePauseBtnRect = RectF()
    private val pauseExitBtnRect = RectF()
    private val pauseResumeBtnRect = RectF()
    private var clearStartTime = 0L

    private var stageIndex = 0
    private var mapCols = 6
    private var mapRows = 4
    private var moveLimit = 5

    private val background1 = BitmapFactory.decodeResource(resources, R.drawable.city1)
    private val background2 = BitmapFactory.decodeResource(resources, R.drawable.city2)
    private val background3 = BitmapFactory.decodeResource(resources, R.drawable.city3)
    private val background4 = BitmapFactory.decodeResource(resources, R.drawable.city3)

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
    private val portalButtonBitmap = BitmapFactory.decodeResource(resources, R.drawable.portal_button)
    private val buttonFloorBitmap = BitmapFactory.decodeResource(resources, R.drawable.button_floor)
    private val buttonFloor1Bitmap = BitmapFactory.decodeResource(resources, R.drawable.button_floor1)
    private val greenPortalBitmap = BitmapFactory.decodeResource(resources, R.drawable.green_portal)

    private val paint = Paint().apply { isFilterBitmap = false }
    private val uiPaint = Paint().apply { isAntiAlias = true }

    private val characterSize = 150f
    private var tileSizeX = 0f
    private var tileSizeY = 150f
    private var startX = 300f
    private var startY = 300f
    private var visualFloorOffset = -100f

    private var playerX = 0f; private var playerY = 0f
    private var targetX = 0f; private var targetY = 0f
    private var isMoving = false; private var facingRight = true; private var state = State.IDLE

    private var shadowX = 0f; private var shadowY = 0f
    private var shadowTargetX = 0f; private var shadowTargetY = 0f
    private var shadowMoving = false; private var shadowFacingRight = true
    private var shadowVisible = false; private var shadowReady = false; private var shadowState = State.IDLE

    private var currentFrame = 0; private var shadowFrame = 0; private var flagFrame = 0
    private var buttonFrame = 0; private var portalButtonFrame = 0; private var portalFrame = 0
    private var frameTimer = 0L; private val frameDelay = 120L

    private val moveHistory = mutableListOf<Direction>()
    private var replayIndex = 0
    private var isDead = false
    private var deathStartTime = 0L
    private var isSwitchPressed = false
    private var isPortalButtonPressed = false
    private var currentPortalState = PortalState.IDLE

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        applyStageSpecs()
        onSceneChangeListener?.invoke(false)
    }

    private fun applyStageSpecs() {
        when (stageIndex) {
            0 -> { mapCols = 6; mapRows = 4; moveLimit = 4; visualFloorOffset = -100f }
            1 -> { mapCols = 5; mapRows = 4; moveLimit = 5; visualFloorOffset = -100f }
            2 -> { mapCols = 6; mapRows = 4; moveLimit = 5; visualFloorOffset = -100f }
            3 -> { mapCols = 6; mapRows = 4; moveLimit = 7; visualFloorOffset = -100f }
        }
        tileSizeX = (width * 0.75f) / mapCols
        resetPosition()
    }

    private fun resetPosition() {
        when (stageIndex) {
            0 -> { playerX = startX; playerY = startY }
            1 -> { playerX = startX + (2 * tileSizeX); playerY = startY + (0 * tileSizeY) }
            2 -> { playerX = startX; playerY = startY }
            3 -> {
                playerX = startX + (2 * tileSizeX)
                playerY = startY + (0 * tileSizeY)
            }
        }
        targetX = playerX; targetY = playerY
        shadowX = playerX; shadowY = playerY
        shadowTargetX = shadowX; shadowTargetY = shadowY
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        when (currentScene) {
            Scene.START -> {
                drawFullBackground(canvas, startBackground)
                drawStartAndQuitButtons(canvas)
            }
            Scene.ROUND_SELECT -> {
                drawFullBackground(canvas, startBackground)
                drawRoundSelectOverlay(canvas)
            }
            Scene.GAME -> {
                drawInGameScene(canvas)
            }
            Scene.PAUSE -> {
                drawInGameScene(canvas)
                drawPauseOverlay(canvas)
            }
            Scene.CLEAR -> {
                drawInGameScene(canvas)
                drawClearOverlay(canvas)

                if (System.currentTimeMillis() - clearStartTime > 1000L) {
                    currentScene = Scene.ROUND_SELECT
                }
            }
        }

        if (currentScene == Scene.GAME) {
            updateAnimation()
        }
        invalidate()
    }

    private fun drawFullBackground(canvas: Canvas, bitmap: Bitmap) {
        val dst = Rect(0, 0, width, height)
        canvas.drawBitmap(bitmap, null, dst, paint)
    }

    private fun drawStartAndQuitButtons(canvas: Canvas) {
        val btnW = 400f; val btnH = 150f
        val top = height * 0.75f
        val centerX = width / 2f; val spacing = 40f

        val startLeft = centerX - btnW - spacing
        startGameBtnRect.set(startLeft, top, startLeft + btnW, top + btnH)
        canvas.drawBitmap(startGameBtnBitmap, null, startGameBtnRect, paint)

        val quitLeft = centerX + spacing
        quitGameBtnRect.set(quitLeft, top, quitLeft + btnW, top + btnH)
        canvas.drawBitmap(quitGameBtnBitmap, null, quitGameBtnRect, paint)
    }

    private fun drawRoundSelectOverlay(canvas: Canvas) {
        val marginX = width * 0.05f; val marginY = height * 0.15f
        val rect = RectF(marginX, marginY, width - marginX, height - marginY)

        uiPaint.color = Color.argb(220, 255, 255, 255)
        canvas.drawRoundRect(rect, 30f, 30f, uiPaint)

        val exitSize = 80f
        val exitLeft = (width - marginX) - exitSize - 20f
        val exitTop = marginY + 20f
        roundSelectExitBtnRect.set(exitLeft, exitTop, exitLeft + exitSize, exitTop + exitSize)
        canvas.drawBitmap(exitBtnBitmap, null, roundSelectExitBtnRect, paint)

        val roundW = 450f; val roundH = 120f
        val roundLeft = (width - roundW) / 2f
        val roundTop = marginY + 50f
        val roundRect = RectF(roundLeft, roundTop, roundLeft + roundW, roundTop + roundH)
        canvas.drawBitmap(roundHeaderBitmap, null, roundRect, paint)

        val bSize = 140f
        val rTop = marginY + 280f
        val centerX = width / 2f
        val gap = 30f

        val r1Left = centerX - (bSize * 2f) - (gap * 1.5f)
        round1BtnRect.set(r1Left, rTop, r1Left + bSize, rTop + bSize)
        canvas.drawBitmap(round1BtnBitmap, null, round1BtnRect, paint)

        val r2Left = centerX - bSize - (gap * 0.5f)
        round2BtnRect.set(r2Left, rTop, r2Left + bSize, rTop + bSize)
        canvas.drawBitmap(round2BtnBitmap, null, round2BtnRect, paint)

        val r3Left = centerX + (gap * 0.5f)
        round3BtnRect.set(r3Left, rTop, r3Left + bSize, rTop + bSize)
        canvas.drawBitmap(round3BtnBitmap, null, round3BtnRect, paint)

        val r4Left = centerX + bSize + (gap * 1.5f)
        round4BtnRect.set(r4Left, rTop, r4Left + bSize, rTop + bSize)
        canvas.drawBitmap(round4BtnBitmap, null, round4BtnRect, paint)
    }

    private fun drawInGameScene(canvas: Canvas) {
        drawBackground(canvas)
        drawMapAssets(canvas)

        if (currentScene == Scene.GAME) {
            updateMovement()
            updateShadowMovement()
            checkCollision()
            updateDeath()
        }

        drawCharacter(canvas, playerX, playerY, state, currentFrame, facingRight, false)
        drawCharacter(canvas, shadowX, shadowY, shadowState, shadowFrame, shadowFacingRight, true)
        drawMoveSlots(canvas)
        drawInGameControlButtons(canvas)
    }

    private fun drawInGameControlButtons(canvas: Canvas) {
        val btnSize = 100f
        val topY = 40f
        val rightX = width - btnSize - 40f

        inGamePauseBtnRect.set(rightX, topY, rightX + btnSize, topY + btnSize)
        canvas.drawBitmap(pauseBtnBitmap, null, inGamePauseBtnRect, paint)

        val restartX = rightX - btnSize - 30f
        inGameRestartBtnRect.set(restartX, topY, restartX + btnSize, topY + btnSize)
        canvas.drawBitmap(restartBtnBitmap, null, inGameRestartBtnRect, paint)
    }

    private fun drawPauseOverlay(canvas: Canvas) {
        uiPaint.color = Color.argb(100, 0, 0, 0)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), uiPaint)

        val marginX = width * 0.1f; val marginY = height * 0.2f
        val rect = RectF(marginX, marginY, width - marginX, height - marginY)
        uiPaint.color = Color.argb(235, 255, 255, 255)
        canvas.drawRoundRect(rect, 30f, 30f, uiPaint)

        val pW = 450f
        val scale = pW / pauseHeaderBitmap.width
        val pH = pauseHeaderBitmap.height * scale
        val pLeft = (width - pW) / 2f
        val pTop = marginY + 100f
        val pRect = RectF(pLeft, pTop, pLeft + pW, pTop + pH)
        canvas.drawBitmap(pauseHeaderBitmap, null, pRect, paint)

        val btnW = 320f
        val btnH = 130f
        val exitH = btnH * 0.75f
        val bTop = marginY + 340f
        val centerX = width / 2f

        val exitLeft = centerX - btnW - 40f
        pauseExitBtnRect.set(exitLeft, bTop, exitLeft + btnW, bTop + exitH)
        canvas.drawBitmap(pauseExitBtnBitmap, null, pauseExitBtnRect, paint)

        val resumeLeft = centerX + 40f
        pauseResumeBtnRect.set(resumeLeft, bTop, resumeLeft + btnW, bTop + btnH)
        canvas.drawBitmap(resumeBtnBitmap, null, pauseResumeBtnRect, paint)
    }

    private fun drawClearOverlay(canvas: Canvas) {
        uiPaint.color = Color.argb(200, 255, 255, 255)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), uiPaint)

        val imgWidth = width * 0.6f
        val scale = imgWidth / clearBitmap.width
        val imgHeight = clearBitmap.height * scale
        val dst = RectF((width - imgWidth) / 2f, (height - imgHeight) / 2f,
            (width + imgWidth) / 2f, (height + imgHeight) / 2f)
        canvas.drawBitmap(clearBitmap, null, dst, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val tx = event.x
            val ty = event.y

            when (currentScene) {
                Scene.START -> {
                    if (startGameBtnRect.contains(tx, ty)) {
                        currentScene = Scene.ROUND_SELECT
                        return true
                    }
                    if (quitGameBtnRect.contains(tx, ty)) {
                        (context as? Activity)?.finish()
                        return true
                    }
                }
                Scene.ROUND_SELECT -> {
                    if (roundSelectExitBtnRect.contains(tx, ty)) {
                        currentScene = Scene.START
                        return true
                    }
                    if (round1BtnRect.contains(tx, ty)) {
                        stageIndex = 0; startSelectedStage(); return true
                    }
                    if (round2BtnRect.contains(tx, ty)) {
                        stageIndex = 1; startSelectedStage(); return true
                    }
                    if (round3BtnRect.contains(tx, ty)) {
                        stageIndex = 2; startSelectedStage(); return true
                    }
                    if (round4BtnRect.contains(tx, ty)) {
                        stageIndex = 3; startSelectedStage(); return true
                    }
                }
                Scene.GAME -> {
                    if (inGameRestartBtnRect.contains(tx, ty)) {
                        resetStage()
                        return true
                    }
                    if (inGamePauseBtnRect.contains(tx, ty)) {
                        currentScene = Scene.PAUSE
                        return true
                    }
                }
                Scene.PAUSE -> {
                    if (pauseResumeBtnRect.contains(tx, ty)) {
                        currentScene = Scene.GAME
                        return true
                    }
                    if (pauseExitBtnRect.contains(tx, ty)) {
                        currentScene = Scene.ROUND_SELECT
                        return true
                    }
                }
                Scene.CLEAR -> {
                    currentScene = Scene.ROUND_SELECT
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun startSelectedStage() {
        applyStageSpecs()
        resetStage()
        currentScene = Scene.GAME
    }

    private fun drawBackground(canvas: Canvas) {
        val bgBitmap = when (stageIndex) {
            0 -> background1
            1 -> background2
            2 -> background3
            else -> background4
        }
        val scale = width.toFloat() / bgBitmap.width
        val scaledHeight = (bgBitmap.height * scale).toInt()
        val dst = Rect(0, 0, width, scaledHeight)
        canvas.drawBitmap(bgBitmap, null, dst, paint)
    }

    private fun drawMapAssets(canvas: Canvas) {
        if (stageIndex == 0) {
            for (i in 0 until 36) {
                val col = i % mapCols; val row = i / mapCols
                val x = startX + col * tileSizeX; val y = startY + row * tileSizeY
                val floorHeight = tileSizeY * 0.5f
                val floorTop = y + (tileSizeY - floorHeight) + visualFloorOffset * 2.5f
                val floorDst = RectF(x, floorTop, x + tileSizeX, floorTop + floorHeight)

                when (i + 1) {
                    7, 8, 9, 10, 12, 19, 20, 21, 22, 23, 24, 31, 34 -> canvas.drawBitmap(floorBitmap, null, floorDst, paint)
                    11 -> {
                        val bFloor = if (isSwitchPressed) buttonFloor1Bitmap else buttonFloorBitmap
                        canvas.drawBitmap(bFloor, null, floorDst, paint)
                    }
                    6 -> {
                        val fWidth = flagBitmap.width / 9
                        val fSrc = Rect(flagFrame * fWidth, 0, (flagFrame + 1) * fWidth, flagBitmap.height)
                        val flagH = tileSizeY * 0.6f; val objectBottom = floorTop - visualFloorOffset * 1.5f
                        val flagDst = RectF(x, objectBottom - flagH, x + tileSizeX, objectBottom)
                        canvas.drawBitmap(flagBitmap, fSrc, flagDst, paint)
                    }
                    17 -> {
                        val bWidth = buttonBitmap.width / 4
                        val bSrc = Rect(buttonFrame * bWidth, 0, (buttonFrame + 1) * bWidth, buttonBitmap.height)
                        val btnW = tileSizeX * 0.4f; val btnH = tileSizeY * 0.4f
                        val objectBottom = floorTop - visualFloorOffset * 1.5f
                        val btnDst = RectF(x + (tileSizeX - btnW) / 2f, objectBottom - btnH, x + (tileSizeX + btnW) / 2f, objectBottom)
                        canvas.drawBitmap(buttonBitmap, bSrc, btnDst, paint)
                    }
                    14 -> {
                        val ladderW = tileSizeX * 0.4f; val objectBottom = floorTop - visualFloorOffset * 1.5f
                        val upperFloorTop = floorTop - tileSizeY
                        val ladderDst = RectF(x + (tileSizeX - ladderW) / 2f, upperFloorTop, x + (tileSizeX + ladderW) / 2f, objectBottom)
                        canvas.drawBitmap(ladderBitmap, null, ladderDst, paint)
                    }
                }
            }
        } else if (stageIndex == 1) {
            for (i in 0 until 20) {
                val col = i % mapCols; val row = i / mapCols
                val x = startX + col * tileSizeX; val y = startY + row * tileSizeY
                val floorHeight = tileSizeY * 0.5f
                val floorTop = y + (tileSizeY - floorHeight) + visualFloorOffset * 0.5f
                val floorDst = RectF(x, floorTop, x + tileSizeX, floorTop + floorHeight)

                val tileNum = i + 1
                when (tileNum) {
                    3, 5, 7, 10, 12, 13, 14, 15, 16 -> canvas.drawBitmap(floorBitmap, null, floorDst, paint)
                    4, 11 -> {
                        val bFloor = if (isSwitchPressed) buttonFloorBitmap else buttonFloor1Bitmap
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
                        val btnW = tileSizeX * 0.5f; val btnH = tileSizeY * 0.3f
                        val btnDst = RectF(x + (tileSizeX - btnW) / 2f, floorTop - btnH, x + (tileSizeX + btnW) / 2f, floorTop)
                        canvas.drawBitmap(buttonBitmap, bSrc, btnDst, paint)
                    }
                }
                if (tileNum == 7) {
                    val ladderW = tileSizeX * 0.4f
                    val ladderDst = RectF(x + (tileSizeX - ladderW) / 2f, floorTop, x + (tileSizeX + ladderW) / 2f, floorTop + tileSizeY)
                    canvas.drawBitmap(ladderBitmap, null, ladderDst, paint)
                }
            }
        } else if (stageIndex == 2) {
            for (i in 0 until 24) {
                val col = i % mapCols; val row = i / mapCols
                val x = startX + col * tileSizeX; val y = startY + row * tileSizeY
                val floorHeight = tileSizeY * 0.5f
                val floorTop = y + (tileSizeY - floorHeight) + visualFloorOffset * 2.5f
                val floorDst = RectF(x, floorTop, x + tileSizeX, floorTop + floorHeight)

                val tileNum = i + 1
                when (tileNum) {
                    7, 8, 9, 10, 12, 20, 21, 22, 23, 24 -> canvas.drawBitmap(floorBitmap, null, floorDst, paint)
                    11 -> {
                        val bFloor = if (isSwitchPressed) buttonFloor1Bitmap else buttonFloorBitmap
                        canvas.drawBitmap(bFloor, null, floorDst, paint)
                    }
                }
                when (tileNum) {
                    3, 18 -> {
                        val pSingleW = greenPortalBitmap.width / 8
                        val pSingleH = greenPortalBitmap.height / 3
                        val srcX = (portalFrame % 8) * pSingleW
                        val srcY = (portalFrame / 8) * pSingleH
                        val pSrc = Rect(srcX, srcY, srcX + pSingleW, srcY + pSingleH)
                        val pW = tileSizeX * 0.8f; val pH = tileSizeY * 1.2f
                        val objectBottom = floorTop - visualFloorOffset * 1.5f
                        val pDst = RectF(x + (tileSizeX - pW) / 2f, objectBottom - pH, x + (tileSizeX + pW) / 2f, objectBottom)
                        canvas.drawBitmap(greenPortalBitmap, pSrc, pDst, paint)
                    }
                    16 -> {
                        val bWidth = portalButtonBitmap.width / 4
                        val bSrc = Rect(portalButtonFrame * bWidth, 0, (portalButtonFrame + 1) * bWidth, portalButtonBitmap.height)
                        val btnW = tileSizeX * 0.4f; val btnH = tileSizeY * 0.4f
                        val objectBottom = floorTop - visualFloorOffset * 1.5f
                        val btnDst = RectF(x + (tileSizeX - btnW) / 2f, objectBottom - btnH, x + (tileSizeX + btnW) / 2f, objectBottom)
                        canvas.drawBitmap(portalButtonBitmap, bSrc, btnDst, paint)
                    }
                    17 -> {
                        val bWidth = buttonBitmap.width / 4
                        val bSrc = Rect(buttonFrame * bWidth, 0, (buttonFrame + 1) * bWidth, buttonBitmap.height)
                        val btnW = tileSizeX * 0.4f; val btnH = tileSizeY * 0.4f
                        val objectBottom = floorTop - visualFloorOffset * 1.5f
                        val btnDst = RectF(x + (tileSizeX - btnW) / 2f, objectBottom - btnH, x + (tileSizeX + btnW) / 2f, objectBottom)
                        canvas.drawBitmap(buttonBitmap, bSrc, btnDst, paint)
                    }
                    14 -> {
                        val ladderW = tileSizeX * 0.4f; val objectBottom = floorTop - visualFloorOffset * 1.5f
                        val upperFloorTop = floorTop - tileSizeY
                        val ladderDst = RectF(x + (tileSizeX - ladderW) / 2f, upperFloorTop, x + (tileSizeX + ladderW) / 2f, objectBottom)
                        canvas.drawBitmap(ladderBitmap, null, ladderDst, paint)
                    }
                    6 -> {
                        val fWidth = flagBitmap.width / 9
                        val fSrc = Rect(flagFrame * fWidth, 0, (flagFrame + 1) * fWidth, flagBitmap.height)
                        val flagH = tileSizeY * 0.6f; val objectBottom = floorTop - visualFloorOffset * 1.5f
                        val flagDst = RectF(x, objectBottom - flagH, x + tileSizeX, objectBottom)
                        canvas.drawBitmap(flagBitmap, fSrc, flagDst, paint)
                    }
                }
            }
        } else if (stageIndex == 3) {
            for (i in 0 until 24) {
                val col = i % mapCols; val row = i / mapCols
                val x = startX + col * tileSizeX; val y = startY + row * tileSizeY
                val floorHeight = tileSizeY * 0.5f
                val floorTop = y + (tileSizeY - floorHeight) + visualFloorOffset * 2.5f
                val floorDst = RectF(x, floorTop, x + tileSizeX, floorTop + floorHeight)

                val tileNum = i + 1

                when (tileNum) {
                    1, 2, 3, 4, 7, 8, 11, 13, 14, 16, 19, 20, 21, 22, 24 -> {
                        canvas.drawBitmap(floorBitmap, null, floorDst, paint)
                    }
                    15, 18 -> {
                        val bFloor = if (isSwitchPressed) buttonFloorBitmap else buttonFloor1Bitmap
                        canvas.drawBitmap(bFloor, null, floorDst, paint)
                    }
                }

                val objectFloorTop = floorTop - tileSizeY

                when (tileNum) {
                    1, 21 -> {
                        val bWidth = buttonBitmap.width / 4
                        val bSrc = Rect(buttonFrame * bWidth, 0, (buttonFrame + 1) * bWidth, buttonBitmap.height)
                        val btnW = tileSizeX * 0.4f; val btnH = tileSizeY * 0.4f
                        val btnDst = RectF(x + (tileSizeX - btnW) / 2f, floorTop - btnH, x + (tileSizeX + btnW) / 2f, floorTop)
                        canvas.drawBitmap(buttonBitmap, bSrc, btnDst, paint)
                    }
                    2, 16 -> {
                        val pSingleW = greenPortalBitmap.width / 8
                        val pSingleH = greenPortalBitmap.height / 3
                        val srcX = (portalFrame % 8) * pSingleW
                        val srcY = (portalFrame / 8) * pSingleH
                        val pSrc = Rect(srcX, srcY, srcX + pSingleW, srcY + pSingleH)
                        val pW = tileSizeX * 0.8f; val pH = tileSizeY * 1.2f
                        val pDst = RectF(x + (tileSizeX - pW) / 2f, floorTop - pH, x + (tileSizeX + pW) / 2f, floorTop)
                        canvas.drawBitmap(greenPortalBitmap, pSrc, pDst, paint)
                    }
                    8 -> {
                        val bWidth = portalButtonBitmap.width / 4
                        val bSrc = Rect(portalButtonFrame * bWidth, 0, (portalButtonFrame + 1) * bWidth, portalButtonBitmap.height)
                        val btnW = tileSizeX * 0.4f; val btnH = tileSizeY * 0.4f
                        val btnDst = RectF(x + (tileSizeX - btnW) / 2f, floorTop - btnH, x + (tileSizeX + btnW) / 2f, floorTop)
                        canvas.drawBitmap(portalButtonBitmap, bSrc, btnDst, paint)
                    }
                    24 -> {
                        val fWidth = flagBitmap.width / 9
                        val fSrc = Rect(flagFrame * fWidth, 0, (flagFrame + 1) * fWidth, flagBitmap.height)
                        val flagH = tileSizeY * 0.6f
                        val flagDst = RectF(x, floorTop - flagH, x + tileSizeX, floorTop)
                        canvas.drawBitmap(flagBitmap, fSrc, flagDst, paint)
                    }
                    7 -> {
                        val ladderW = tileSizeX * 0.4f
                        val ladderDst = RectF(x + (tileSizeX - ladderW) / 2f, floorTop, x + (tileSizeX + ladderW) / 2f, floorTop + tileSizeY)
                        canvas.drawBitmap(ladderBitmap, null, ladderDst, paint)
                    }
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

        val characterFloorTop = when (stageIndex) {
            0 -> y + (tileSizeY - floorHeight) + visualFloorOffset
            1 -> y + (tileSizeY - floorHeight) + visualFloorOffset * 0.5f
            2 -> y + (tileSizeY - floorHeight) + visualFloorOffset
            3 -> y + (tileSizeY - floorHeight) + visualFloorOffset * 2.5f
            else -> y + (tileSizeY - floorHeight) + visualFloorOffset
        }

        val dst = RectF(drawX, characterFloorTop - characterSize, drawX + characterSize, characterFloorTop)

        if (isRight || curState == State.CLIMB) {
            canvas.drawBitmap(bitmap, src, dst, paint)
        } else {
            canvas.save(); canvas.scale(-1f, 1f, drawX + characterSize / 2, characterFloorTop - characterSize / 2)
            canvas.drawBitmap(bitmap, src, dst, paint)
            canvas.restore()
        }
    }

    private fun updateMovement() {
        if (!isMoving) return
        val speed = tileSizeX / 10f
        playerX = moveToward(playerX, targetX, speed)

        if ((stageIndex == 1 || stageIndex == 3) && state == State.IDLE && playerY != targetY) {
            val pIdx = getCurrentIndex(playerX, playerY) + 1
            if (stageIndex == 1) {
                val permanentTiles = listOf(3, 5, 7, 10, 12, 13, 14, 15, 16)
                val isButtonFloorValid = (pIdx == 4 || pIdx == 11) && !isSwitchPressed
                if (permanentTiles.contains(pIdx) || isButtonFloorValid) {
                    targetY = startY + (Math.round((playerY - startY) / tileSizeY) * tileSizeY)
                    playerY = targetY; isMoving = false; checkTileEffect(); return
                }
            } else if (stageIndex == 3) {
                val permanentTiles = listOf(1, 2, 3, 4, 7, 8, 11, 13, 14, 16, 19, 20, 21, 22, 24)
                val isButtonFloorValid = (pIdx == 15 || pIdx == 18) && !isSwitchPressed
                if (permanentTiles.contains(pIdx) || isButtonFloorValid) {
                    targetY = startY + (Math.round((playerY - startY) / tileSizeY) * tileSizeY)
                    playerY = targetY; isMoving = false; checkTileEffect(); return
                }
            }
        }
        playerY = moveToward(playerY, targetY, speed)

        if (playerX == targetX && playerY == targetY) {
            isMoving = false
            if (state != State.DEATH) { state = State.IDLE; currentFrame = 0 }
            checkTileEffect()
        }
        checkBoundary()
    }

    private fun updateShadowMovement() {
        if (!shadowMoving) return
        val speed = tileSizeX / 10f
        shadowX = moveToward(shadowX, shadowTargetX, speed)

        if ((stageIndex == 1 || stageIndex == 3) && shadowState == State.IDLE && shadowY != shadowTargetY) {
            val sIdx = getCurrentIndex(shadowX, shadowY) + 1
            if (stageIndex == 1) {
                val permanentTiles = listOf(3, 5, 7, 10, 12, 13, 14, 15, 16)
                val isButtonFloorValid = (sIdx == 4 || sIdx == 11) && !isSwitchPressed
                if (permanentTiles.contains(sIdx) || isButtonFloorValid) {
                    shadowTargetY = startY + (Math.round((shadowY - startY) / tileSizeY) * tileSizeY)
                    shadowY = shadowTargetY; shadowMoving = false; checkTileEffect(); return
                }
            } else if (stageIndex == 3) {
                val permanentTiles = listOf(1, 2, 3, 4, 7, 8, 11, 13, 14, 16, 19, 20, 21, 22, 24)
                val isButtonFloorValid = (sIdx == 15 || sIdx == 18) && !isSwitchPressed
                if (permanentTiles.contains(sIdx) || isButtonFloorValid) {
                    shadowTargetY = startY + (Math.round((shadowY - startY) / tileSizeY) * tileSizeY)
                    shadowY = shadowTargetY; shadowMoving = false; checkTileEffect(); return
                }
            }
        }
        shadowY = moveToward(shadowY, shadowTargetY, speed)

        if (shadowX == shadowTargetX && shadowY == shadowTargetY) {
            shadowMoving = false
            if (shadowState != State.DEATH) { shadowState = State.IDLE; shadowFrame = 0 }
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

            if (isSwitchPressed) { if (buttonFrame < 3) buttonFrame++ } else { if (buttonFrame > 0) buttonFrame-- }
            if (isPortalButtonPressed) { if (portalButtonFrame < 3) portalButtonFrame++ } else { if (portalButtonFrame > 0) portalButtonFrame-- }

            when (currentPortalState) {
                PortalState.IDLE -> { portalFrame = (portalFrame + 1) % 8 }
                PortalState.TELEPORT -> {
                    if (portalFrame < 16 || portalFrame > 19) portalFrame = 16
                    else if (portalFrame < 19) portalFrame++
                }
                PortalState.TELEPORT_REVERSE -> {
                    if (portalFrame < 16 || portalFrame > 19) portalFrame = 19
                    if (portalFrame > 16) portalFrame-- else currentPortalState = PortalState.IDLE
                }
            }
        }
    }

    private fun getFrameLimit(s: State) = when (s) {
        State.IDLE, State.CLIMB -> 4
        State.RUN -> 6
        State.DEATH -> 8
    }

    private fun drawMoveSlots(canvas: Canvas) {
        val slotSize = 120f; val totalWidth = moveLimit * slotSize
        val startXPos = (width - totalWidth) / 2f; val y = 40f

        for (i in 0 until moveLimit) {
            val left = startXPos + i * slotSize
            val dst = RectF(left, y, left + 110f, y + 110f)
            canvas.drawBitmap(emptySlotBitmap, null, dst, paint)

            if (i < moveHistory.size) {
                val iconBitmap = when (moveHistory[i]) {
                    Direction.UP -> upIconBitmap; Direction.DOWN -> downIconBitmap
                    Direction.LEFT -> leftIconBitmap; Direction.RIGHT -> rightIconBitmap
                }
                canvas.drawBitmap(iconBitmap, null, dst, paint)
            }
        }
    }

    private fun recordMove(direction: Direction) {
        if (moveHistory.size >= moveLimit) return
        moveHistory.add(direction)

        if (moveHistory.size == moveLimit && !shadowVisible) {
            shadowVisible = true; shadowReady = true
            when (stageIndex) {
                0 -> { shadowX = startX; shadowY = startY }
                1 -> { shadowX = startX + (2 * tileSizeX); shadowY = startY + (0 * tileSizeY) }
                2 -> { shadowX = startX; shadowY = startY }
                3 -> { shadowX = startX + (2 * tileSizeX); shadowY = startY + (0 * tileSizeY) }
            }
            shadowTargetX = shadowX; shadowTargetY = shadowY
        }
    }

    fun moveShadow() {
        if (!shadowVisible || replayIndex >= moveHistory.size) return
        if (shadowReady) { shadowReady = false; return }

        when (moveHistory[replayIndex]) {
            Direction.RIGHT -> { shadowTargetX += tileSizeX; shadowFacingRight = true; shadowState = State.RUN }
            Direction.LEFT -> { shadowTargetX -= tileSizeX; shadowFacingRight = false; shadowState = State.RUN }
            Direction.UP -> {
                val idx = getCurrentIndex(shadowX, shadowY) + 1
                if (stageIndex == 0 && idx == 14) { shadowTargetY -= tileSizeY * 2; shadowState = State.CLIMB }
                else if (stageIndex == 1 && idx == 12) { shadowTargetY -= tileSizeY; shadowState = State.CLIMB }
                else if (stageIndex == 2 && idx == 14) { shadowTargetY -= tileSizeY * 2; shadowState = State.CLIMB }
                else if (stageIndex == 3 && idx == 13) { shadowTargetY -= tileSizeY; shadowState = State.CLIMB }
            }
            Direction.DOWN -> {
                val idx = getCurrentIndex(shadowX, shadowY) + 1
                if (stageIndex == 0 && idx == 2) { shadowTargetY += tileSizeY * 2; shadowState = State.CLIMB }
                else if (stageIndex == 1 && idx == 7) { shadowTargetY += tileSizeY; shadowState = State.CLIMB }
                else if (stageIndex == 2 && idx == 2) { shadowTargetY += tileSizeY * 2; shadowState = State.CLIMB }
                else if (stageIndex == 3 && idx == 7) { shadowTargetY += tileSizeY; shadowState = State.CLIMB }
            }
        }
        shadowMoving = true; shadowFrame = 0; replayIndex++
    }

    private fun checkBoundary() {
        if (isDead) return
        if (playerX < 0f || playerY < 0f || playerY + tileSizeY > height) startDeath()
        if (shadowVisible && (shadowX < 0f || shadowY < 0f || shadowY + tileSizeY > height)) startDeath()
    }

    private fun checkCollision() {
        if (!shadowVisible || isDead) return
        if (Math.abs(playerX - shadowX) < tileSizeX * 0.4f && Math.abs(playerY - shadowY) < tileSizeY * 0.4f) startDeath()
    }

    private fun startDeath() {
        isDead = true; deathStartTime = System.currentTimeMillis()
        state = State.DEATH; shadowState = State.DEATH
        currentFrame = 0; shadowFrame = 0; isMoving = false; shadowMoving = false
    }

    private fun updateDeath() {
        if (!isDead) return
        if (System.currentTimeMillis() - deathStartTime > 1000L) resetStage()
    }

    private fun resetStage() {
        resetPosition()
        moveHistory.clear(); replayIndex = 0
        shadowVisible = false; shadowReady = false; isMoving = false; shadowMoving = false
        state = State.IDLE; shadowState = State.IDLE
        currentFrame = 0; shadowFrame = 0; isDead = false
        isSwitchPressed = false; buttonFrame = 0
        isPortalButtonPressed = false; portalButtonFrame = 0
        currentPortalState = PortalState.IDLE; portalFrame = 0
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

        if (stageIndex == 0) {
            isSwitchPressed = (pIdx == 17 || sIdx == 17)
            val colP = Math.round((playerX - startX) / tileSizeX).toInt()
            val rowP = Math.round((playerY - startY) / tileSizeY).toInt()

            if (playerX < startX - 10f || colP >= mapCols || pIdx == -1) {
                targetY = height.toFloat() + characterSize; isMoving = true; state = State.RUN
            }
            if (shadowVisible) {
                val colS = Math.round((shadowX - startX) / tileSizeX).toInt()
                if (shadowX < startX - 10f || colS >= mapCols || sIdx == -1) {
                    shadowTargetY = height.toFloat() + characterSize; shadowMoving = true; shadowState = State.RUN
                }
            }
            if ((pIdx == 5 || pIdx == 11) && !isSwitchPressed) {
                targetY = startY + ((rowP + 1) * tileSizeY); isMoving = true; state = State.RUN
            }
            if (shadowVisible && (sIdx == 5 || sIdx == 11) && !isSwitchPressed) {
                val rowS = Math.round((shadowY - startY) / tileSizeY).toInt()
                shadowTargetY = startY + ((rowS + 1) * tileSizeY); shadowMoving = true; shadowState = State.RUN
            }
            if (pIdx == 6) {
                currentScene = Scene.CLEAR
                clearStartTime = System.currentTimeMillis()
            }
            return
        }

        if (stageIndex == 1) {
            isSwitchPressed = (pIdx == 12 || sIdx == 12)
            val permanentTiles = listOf(3, 5, 7, 10, 12, 13, 14, 15, 16)
            val isPlayerOnValidTile = permanentTiles.contains(pIdx) || ((pIdx == 4 || pIdx == 11) && !isSwitchPressed)

            if (pIdx == -1 || !isPlayerOnValidTile) {
                val currentCol = Math.round((playerX - startX) / tileSizeX).toInt()
                val currentRow = Math.round((playerY - startY) / tileSizeY).toInt()
                var foundLandingRow = -1; var isFatalFall = true

                for (nextRow in (currentRow + 1) until mapRows) {
                    val checkIdx = (nextRow * mapCols + currentCol) + 1
                    val isValidLanding = permanentTiles.contains(checkIdx) || ((checkIdx == 4 || checkIdx == 11) && !isSwitchPressed)
                    if (isValidLanding) { foundLandingRow = nextRow; isFatalFall = false; break }
                }
                if (!isFatalFall && foundLandingRow != -1) {
                    playerX = startX + (currentCol * tileSizeX); targetY = startY + (foundLandingRow * tileSizeY)
                    isMoving = true; state = State.IDLE
                } else {
                    targetY = height.toFloat() + characterSize; isMoving = true; state = State.RUN
                }
            }

            if (shadowVisible) {
                val isShadowOnValidTile = permanentTiles.contains(sIdx) || ((sIdx == 4 || sIdx == 11) && !isSwitchPressed)
                if (sIdx == -1 || !isShadowOnValidTile) {
                    val currentColS = Math.round((shadowX - startX) / tileSizeX).toInt()
                    val currentRowS = Math.round((shadowY - startY) / tileSizeY).toInt()
                    var foundLandingRowS = -1; var isFatalFallS = true

                    for (nextRowS in (currentRowS + 1) until mapRows) {
                        val checkIdxS = (nextRowS * mapCols + currentColS) + 1
                        val isValidLandingS = permanentTiles.contains(checkIdxS) || ((checkIdxS == 4 || checkIdxS == 11) && !isSwitchPressed)
                        if (isValidLandingS) { foundLandingRowS = nextRowS; isFatalFallS = false; break }
                    }
                    if (!isFatalFallS && foundLandingRowS != -1) {
                        shadowX = startX + (currentColS * tileSizeX); shadowTargetY = startY + (foundLandingRowS * tileSizeY)
                        shadowMoving = true; shadowState = State.IDLE
                    } else {
                        shadowTargetY = height.toFloat() + characterSize; shadowMoving = true; shadowState = State.RUN
                    }
                }
            }
            if (pIdx == 16) {
                currentScene = Scene.CLEAR
                clearStartTime = System.currentTimeMillis()
            }
            return
        }

        if (stageIndex == 2) {
            isSwitchPressed = (pIdx == 17 || sIdx == 17)

            val wasPressed = isPortalButtonPressed
            isPortalButtonPressed = (pIdx == 16 || sIdx == 16)
            if (isPortalButtonPressed && !wasPressed && currentPortalState == PortalState.TELEPORT) {
                currentPortalState = PortalState.TELEPORT_REVERSE
            }

            val pos3X = startX + (2 * tileSizeX);  val pos3Y = startY + (0 * tileSizeY)
            val pos18X = startX + (5 * tileSizeX); val pos18Y = startY + (2 * tileSizeY)

            if (currentPortalState == PortalState.IDLE) {
                if (pIdx == 3) {
                    playerX = pos18X; playerY = pos18Y
                    targetX = pos18X; targetY = pos18Y
                    currentPortalState = PortalState.TELEPORT
                } else if (pIdx == 18) {
                    playerX = pos3X; playerY = pos3Y
                    targetX = pos3X; targetY = pos3Y
                    currentPortalState = PortalState.TELEPORT
                }
            }

            if (shadowVisible && currentPortalState == PortalState.IDLE) {
                if (sIdx == 3) {
                    shadowX = pos18X; shadowY = pos18Y
                    shadowTargetX = pos18X; shadowTargetY = pos18Y
                    currentPortalState = PortalState.TELEPORT
                } else if (sIdx == 18) {
                    shadowX = pos3X; shadowY = pos3Y
                    shadowTargetX = pos3X; shadowTargetY = pos3Y
                    currentPortalState = PortalState.TELEPORT
                }
            }

            val colP = Math.round((playerX - startX) / tileSizeX).toInt()
            val rowP = Math.round((playerY - startY) / tileSizeY).toInt()

            if (playerX < startX - 10f || colP >= mapCols || pIdx == -1) {
                targetY = height.toFloat() + characterSize; isMoving = true; state = State.RUN
            }
            if (shadowVisible) {
                val colS = Math.round((shadowX - startX) / tileSizeX).toInt()
                if (shadowX < startX - 10f || colS >= mapCols || sIdx == -1) {
                    shadowTargetY = height.toFloat() + characterSize; shadowMoving = true; shadowState = State.RUN
                }
            }
            if ((pIdx == 5 || pIdx == 11) && !isSwitchPressed) {
                targetY = startY + ((rowP + 1) * tileSizeY); isMoving = true; state = State.RUN
            }
            if (shadowVisible && (sIdx == 5 || sIdx == 11) && !isSwitchPressed) {
                val rowS = Math.round((shadowY - startY) / tileSizeY).toInt()
                shadowTargetY = startY + ((rowS + 1) * tileSizeY); shadowMoving = true; shadowState = State.RUN
            }
            if (pIdx == 6) {
                currentScene = Scene.CLEAR
                clearStartTime = System.currentTimeMillis()
            }
            return
        }

        if (stageIndex == 3) {
            isSwitchPressed = (pIdx == 1 || pIdx == 21 || sIdx == 1 || sIdx == 21)

            val buttonActive = (pIdx == 8 || sIdx == 8)
            isPortalButtonPressed = buttonActive

            if (buttonActive) {
                if (currentPortalState != PortalState.IDLE) {
                    currentPortalState = PortalState.IDLE
                    portalFrame = 0
                }
            }

            val pos2X = startX + (1 * tileSizeX);  val pos2Y = startY + (0 * tileSizeY)
            val pos16X = startX + (3 * tileSizeX); val pos16Y = startY + (2 * tileSizeY)

            if (currentPortalState == PortalState.IDLE) {
                if (pIdx == 2) {
                    playerX = pos16X; playerY = pos16Y
                    targetX = pos16X; targetY = pos16Y
                    currentPortalState = PortalState.TELEPORT
                } else if (pIdx == 16) {
                    playerX = pos2X; playerY = pos2Y
                    targetX = pos2X; targetY = pos2Y
                    currentPortalState = PortalState.TELEPORT
                }

                if (shadowVisible) {
                    if (sIdx == 2) {
                        shadowX = pos16X; shadowY = pos16Y
                        shadowTargetX = pos16X; shadowTargetY = pos16Y
                        currentPortalState = PortalState.TELEPORT
                    } else if (sIdx == 16) {
                        shadowX = pos2X; shadowY = pos2Y
                        shadowTargetX = pos2X; shadowTargetY = pos2Y
                        currentPortalState = PortalState.TELEPORT
                    }
                }
            }

            val permanentTiles = listOf(1, 2, 3, 4, 7, 8, 11, 13, 14, 16, 19, 20, 21, 22, 24)
            val isPlayerOnValidTile = permanentTiles.contains(pIdx) || ((pIdx == 15 || pIdx == 18) && !isSwitchPressed)

            if (pIdx == -1 || !isPlayerOnValidTile) {
                val currentCol = Math.round((playerX - startX) / tileSizeX).toInt()
                val currentRow = Math.round((playerY - startY) / tileSizeY).toInt()
                var foundLandingRow = -1; var isFatalFall = true

                for (nextRow in (currentRow + 1) until mapRows) {
                    val checkIdx = (nextRow * mapCols + currentCol) + 1
                    val isValidLanding = permanentTiles.contains(checkIdx) || ((checkIdx == 15 || checkIdx == 18) && !isSwitchPressed)
                    if (isValidLanding) { foundLandingRow = nextRow; isFatalFall = false; break }
                }
                if (!isFatalFall && foundLandingRow != -1) {
                    playerX = startX + (currentCol * tileSizeX); targetY = startY + (foundLandingRow * tileSizeY)
                    isMoving = true; state = State.IDLE
                } else {
                    targetY = height.toFloat() + characterSize; isMoving = true; state = State.RUN
                }
            }

            if (shadowVisible) {
                val isShadowOnValidTile = permanentTiles.contains(sIdx) || ((sIdx == 15 || sIdx == 18) && !isSwitchPressed)
                if (sIdx == -1 || !isShadowOnValidTile) {
                    val currentColS = Math.round((shadowX - startX) / tileSizeX).toInt()
                    val currentRowS = Math.round((shadowY - startY) / tileSizeY).toInt()
                    var foundLandingRowS = -1; var isFatalFallS = true

                    for (nextRowS in (currentRowS + 1) until mapRows) {
                        val checkIdxS = (nextRowS * mapCols + currentColS) + 1
                        val isValidLandingS = permanentTiles.contains(checkIdxS) || ((checkIdxS == 15 || checkIdxS == 18) && !isSwitchPressed)
                        if (isValidLandingS) { foundLandingRowS = nextRowS; isFatalFallS = false; break }
                    }
                    if (!isFatalFallS && foundLandingRowS != -1) {
                        shadowX = startX + (currentColS * tileSizeX); shadowTargetY = startY + (foundLandingRowS * tileSizeY)
                        shadowMoving = true; shadowState = State.IDLE
                    } else {
                        shadowTargetY = height.toFloat() + characterSize; shadowMoving = true; shadowState = State.RUN
                    }
                }
            }

            if (pIdx == 24) {
                currentScene = Scene.CLEAR
                clearStartTime = System.currentTimeMillis()
            }
        }
    }

    fun moveRight() {
        if (isMoving || isDead || currentScene != Scene.GAME) return
        recordMove(Direction.RIGHT); facingRight = true; targetX += tileSizeX; state = State.RUN; startMove()
    }

    fun moveLeft() {
        if (isMoving || isDead || currentScene != Scene.GAME) return
        recordMove(Direction.LEFT); facingRight = false; targetX -= tileSizeX; state = State.RUN; startMove()
    }

    fun moveUp() {
        if (isMoving || isDead || currentScene != Scene.GAME) return
        val idx = getCurrentIndex(playerX, playerY) + 1
        if (stageIndex == 0 && idx == 14) {
            recordMove(Direction.UP); targetY -= tileSizeY * 2; state = State.CLIMB; startMove()
        } else if (stageIndex == 1 && idx == 12) {
            recordMove(Direction.UP); targetY -= tileSizeY; state = State.CLIMB; startMove()
        } else if (stageIndex == 2 && idx == 14) {
            recordMove(Direction.UP); targetY -= tileSizeY * 2; state = State.CLIMB; startMove()
        } else if (stageIndex == 3 && idx == 13) {
            recordMove(Direction.UP); targetY -= tileSizeY; state = State.CLIMB; startMove()
        }
    }

    fun moveDown() {
        if (isMoving || isDead || currentScene != Scene.GAME) return
        val idx = getCurrentIndex(playerX, playerY) + 1
        if (stageIndex == 0 && idx == 2) {
            recordMove(Direction.DOWN); targetY += tileSizeY * 2; state = State.CLIMB; startMove()
        } else if (stageIndex == 1 && idx == 7) {
            recordMove(Direction.DOWN); targetY += tileSizeY; state = State.CLIMB; startMove()
        } else if (stageIndex == 2 && idx == 2) {
            recordMove(Direction.DOWN); targetY += tileSizeY * 2; state = State.CLIMB; startMove()
        } else if (stageIndex == 3 && idx == 7) {
            recordMove(Direction.DOWN); targetY += tileSizeY; state = State.CLIMB; startMove()
        }
    }

    private fun startMove() { moveShadow(); isMoving = true; currentFrame = 0 }
}