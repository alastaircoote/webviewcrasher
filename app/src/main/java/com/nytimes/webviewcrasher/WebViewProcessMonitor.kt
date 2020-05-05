package com.nytimes.webviewcrasher

import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.os.Parcelable
import android.webkit.WebView
import androidx.webkit.WebViewRenderProcess
import androidx.webkit.WebViewRenderProcessClient
import java.lang.RuntimeException
import java.util.*
import kotlin.concurrent.schedule

typealias GetRestartIntent = () -> Intent

class WebViewProcessMonitor(val getRestartIntent: GetRestartIntent) : WebViewRenderProcessClient() {

    override fun onRenderProcessResponsive(view: WebView, renderer: WebViewRenderProcess?) {
        println("Webview became responsive again!")
    }

    override fun onRenderProcessUnresponsive(view: WebView, renderer: WebViewRenderProcess?) {
        println("Webview detected that render process has become inactive")
        if (renderer == null) {
            throw RuntimeException("No webview renderer to terminate, so crashing")
        }

        println("We have access to the webview render process, so we're going to ask it to terminate.")

        val appContext = view.context.applicationContext

        // Because we have no way of knowing what the outcome of this termination is, we boot up
        // a background service that monitors if and when the app process dies.

        Intent(view.context, CrashRecoveryService::class.java).also { intent ->

            // We tell the service which process ID to look for
            val pid = android.os.Process.myPid()

            // I'm sure there's a more sensible way of doing this but hey, proof of concept!
            intent.data = Uri.parse("process://$pid")

            // This is the intent we want to use when the service detects a crash, to restart
            // the app.
            val restartIntent = getRestartIntent()
            intent.putExtra(CrashRecoveryService.RESTART_INTENT,restartIntent)

            val conn = object : ServiceConnection {
                override fun onServiceDisconnected(name: ComponentName?) {
                    println("Crash recovery service disconnected")
                }

                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    println("Connnected to recovery service, terminating render process")

                    renderer.terminate()

                    val connection = this

                    Timer("Successful webview recovery",false).schedule(5000) {
                        println("5 seconds have passed, presumably we survived.")
                        view.context.unbindService(connection)
                    }
                }

            }



            view.context.bindService(intent, conn, Context.BIND_AUTO_CREATE)



        }


    }



}