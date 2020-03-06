package com.nytimes.webviewcrasher

import android.webkit.WebView
import java.lang.ref.WeakReference
import java.lang.reflect.Proxy

class WebViewMonitor {

    companion object {

        private var webviewReferences:MutableSet<WeakReference<WebView>> = mutableSetOf()

        val allWebViews: Array<WebView>
            get() {
                val webviews = arrayListOf<WebView>()
                for (weakref in webviewReferences) {
                    val webView = weakref.get()
                    if (webView == null) {
                        webviewReferences.remove(weakref)
                    } else {
                        webviews.add(webView)
                    }
                }
                return webviews.toTypedArray()
            }

        fun setup() {

            // We take advantage of the fact that all webviews call back to a single "WebViewFactoryProvider"
            // to create webviews, and that it stores a reference to that provider in a static variable.
            // So we store a custom proxy in that variable slot, wrapping the original provider and tracking
            // when new webviews are created.

            // The provider class we're going to wrap:
            val factoryProviderClass = Class.forName("android.webkit.WebViewFactoryProvider")
            // And the underlying factory:
            val factoryClass = Class.forName("android.webkit.WebViewFactory")

            // The static variable where the provider is stored after it is created:
            val currentlyStoredFactoryProviderField = factoryClass.getDeclaredField("sProviderInstance")

            // This is the method that returns the provider. If the static variable above is filled
            // it returns that instead.
            val getProviderMethod = factoryClass.getDeclaredMethod("getProvider")

            // Let's check and make sure that this variable is the type we're expecting:
            if (currentlyStoredFactoryProviderField.type != factoryProviderClass) {
                throw Error("Underlying API is not in the shape we're expecting")
            }

            // Make both that variable and the getProvider() method accessible:
            currentlyStoredFactoryProviderField.isAccessible = true
            getProviderMethod.isAccessible = true

            // The static variable gets set whenever the first WebView created. So if it's
            // set it means there is a WebView out there we aren't tracking. Which would be bad!
            // So we throw an error to make sure that we're putting this in the right place.
            if (currentlyStoredFactoryProviderField.get(factoryClass) != null) {
                throw Error("WebView provider is already set up. You must set up the monitor first.")
            }

            // Now we manually invoke getProvider(), same as our first webview would have done
            val providerInstance = getProviderMethod.invoke(factoryClass)

            // And wrap it in a proxy that tracks whenever a webview is created, storing a reference.
            val proxy = Proxy.newProxyInstance(
                providerInstance.javaClass.classLoader,
                arrayOf(currentlyStoredFactoryProviderField.type)
            ) { _, method, methodArgs ->
                if (method.name == "createWebView") {
                    val webView = methodArgs[0] as WebView
                    // We store a weak reference to the webview so that when the code is done with
                    // it we don't keep it hanging around:
                    val weakRef = WeakReference(webView)
                    webviewReferences.add(weakRef)
                }
                // No matter what, invoke the actual method wanted anyway:
                method.invoke(providerInstance,*methodArgs)
            }

            // Now store the proxy in the static variable field so that all webview
            // creations use it:
            currentlyStoredFactoryProviderField.set(factoryClass, proxy)

            // And make it inAccessible again:
            currentlyStoredFactoryProviderField.isAccessible = false
        }
    }

}