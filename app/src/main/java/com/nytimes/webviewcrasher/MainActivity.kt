package com.nytimes.webviewcrasher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_webview_container.view.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    // This is our "state", just a list of letters
    var letters:MutableList<Char> = mutableListOf()

    // The process monitor we'll attach to all of our webviews
    val processMonitor = WebViewProcessMonitor {
        Intent(this.applicationContext, this::class.java).also {
            it.putExtra("APPLICATION_STATE", letters.toCharArray())
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WebView.setWebContentsDebuggingEnabled(true)

        addHandled.setOnClickListener {
            addAWebView(true)
        }

        addUnhandled.setOnClickListener {
            addAWebView(false)
        }

        intent.extras?.getCharArray("APPLICATION_STATE")?.let {
            println("BUNDLE?? ${it[0]}")
            this.letters = it.toMutableList()
            this.letters.forEach { char ->
                addAWebView(true, char)
            }
        }

        // Stupid hack but the absolute layout we're setting in resizeViews doesn't work on initial
        // load because all the measured sizes are 0. So when that changes, check again.
        outerFrame.addOnLayoutChangeListener { _, _, _, _, _, oldLeft, oldTop, oldRight, oldBottom ->
            if (oldLeft == oldRight || oldTop == oldBottom) {
                resizeViews()
            }
        }
    }

    var currentWebviews: ArrayList<View> = arrayListOf()

    val margin:Int = 10

    fun resizeViews() {
        var numOfWebViewRows = 1
        if (this.currentWebviews.size > 2) {
            numOfWebViewRows = ceil(this.currentWebviews.size / 2.0).toInt()
        }

        println("HEIGHT, ${frameLayout.height}, ${frameLayout.measuredHeight}")
        val rowHeight = frameLayout.measuredHeight / numOfWebViewRows

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

    fun setWebview(child: LinearLayout, handled: Boolean, letter:Char) {
        child.webView.settings.javaScriptEnabled = true


        WebViewCompat.setWebViewRenderProcessClient(child.webView, processMonitor)

        child.webView.webViewClient = object : WebViewClient() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onRenderProcessGone(
                view: WebView?,
                detail: RenderProcessGoneDetail?
            ): Boolean {
                println("Render process is gone")
                return if (handled) {
                    val urlHash = Uri.parse(child.webView.url).fragment
                    val char = urlHash?.toCharArray()?.get(0)
                    removeWebView(child)
                    addAWebView(handled, char)
                    true
                } else {
                    false
                }

            }
        }

        child.webView.loadUrl("file:///android_asset/index.html#$letter")
    }

    fun removeWebView(child:LinearLayout) {
        frameLayout.removeView(child)
        currentWebviews.remove(child)
        child.webView.destroy()
        resizeViews()
    }

    fun addAWebView(handled:Boolean, predefinedLetter: Char? = null) {

        val letter = predefinedLetter ?: Random.nextInt(65,90).toChar()
        if (predefinedLetter == null) {
            letters.add(letter)
        }

        val child = layoutInflater.inflate(R.layout.fragment_webview_container,null) as LinearLayout

        setWebview(child, handled, letter)
        child.setBackgroundColor(if (handled) Color.GREEN else Color.RED)


        frameLayout.addView(child)
        currentWebviews.add(child)

        child.hangButton.setOnClickListener {
            child.webView.evaluateJavascript("while(1) {}") {
                println("Sending hanging JS to webview")
            }
        }


        child.destroyButton.setOnClickListener {
            removeWebView(child)
        }


        resizeViews()

    }
}
