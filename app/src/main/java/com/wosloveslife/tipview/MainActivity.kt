package com.wosloveslife.tipview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private var tutorialLayout: TutorialLayout? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn2.setOnClickListener {
            show()
        }

        btn1.setOnClickListener {
            tutorialLayout?.dismiss()
        }
    }

    private fun show() {
        val anchor = LayoutInflater.from(this).inflate(R.layout.tutorial_gc_switcher, null)

        val margin = (resources.displayMetrics.density * 48).toInt()
        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(margin, (window.decorView.height / 2.5).toInt(), margin, margin)
        tutorialLayout = TutorialLayout.Builder(this)
                .addView(anchor, params)
                .addCable(anchor, btn1)
                .addExhibit(ShowcaseView.Exhibit.Shape.SHAPE_OVAL, true, btn1, true)
                .build()
        tutorialLayout!!
                .show(this)
    }
}
