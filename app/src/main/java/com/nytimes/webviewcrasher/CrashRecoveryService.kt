package com.nytimes.webviewcrasher

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.widget.Toast
import java.util.*


class CrashWatchEntry(val processId: Int, val restartIntent: Intent) {

}

class CrashRecoveryService : Service() {

    companion object {
        const val RESTART_INTENT = "RESTART_INTENT"
    }

    var currentEntries:MutableMap<Int, Intent> = mutableMapOf()

    fun getProcessId(intent: Intent): Int? {
        return intent.data?.host?.toInt()
    }

    override fun onBind(intent: Intent?): IBinder? {

        if (intent == null) {
            return null
        }

        val processId = getProcessId(intent)
        if (processId == null) {
            println("Could not parse out process ID, ignoring")
            return null
        }


        val intentToLaunchOnRestart = intent.extras?.get(RESTART_INTENT) as? Intent

        if (intentToLaunchOnRestart == null) {
            println("Ignoring bind as it has not provided the required information")
            return null
        }

        // According to the docs, intent extras are not available in onUnbind, so we store
        // them when they arrive.

        currentEntries[processId] = intentToLaunchOnRestart

        val t = Toast.makeText(this,"Encountered a problem, recovering...", Toast.LENGTH_SHORT)
        t.show()

        return Binder()
    }

    override fun onDestroy() {
        println("Destroying service")
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {

        if (intent == null) {
            return false
        }

        val processId = getProcessId(intent)
        if (processId == null) {
            println("Could not parse out process ID, ignoring")
            return false
        }

        val restartIntent = currentEntries.remove(processId)
        if (restartIntent == null) {
            println("No crash entry for unbind with intent, ignoring it.")
            return false
        }

        println("Checking for presence of process ${processId}")
        val activityManager = this.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        val processExists = activityManager.runningAppProcesses.any {
            it.pid == processId
        }

        if (processExists) {
            println("The process still exists, so presumably it was able to terminate and recreate webviews successfully.")
            return false
        }

        println("Process has disappeared, so firing the restart intent")
        startActivity(restartIntent)
        return false
    }


}
