package com.nytimes.webviewcrasher

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_webview_container.view.*
import kotlin.math.ceil
import kotlin.math.floor


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WebView.setWebContentsDebuggingEnabled(true)

        addButton.setOnClickListener {
            addAWebView()
        }

    }

    var currentWebviews: ArrayList<View> = arrayListOf()

    val margin:Int = 10

    fun resizeViews() {
        var numOfWebViewRows = 1
        if (this.currentWebviews.size > 2) {
            numOfWebViewRows = ceil(this.currentWebviews.size / 2.0).toInt()
        }

        var rowHeight = frameLayout.measuredHeight / numOfWebViewRows

        this.currentWebviews.forEachIndexed { idx, wv ->
            val rowNumber = floor(idx / 2.0).toInt()
            val width = frameLayout.measuredWidth / 2 - (margin * 2)
            val height = rowHeight - (margin * 2)
            val frame = FrameLayout.LayoutParams(width, height)
            frame.topMargin = (rowHeight * rowNumber) + margin
            if (idx % 2 > 0) {
                frame.leftMargin = (frameLayout.measuredWidth / 2) + margin
            } else {
                frame.leftMargin = margin
            }
            wv.layoutParams = frame
        }
    }

    fun addAWebView() {

        val child = layoutInflater.inflate(R.layout.fragment_webview_container,null)
        child.webView.settings.javaScriptEnabled = true
        frameLayout.addView(child)
        currentWebviews.add(child)

        child.hangButton.setOnClickListener {
            child.webView.loadUrl("chrome://hang")
        }
        child.destroyButton.setOnClickListener {
            frameLayout.removeView(child)
            currentWebviews.remove(child)
            child.webView.destroy()
            resizeViews()
        }

        child.crashButton.setOnClickListener {
            child.webView.loadUrl("chrome://crash")
        }

        child.webView.loadUrl("file:///android_asset/index.html")

        resizeViews()

    }
}
