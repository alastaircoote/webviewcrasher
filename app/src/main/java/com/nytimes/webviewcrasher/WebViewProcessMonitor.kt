package com.nytimes.webviewcrasher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.webkit.WebView
import androidx.webkit.WebViewRenderProcess
import androidx.webkit.WebViewRenderProcessClient
import java.lang.RuntimeException
import java.util.*
import kotlin.concurrent.schedule

typealias GetRestartIntent = () -> Intent

// This is the core of the additional androidx functionality. It lets us detect when a browser
// render process has stopped responding and act accordingly.
class WebViewProcessMonitor(val getRestartIntent: GetRestartIntent) : WebViewRenderProcessClient() {

    override fun onRenderProcessResponsive(view: WebView, renderer: WebViewRenderProcess?) {
        // Not sure we have much use for this, really. In theory a render process can come back to
        // life after a long script evaluation but we're destroying the process whenever it becomes
        // unresponsive (which already has a ~5 second delay), not sure we'll want to wait much longer
        // than that.
        println("Webview became responsive again!")
    }

    override fun onRenderProcessUnresponsive(view: WebView, renderer: WebViewRenderProcess?) {
        println("Webview detected that render process has become inactive")
        if (renderer == null) {
            throw RuntimeException("No webview renderer to terminate, so crashing")
        }

        println("We have access to the webview render process, so we're going to ask it to terminate.")

        // Because we have no way of knowing what the outcome of this termination is, we boot up
        // a background service that monitors if and when the app process dies.

        Intent(view.context, CrashRecoveryService::class.java).also { intent ->

            // We tell the service which process ID to look for
            val pid = android.os.Process.myPid()

            // I'm sure there's a more sensible way of doing this but hey, proof of concept!
            intent.data = Uri.parse("process://$pid")

            // This is the intent we want to use when the service detects a crash, to restart
            // the app.
            intent.putExtra(CrashRecoveryService.RESTART_INTENT,getRestartIntent())

            val serviceConnection = object : ServiceConnection {
                override fun onServiceDisconnected(name: ComponentName?) {
                    println("Crash recovery service disconnected")
                }

                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    println("Connnected to recovery service, terminating render process")

                    renderer.terminate()

                    val unbind = {
                        println("5 seconds have passed, presumably we survived.")
                        view.context.unbindService(this)
                    }

                    Timer("Successful webview recovery",false).schedule(5000) {
                        unbind()
                    }
                }
            }

            view.context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        }


    }



}