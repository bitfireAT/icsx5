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
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.brotli.BrotliInterceptor
import okhttp3.internal.tls.OkHostnameVerifier
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext

class AppHttpClient @AssistedInject constructor(
    @Assisted customUserAgent: String?,
    @Assisted createEngine: (CustomCertManager, SSLContext) -> HttpClientEngine,
    @ApplicationContext context: Context
) {

    @AssistedFactory
    interface Factory {
        fun create(
            customUserAgent: String? = null,
            createEngine: (CustomCertManager, SSLContext) -> HttpClientEngine = { certManager, sslContext ->
                OkHttp.create {
                    addNetworkInterceptor(BrotliInterceptor)
                    addNetworkInterceptor(object : Interceptor {
                        override fun intercept(chain: Interceptor.Chain): Response {
                            val request = chain.request().newBuilder()
                                .header("User-Agent", customUserAgent ?: Constants.USER_AGENT)
                                .build()
                            return chain.proceed(request)
                        }
                    })
                    config {
                        followRedirects(true)
                        connectTimeout(20, TimeUnit.SECONDS)
                        readTimeout(60, TimeUnit.SECONDS)
                        sslSocketFactory(sslContext.socketFactory, certManager)
                        hostnameVerifier(certManager.HostnameVerifier(OkHostnameVerifier))
                    }
                }
            }
        ): AppHttpClient
    }

    // CustomCertManager is Closeable, but HttpClient will live as long as the application is in memory,
    // so we don't need to close it
    private val certManager = CustomCertManager(context, appInForeground = MutableStateFlow(false))

    private val sslContext = SSLContext.getInstance("TLS")
    init {
        sslContext.init(null, arrayOf(certManager), null)
    }

    val httpClient = HttpClient(createEngine(certManager, sslContext))

}