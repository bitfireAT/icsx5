/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid

import android.content.Context
import at.bitfire.cert4android.CustomCertManager
import at.bitfire.cert4android.CustomCertStore
import at.bitfire.icsdroid.ui.ForegroundTracker
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.http.HttpHeaders
import okhttp3.brotli.BrotliInterceptor
import okhttp3.internal.tls.OkHostnameVerifier
import javax.net.ssl.SSLContext

/**
 * Provides the apps [HttpClient] instance with a custom certificate manager and a custom user agent
 * (if provided).
 */
class AppHttpClient @AssistedInject constructor(
    @Assisted customUserAgent: String?,
    @Assisted createEngine: (CustomCertManager, SSLContext) -> HttpClientEngine,
    @ApplicationContext context: Context
) {

    @AssistedFactory
    interface Factory {

        /**
         * Provides the apps [HttpClient] instance with a custom certificate manager and a custom user agent
         * (if provided).
         *
         * @param customUserAgent custom user agent to use, or null to use the default one
         * @param createEngine function to create the [HttpClientEngine] to use. Can be a mock engine.
         */
        fun create(
            customUserAgent: String? = null,
            createEngine: (CustomCertManager, SSLContext) -> HttpClientEngine = { certManager, sslContext ->
                OkHttp.create {
                    addNetworkInterceptor(BrotliInterceptor)
                    config {
                        sslSocketFactory(sslContext.socketFactory, certManager)
                        hostnameVerifier(certManager.HostnameVerifier(OkHostnameVerifier))
                    }
                }
            }
        ): AppHttpClient
    }

    // CustomCertManager is Closeable, but HttpClient will live as long as the application is in memory,
    // so we don't need to close it
    private val certManager = CustomCertManager(
        certStore = CustomCertStore.getInstance(context),
        trustSystemCerts = true,
        appInForeground = ForegroundTracker.inForeground
    )

    private val sslContext = SSLContext.getInstance("TLS")
    init {
        sslContext.init(null, arrayOf(certManager), null)
    }

    val httpClient = HttpClient(createEngine(certManager, sslContext)) {
        // Add user given user agent to all engines
        install(UserAgent) {
            agent = customUserAgent ?: Constants.USER_AGENT
        }

        // Increase default timeouts
        install(HttpTimeout) {
            connectTimeoutMillis = 20_000
            requestTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
        }

        // Enable cookie storage - in memory, will be lost on app restart
        install(HttpCookies)

        // Some servers have issues with the Accept-Charset header. It is actually deprecated/not-recommended by RFC 9110 §12.5.2.
        // Ktor adds it by default, so we need to manually strip it with a custom plugin.
        install(createClientPlugin("RemoveAcceptCharsetHeader") {
            on(Send) { request ->
                request.headers.remove(HttpHeaders.AcceptCharset)
                proceed(request)
            }
        })

        // Disable redirect following, it's handled by CalendarFetcher
        followRedirects = false
    }

}