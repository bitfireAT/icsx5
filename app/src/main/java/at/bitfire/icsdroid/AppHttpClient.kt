/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid

import android.content.Context
import at.bitfire.cert4android.CustomCertManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.engine.okhttp.OkHttpEngine
import io.ktor.client.plugins.UserAgent
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.brotli.BrotliInterceptor
import okhttp3.internal.tls.OkHostnameVerifier
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext

class AppHttpClient @AssistedInject constructor(
    @Assisted customUserAgent: String? = null,
    @Assisted engine: HttpClientEngine,
    @ApplicationContext context: Context
) {

    @AssistedFactory
    interface Factory {
        fun create(
            customUserAgent: String? = null,
            engine: HttpClientEngine = OkHttp.create(),
        ): AppHttpClient
    }

    /**
     * The user agent to use in requests
     */
    val userAgent = customUserAgent ?: Constants.USER_AGENT

    // CustomCertManager is Closeable, but HttpClient will live as long as the application is in memory,
    // so we don't need to close it
    private val certManager = CustomCertManager(context, appInForeground = MutableStateFlow(false))

    private val sslContext = SSLContext.getInstance("TLS")
    init {
        sslContext.init(null, arrayOf(certManager), null)
    }

    val httpClient = HttpClient(engine) {
        // Add user given user agent to all engines
        install(UserAgent) {
            agent = userAgent
        }

        @Suppress("UNCHECKED_CAST")
        if (engine is OkHttpEngine) (this as HttpClientConfig<OkHttpConfig>).engine {
            addNetworkInterceptor(BrotliInterceptor)
            config {
                followRedirects(false)
                connectTimeout(20, TimeUnit.SECONDS)
                readTimeout(60, TimeUnit.SECONDS)
                sslSocketFactory(sslContext.socketFactory, certManager)
                hostnameVerifier(certManager.HostnameVerifier(OkHostnameVerifier))
            }
        } else {
            followRedirects = false
        }
    }

}
