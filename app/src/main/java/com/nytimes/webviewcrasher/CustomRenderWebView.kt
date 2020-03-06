package com.nytimes.webviewcrasher

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView
import android.widget.AbsoluteLayout
import java.lang.ref.WeakReference
import java.lang.reflect.Proxy


fun test(c:Context) {


    val factory = Class.forName("android.webkit.WebViewFactory")
    val factoryStoredProvider = factory.getDeclaredField("sProviderInstance")
    factoryStoredProvider.isAccessible = true
    factoryStoredProvider.set(factory, null)
}



class WebViewSeparateProcessFactory {

    var alreadyCreatedProvider:Any? = null

    val factoryClass = Class.forName("android.webkit.WebViewFactory")
    val factoryStoredProvider = factoryClass.getDeclaredField("sProviderInstance")

//    val hmmClass = Class.forName("com.android.webview.chromium.WebViewChromiumFactoryProvider")
//    val hmmCommandLinePath = hmmClass.getDeclaredField("")

    fun createWebView(context: Context) : WebView {
        factoryStoredProvider.isAccessible = true
        // Grab the existing provider, if it exists
        val existingProvider = factoryStoredProvider.get(factoryClass)

        // Now insert our provider, OR null if we haven't yet provided one
        factoryStoredProvider.set(factoryClass, this.alreadyCreatedProvider)

        WebView.setDataDirectorySuffix("sdfgwdfg")
        val wv = WebView(context)

        if (this.alreadyCreatedProvider == null){
            this.alreadyCreatedProvider = factoryStoredProvider.get(factoryClass)
        }
        factoryStoredProvider.set(factoryClass, existingProvider)

        return wv
    }




}