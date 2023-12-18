package com.cyllxapk.tapgame

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.lifecycle.lifecycleScope
import com.cyllxapk.tapgame.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var mainView: ActivityMainBinding
    private lateinit var inAnimation: Animation
    private lateinit var outAnimation: Animation
    private var sound: MediaPlayer? = null

    private lateinit var gameButtons: ArrayList<View>
    private lateinit var gameButtonsToSounds: HashMap<View, Int>
    private val generatedSequenceOfButtonsClicks = arrayListOf<Int>()
    private var currentPositionForValidation = 0
    private var personalBest = 0

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainView = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainView.root)

        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        mainView.apply {
            gameButtons = arrayListOf(blueButton, redButton, greenButton, yellowButton)
            gameButtonsToSounds = hashMapOf(
                blueButton to R.raw.blue_bt_tap_sound,
                redButton to R.raw.red_bt_tap_sound,
                greenButton to R.raw.green_bt_tap_sound,
                yellowButton to R.raw.yellow_bt_tap_sound
            )
        }

        lifecycleScope.launch {
            personalBest = loadRecord(applicationContext)
            val personalBestText = getString(R.string.personal_best) + " $personalBest"
            mainView.personalBest.text = personalBestText
        }

        inAnimation = AnimationUtils.loadAnimation(this, R.anim.click_anim_in)
        outAnimation = AnimationUtils.loadAnimation(this, R.anim.click_anim_out)

        setGameButtonsClickable(false)

        mainView.startButton.setOnClickListener {
            setStartButtonClickable(false)
            systemMove()
        }
    }

    fun gameButtonClick(button: View) {

        val playerClick = gameButtons.indexOfFirst { it == button }
        val requiredClick = generatedSequenceOfButtonsClicks[currentPositionForValidation]

        if (playerClick == requiredClick) {

            startClickEffect(button, false)

            if (playerCoped()) {

                if (generatedSequenceOfButtonsClicks.size > personalBest) setNewRecord()

                currentPositionForValidation = 0
                setGameButtonsClickable(false)
                systemMove()

            } else currentPositionForValidation ++

        } else {

            startClickEffect(button, true)
            defeat()

        }

    }

    private fun playerCoped() = generatedSequenceOfButtonsClicks.size - 1 == currentPositionForValidation

    private fun setNewRecord() {
        personalBest = generatedSequenceOfButtonsClicks.size

        lifecycleScope.launch {
            saveRecord(applicationContext, personalBest)
            val personalBestText = getString(R.string.personal_best) + " $personalBest"
            mainView.personalBest.text = personalBestText
        }
    }

    private fun defeat() {
        setGameButtonsClickable(false)
        setStartButtonClickable(true)
        generatedSequenceOfButtonsClicks.clear()
        currentPositionForValidation = 0
    }

    private fun systemMove() {
        val timeBetweenClicks: Long = when (generatedSequenceOfButtonsClicks.size / 5) {
            0 -> 600
            1 -> 500
            2 -> 400
            else -> 300
        }

        generatedSequenceOfButtonsClicks.add((0..3).random())

        lifecycleScope.launch {
            delay(500)
            mainView.currentNumberOfMemorization.text = "${generatedSequenceOfButtonsClicks.size}"
            delay(600)
            generatedSequenceOfButtonsClicks.forEach { index ->
                val currentButton = gameButtons[index]
                startClickEffect(currentButton, false)
                delay(timeBetweenClicks)
            }
            setGameButtonsClickable(true)
        }
    }

    private fun setGameButtonsClickable(isClickable: Boolean) {
        gameButtons.forEach { it.isClickable = isClickable }
    }

    private fun setStartButtonClickable(isClickable: Boolean) {
        mainView.startButton.isClickable = isClickable
    }

    private fun startClickEffect(gameButton: View, isDefeat: Boolean) {
        val soundID = if (isDefeat) R.raw.defeat_sound else gameButtonsToSounds[gameButton]!!
        lifecycleScope.launch {
            gameButton.startAnimation(outAnimation)
            playSound(soundID)
            delay(150)
            gameButton.startAnimation(inAnimation)
        }
    }

    private fun playSound(soundID: Int) {
        resetMediaPlayer()
        sound = MediaPlayer.create(applicationContext, soundID)
        sound?.start()
    }

    private fun resetMediaPlayer() {
        sound?.stop()
        sound?.release()
        sound = null
    }

}

private suspend fun saveRecord(context: Context, record: Int) = withContext(Dispatchers.IO) {
    context.openFileOutput("saved_record.txt", Context.MODE_PRIVATE).use {
        it.write(record.toString().toByteArray())
    }
}

private suspend fun loadRecord(context: Context): Int {
    val readValue: Int

    withContext(Dispatchers.IO) {
        readValue = try {
            context.openFileInput("saved_record.txt").bufferedReader().readLine().toInt()
        } catch (e: IOException) {
            0
        }
    }

    return readValue
}