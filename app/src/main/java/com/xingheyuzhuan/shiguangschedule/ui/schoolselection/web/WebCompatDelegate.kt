package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import android.graphics.Bitmap
import android.webkit.*

class WebCompatDelegate(private val webView: WebView) {

    fun enhanceSettings(isDesktopMode: Boolean): WebCompatDelegate {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowUniversalAccessFromFileURLs = true
            allowFileAccessFromFileURLs = true
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            if (isDesktopMode) {
                useWideViewPort = true
                loadWithOverviewMode = false
                layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
            } else {
                useWideViewPort = false
                loadWithOverviewMode = true
            }

            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        return this
    }

    fun wrapWebViewClient(original: WebViewClient, isDesktopMode: Boolean): WebViewClient {
        return object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                original.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                original.onPageFinished(view, url)

                if (isDesktopMode) {
                    view?.evaluateJavascript("""
                        (function() {
                            var metas = document.getElementsByTagName('meta');
                            for (var i = metas.length - 1; i >= 0; i--) {
                                if (metas[i].getAttribute('name') === 'viewport') metas[i].parentNode.removeChild(metas[i]);
                            }
                            var meta = document.createElement('meta');
                            meta.name = "viewport";
                            meta.content = "width=1920, initial-scale=1.0, minimum-scale=1.0, maximum-scale=5.0, user-scalable=yes";
                            document.head.appendChild(meta);
                            var css = 'html, body { width: 1920px !important; min-width: 1920px !important; overflow-x: auto !important; position: static !important; }';
                            var style = document.createElement('style');
                            style.type = 'text/css';
                            style.appendChild(document.createTextNode(css));
                            document.head.appendChild(style);
                        })();
                    """.trimIndent(), null)
                }
            }

            override fun onReceivedSslError(v: WebView, h: SslErrorHandler, e: android.net.http.SslError) = original.onReceivedSslError(v, h, e)
            override fun onReceivedError(v: WebView, q: WebResourceRequest, e: WebResourceError) = original.onReceivedError(v, q, e)
        }
    }

    fun wrapWebChromeClient(original: WebChromeClient, onProgress: (Int) -> Unit): WebChromeClient {
        return object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                onProgress(newProgress)
                original.onProgressChanged(view, newProgress)
            }
            override fun onReceivedTitle(v: WebView?, t: String?) = original.onReceivedTitle(v, t)
        }
    }
}