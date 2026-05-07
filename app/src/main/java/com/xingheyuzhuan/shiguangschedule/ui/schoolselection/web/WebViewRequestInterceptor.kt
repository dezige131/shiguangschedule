package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.util.concurrent.TimeUnit

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class WebViewRequestInterceptor {
    companion object {
        private val clientWithRedirects = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        private val clientNoRedirects = clientWithRedirects.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

        // Registry for POST bodies sent from JS
        private val postBodyRegistry = java.util.Collections.synchronizedMap(mutableMapOf<String, RegisteredPostData>())

        fun registerPostData(id: String, body: String, contentType: String) {
            postBodyRegistry[id] = RegisteredPostData(body, contentType)
        }

        private data class RegisteredPostData(val body: String, val contentType: String)
    }

    private val cookieManager = CookieManager.getInstance()

    fun intercept(request: WebResourceRequest): WebResourceResponse? {
        val url = request.url.toString()
        
        // Skip non-http(s) urls
        if (!url.startsWith("http")) return null
        
        // 1. Check for registered POST body ID (from JS)
        val requestIdHeader = request.requestHeaders["X-WebView-Post-Id"]
        val requestIdParam = request.url.getQueryParameter("_webview_post_id")
        val requestId = requestIdHeader ?: requestIdParam
        
        val registeredData = requestId?.let { postBodyRegistry.remove(it) }

        // If it's a POST and we DON'T have a registered body, we can't strip the header reliably 
        // unless we let WebView handle it (which fails the user's requirement).
        // But if we HAVE a registered body, we MUST intercept regardless of method.
        if (request.method.uppercase() != "GET" && registeredData == null) {
            return null
        }

        // For main frame, we want to handle redirects manually to update WebView's URL state via JS
        val client = if (request.isForMainFrame) clientNoRedirects else clientWithRedirects

        try {
            val builder = Request.Builder().url(url)
            
            // 2. Set Method and Body
            if (registeredData != null) {
                val mediaType = registeredData.contentType.ifBlank { "application/x-www-form-urlencoded" }.toMediaTypeOrNull()
                val body = registeredData.body.toRequestBody(mediaType)
                builder.method(request.method, body)
            } else {
                builder.method(request.method, null)
            }

            // 3. Copy headers from WebView, excluding X-Requested-With and our internal ID header
            request.requestHeaders.forEach { (key, value) ->
                if (!key.equals("X-Requested-With", ignoreCase = true) && 
                    !key.equals("X-WebView-Post-Id", ignoreCase = true)) {
                    builder.addHeader(key, value)
                }
            }

            // 4. Sync Cookies from WebView to OkHttp
            val cookies = cookieManager.getCookie(url)
            if (!cookies.isNullOrEmpty()) {
                builder.addHeader("Cookie", cookies)
            }

            val response = client.newCall(builder.build()).execute()

            // 5. Sync Cookies from OkHttp back to WebView
            val responseHeaders = response.headers
            responseHeaders.values("Set-Cookie").forEach {
                cookieManager.setCookie(url, it)
            }
            cookieManager.flush()

            // 6. Handle Redirects (3xx) for main frame
            if (response.code in 300..399) {
                if (request.isForMainFrame) {
                    val location = response.header("Location")
                    if (location != null) {
                        val absoluteLocation = response.request.url.resolve(location)?.toString() ?: location
                        val html = "<html><script>window.location.replace('$absoluteLocation');</script></html>"
                        response.close()
                        return WebResourceResponse(
                            "text/html",
                            "UTF-8",
                            200,
                            "OK",
                            mapOf("Cache-Control" to "no-cache"),
                            html.byteInputStream()
                        )
                    }
                }
                response.close()
                return null
            }

            // 7. Construct WebResourceResponse
            val contentType = response.header("Content-Type")
            val mimeType = contentType?.substringBefore(";") ?: "text/html"
            val encoding = contentType?.substringAfter("charset=", "UTF-8") ?: "UTF-8"

            val responseHeadersMap = mutableMapOf<String, String>()
            for (i in 0 until responseHeaders.size) {
                val name = responseHeaders.name(i)
                // Don't pass through Content-Encoding (like gzip) as WebView handles it differently
                if (!name.equals("Content-Encoding", ignoreCase = true)) {
                    responseHeadersMap[name] = responseHeaders.value(i)
                }
            }

            return WebResourceResponse(
                mimeType,
                encoding,
                response.code,
                response.message.ifBlank { "OK" },
                responseHeadersMap,
                response.body.byteStream()
            )
        } catch (e: Exception) {
            Log.e("WebViewInterceptor", "Error intercepting request: $url", e)
            return null
        }
    }
}
