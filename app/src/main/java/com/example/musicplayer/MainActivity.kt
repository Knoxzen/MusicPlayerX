package com.example.musicplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var albumArtImageView: ImageView
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var playButton: ImageButton
    private lateinit var seekBar: SeekBar
    private var selectedFileUri: Uri? = null
    private var isSeekBarTracking: Boolean = false
    private var currentMusicPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request storage permission if not granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        }

        // Set up the play button click listener
        playButton = findViewById(R.id.playBtn)
        playButton.setOnClickListener {
            if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                pauseMusic()
            } else {
                if (selectedFileUri != null) {
                    if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying) {
                        resumeMusic()
                    } else {
                        playMusic(selectedFileUri!!)
                    }
                } else {
                    // Show file picker to choose the music file
                    showFilePicker()
                }
            }
        }

        // Set up the seek bar and attach seek bar change listener
        seekBar = findViewById(R.id.seekBar)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSeekBarTracking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isSeekBarTracking = false
            }
        })

        /// Set up album art ImageView
        albumArtImageView = findViewById(R.id.default_album_art)

        // Update the seek bar progress periodically
        Thread(Runnable {
            while (true) {
                try {
                    Thread.sleep(1000)
                    runOnUiThread {
                        if (::mediaPlayer.isInitialized && !isSeekBarTracking) {
                            seekBar.progress = mediaPlayer.currentPosition
                        }
                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }).start()
    }

    private fun showFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "audio/*"
        startActivityForResult(intent, FILE_PICKER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            selectedFileUri = data.data
            if (selectedFileUri != null) {
                val cursor = contentResolver.query(selectedFileUri!!, null, null, null, null)
                cursor?.use {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    it.moveToFirst()
                    playMusic(selectedFileUri!!)
                }
            }
        }
    }


    private fun playMusic(uri: Uri) {
        mediaPlayer = MediaPlayer.create(this, uri)
        mediaPlayer.start()
        playButton.setImageResource(R.drawable.baseline_pause_24)
        seekBar.max = mediaPlayer.duration

        // Retrieve album art and display it
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, uri)
        val rawAlbumArt = retriever.embeddedPicture
        if (rawAlbumArt != null) {
            val bitmap = BitmapFactory.decodeByteArray(rawAlbumArt, 0, rawAlbumArt.size)
            albumArtImageView.setImageBitmap(bitmap)}
//        } else {
//            // Set a default image if album art is not available
//            albumArtImageView.setImageResource(R.drawable.default_album_art)
//        }
    }

    private fun pauseMusic() {
        mediaPlayer.pause()
        currentMusicPosition = mediaPlayer.currentPosition
        playButton.setImageResource(R.drawable.baseline_play_arrow_24)
    }

    private fun resumeMusic() {
        mediaPlayer.seekTo(currentMusicPosition)
        mediaPlayer.start()
        playButton.setImageResource(R.drawable.baseline_pause_24)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
    }

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val FILE_PICKER_REQUEST_CODE = 2
    }
}
