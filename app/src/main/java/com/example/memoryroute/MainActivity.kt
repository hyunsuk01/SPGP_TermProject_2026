package com.example.memoryroute

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val gameView = findViewById<GameView>(R.id.gameView)

        findViewById<ImageButton>(R.id.btnUp).setOnClickListener {
            gameView.moveUp()
        }

        findViewById<ImageButton>(R.id.btnDown).setOnClickListener {
            gameView.moveDown()
        }

        findViewById<ImageButton>(R.id.btnLeft).setOnClickListener {
            gameView.moveLeft()
        }

        findViewById<ImageButton>(R.id.btnRight).setOnClickListener {
            gameView.moveRight()
        }
    }
}