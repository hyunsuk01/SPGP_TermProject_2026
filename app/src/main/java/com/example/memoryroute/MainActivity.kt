package com.example.memoryroute

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val gameView = findViewById<GameView>(R.id.gameView)

        val btnUp = findViewById<ImageButton>(R.id.btnUp)
        val btnDown = findViewById<ImageButton>(R.id.btnDown)
        val btnLeft = findViewById<ImageButton>(R.id.btnLeft)
        val btnRight = findViewById<ImageButton>(R.id.btnRight)

        gameView.onSceneChangeListener = { isInGame ->
            val visibilityState = if (isInGame) View.VISIBLE else View.GONE

            btnUp.visibility = visibilityState
            btnDown.visibility = visibilityState
            btnLeft.visibility = visibilityState
            btnRight.visibility = visibilityState
        }

        btnUp.setOnClickListener {
            gameView.moveUp()
        }

        btnDown.setOnClickListener {
            gameView.moveDown()
        }

        btnLeft.setOnClickListener {
            gameView.moveLeft()
        }

        btnRight.setOnClickListener {
            gameView.moveRight()
        }
    }
}