package com.suenara.coloringcomplexdrawable

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimatedStateListDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.util.StateSet
import android.widget.CheckBox
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.suenara.enhancedvectordrawable.*
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val d = makeAnimated()
        findViewById<CheckBox>(R.id.custom_checkbox).buttonDrawable = d
        findViewById<ImageView>(R.id.check_on).setImageDrawable(getDrawable(R.drawable.vd_check_on))
        findViewById<ImageView>(R.id.check_off).setImageDrawable(getDrawable(R.drawable.vd_check_off))

        findViewById<ImageView>(R.id.custom_check_on).setImageDrawable(
            EnhancedVectorDrawable(
                this,
                R.drawable.vd_check_on
            )
        )
        findViewById<ImageView>(R.id.custom_check_off).setImageDrawable(
            EnhancedVectorDrawable(
                this,
                R.drawable.vd_check_off
            )
        )


        val repeatTime = 3000L
        findViewById<ImageView>(R.id.animated_check_on).setImageDrawable(getDrawable(R.drawable.avd_check_off_to_on).also {
            Timer().scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    runOnUiThread {
                        (it as Animatable).stop()
                        (it as Animatable).start()
                    }
                }
            }, 0, repeatTime)
        })
        findViewById<ImageView>(R.id.custom_animated_check_on).setImageDrawable(
            EnhancedAnimatedVectorDrawable(
                this,
                R.drawable.avd_check_off_to_on
            ).modifyColors().also {
                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        runOnUiThread {
                            it.start()
                        }
                    }
                }, 1500)
            })

        findViewById<ImageView>(R.id.animated_call).setImageDrawable(getDrawable(R.drawable.avd_call_in_progress).also {
            Timer().scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    runOnUiThread {
                        (it as Animatable).stop()
                        (it as Animatable).start()
                    }
                }
            }, 0, repeatTime)
        })
        findViewById<ImageView>(R.id.custom_animated_call).setImageDrawable(
            EnhancedAnimatedVectorDrawable(
                this,
                R.drawable.avd_call_in_progress
            ).also {
                Timer().scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        runOnUiThread{
                            it.stop()
                            it.start()
                        }
                    }
                }, 0, repeatTime)
            })

        findViewById<ImageView>(R.id.animated_gear).apply {
            setImageResource(R.drawable.gear)
            Timer().scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    runOnUiThread {
                        (drawable as Animatable).stop()
                        (drawable as Animatable).start()
                    }
                }
            }, 0, repeatTime)

        }

        findViewById<ImageView>(R.id.custom_animated_gear).setImageDrawable(
            EnhancedAnimatedVectorDrawable(
                this,
                R.drawable.gear
            ).also { drawable ->
                drawable.setFillColor("gear", Color.MAGENTA)
                drawable.setFillColor("cutout", Color.BLUE)
                Timer().scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        runOnUiThread{
                            drawable.stop()
                            drawable.start()
                        }
                    }
                }, 0, repeatTime)
            }
        )
    }

    private fun makeAnimated(): StateListDrawable {
        val an = AnimatedStateListDrawable()
        an.addState(
            intArrayOf(android.R.attr.state_checked),
            EnhancedVectorDrawable(this, R.drawable.vd_check_on)!!,
            R.drawable.vd_check_on
        )
        an.addState(StateSet.NOTHING, EnhancedVectorDrawable(this, R.drawable.vd_check_off)!!, R.drawable.vd_check_off)
        an.addTransition(
            R.drawable.vd_check_on,
            R.drawable.vd_check_off,
            EnhancedAnimatedVectorDrawable(this, R.drawable.avd_check_on_to_off).modifyColors(),
            false
        )
        an.addTransition(
            R.drawable.vd_check_off,
            R.drawable.vd_check_on,
            EnhancedAnimatedVectorDrawable(this, R.drawable.avd_check_off_to_on).modifyColors(),
            false
        )
        an.jumpToCurrentState()
        return an
    }

    private fun EnhancedAnimatedVectorDrawable.modifyColors() = also {
        it.changeAnimations("bg", AnimationTarget.Property.STROKE_COLOR, Color.RED, Color.GREEN, Color.CYAN)
        it.setStrokeColor("fg", Color.BLACK)
    }

    inline fun <reified T> Any.cast(): T = this as T
}