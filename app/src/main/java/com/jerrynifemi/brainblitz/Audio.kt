package com.jerrynifemi.brainblitz

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

/**
 * One place for every sound: pooled effects plus a single looping music track.
 * The score and effects were synthesised for this game, not pulled from a library.
 */
object Audio {

    private const val PREFS = "brain_blitz_audio"
    private const val KEY_ENABLED = "enabled"
    private const val MUSIC_VOLUME = 0.40f

    private var pool: SoundPool? = null
    private val soundIds = HashMap<Int, Int>()
    private var music: MediaPlayer? = null
    private var musicRes = 0

    private val effects = intArrayOf(
        R.raw.sfx_tap,
        R.raw.sfx_correct,
        R.raw.sfx_wrong,
        R.raw.sfx_streak,
        R.raw.sfx_word,
        R.raw.sfx_pop,
        R.raw.sfx_cascade,
        R.raw.sfx_swap,
        R.raw.sfx_snake_eat,
        R.raw.sfx_snake_die,
        R.raw.sfx_line_clear,
        R.raw.sfx_rotate,
        R.raw.sfx_win,
        R.raw.sfx_gameover,
        R.raw.sfx_splash,
        R.raw.sfx_timewarn,
        R.raw.sfx_levelup
    )

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun enabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, value).apply()
        if (value) resumeMusic(context) else pauseMusic()
    }

    private fun soundPool(context: Context): SoundPool {
        pool?.let { return it }
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val created = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attributes)
            .build()
        effects.forEach { res ->
            soundIds[res] = created.load(context.applicationContext, res, 1)
        }
        pool = created
        return created
    }

    fun play(context: Context, resId: Int, volume: Float = 1f) {
        if (!enabled(context)) return
        val sp = soundPool(context)
        val id = soundIds[resId] ?: return
        try {
            sp.play(id, volume, volume, 1, 0, 1f)
        } catch (_: Exception) {
            // Audio is never worth crashing a game over.
        }
    }

    /** Warm the sample pool so the first effect of a session isn't swallowed. */
    fun preload(context: Context) {
        soundPool(context)
    }

    fun playMusic(context: Context, resId: Int) {
        if (musicRes == resId && music != null) {
            if (enabled(context)) resumeMusic(context)
            return
        }
        music?.release()
        music = null
        musicRes = resId
        if (!enabled(context)) return
        try {
            music = MediaPlayer.create(context.applicationContext, resId)?.apply {
                isLooping = true
                setVolume(MUSIC_VOLUME, MUSIC_VOLUME)
                start()
            }
        } catch (_: Exception) {
            music = null
        }
    }

    fun pauseMusic() {
        try {
            music?.let { if (it.isPlaying) it.pause() }
        } catch (_: Exception) {
        }
    }

    fun resumeMusic(context: Context) {
        if (!enabled(context)) return
        val player = music
        if (player == null) {
            if (musicRes != 0) playMusic(context, musicRes)
            return
        }
        try {
            if (!player.isPlaying) player.start()
        } catch (_: Exception) {
        }
    }

    fun stopMusic() {
        try {
            music?.release()
        } catch (_: Exception) {
        }
        music = null
        musicRes = 0
    }
}
