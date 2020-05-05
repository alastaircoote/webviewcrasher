# WebView Crasher

## What is this?

A very basic proof of concept using the androidx.webkit helpers for Android Webviews.

## What does it do?

It should work out of the box with Android Studio. When you launch the app you'll see two buttons:

- add handled webview
- add unhandled webview

When you tap either you'll see a webview appear. Each one is assigned a random letter to demonstrate the idea of saving state on crash (more on on that later)

Underneath each webview are two buttons:

- hang
- destroy

Destroy will simply remove this webview from the window. Hang will send some JavaScript into the browser that will cause the browser render process to become unresponsive. You should see the timers on all the webviews stop. If you tap on one of the webviews you'll send an input event that will trigger the web process crash listener a few (less than 5) seconds later.

When that listener is triggered we bind to the CrashRecoveryService on another process, passing it an intent for relaunching the app with the current application state attached. What happens next depends on:

#### You have an 'unhandled' webview

Because that webview does't have a renderProcessGone listener it will cause the app to crash. When that happens our CrashRecoveryService kicks in, relaunching the activity with the current application state attached, meaning you should see the same letters reappear in the webviews.

#### You have no 'unhandled' webviews

The webviews should disappear but then quickly reappear as we recreate them inside the renderProcessGone listener. (they will actually be recreated in the wrong order right now...)
