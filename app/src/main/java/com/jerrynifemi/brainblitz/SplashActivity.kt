package com.jerrynifemi.brainblitz

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView

/** Animated launch screen: badge pops, rings pulse, title slides up, then home. */
class SplashActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private var launched = false
    private val goHome = Runnable { go() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        Audio.preload(this)
        handler.postDelayed({ Audio.play(this, R.raw.sfx_splash) }, 220)

        val badge = findViewById<ImageView>(R.id.logoBadge)
        val title = findViewById<TextView>(R.id.tvSplashTitle)
        val tagline = findViewById<TextView>(R.id.tvSplashTagline)
        val ring1 = findViewById<View>(R.id.ring1)
        val ring2 = findViewById<View>(R.id.ring2)
        val glow = findViewById<View>(R.id.badgeGlow)

        badge.scaleX = 0.3f
        badge.scaleY = 0.3f
        badge.alpha = 0f
        badge.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(620)
            .setInterpolator(OvershootInterpolator(1.8f))
            .start()

        glow.alpha = 0f
        glow.animate().alpha(1f).setDuration(700).start()

        pulse(ring1, 0L)
        pulse(ring2, 260L)

        title.translationY = 34f
        title.alpha = 0f
        title.animate().translationY(0f).alpha(1f).setDuration(460).setStartDelay(260).start()

        tagline.alpha = 0f
        tagline.animate().alpha(1f).setDuration(500).setStartDelay(520).start()

        handler.postDelayed(goHome, 1750)
        findViewById<View>(R.id.splashRoot).setOnClickListener { go() }
    }

    private fun pulse(view: View, delay: Long) {
        view.scaleX = 0.7f
        view.scaleY = 0.7f
        view.alpha = 0.55f
        view.animate()
            .scaleX(2.1f).scaleY(2.1f).alpha(0f)
            .setDuration(1250)
            .setStartDelay(delay)
            .start()
    }

    private fun go() {
        if (launched) return
        launched = true
        handler.removeCallbacks(goHome)
        startActivity(Intent(this, HomeActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
