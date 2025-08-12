/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid

import android.content.Context
import at.bitfire.cert4android.CustomCertManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.engine.okhttp.OkHttpEngine
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.brotli.BrotliInterceptor
import okhttp3.internal.tls.OkHostnameVerifier
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.ssl.SSLContext

class AppHttpClient(
    context: Context,
    engine: HttpClientEngine = OkHttp.create()
) {

    companion object {
        private val appInForeground = MutableStateFlow(false)

        fun setForeground(foreground: Boolean) {
            appInForeground.tryEmit(foreground)
        }
    }

    // CustomCertManager is Closeable, but HttpClient will live as long as the application is in memory,
    // so we don't need to close it
    private val certManager = CustomCertManager(context, appInForeground = appInForeground)

    private val sslContext = SSLContext.getInstance("TLS")
    init {
        sslContext.init(null, arrayOf(certManager), null)
    }

    val httpClient = HttpClient(engine) {
        @Suppress("UNCHECKED_CAST")
        if (engine is OkHttpEngine) (this as HttpClientConfig<OkHttpConfig>).engine {
            addNetworkInterceptor(BrotliInterceptor)
            addNetworkInterceptor(UserAgentInterceptor)
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

    object UserAgentInterceptor : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
                .newBuilder()
                .header("User-Agent", Constants.USER_AGENT)
            return chain.proceed(request.build())
        }

    }

    @Module
    @InstallIn(SingletonComponent::class)
    object HttpClientModule {
        @Singleton
        @Provides
        fun provideAppHttpClient(
            @ApplicationContext context: Context
        ): AppHttpClient {
            return AppHttpClient(context, OkHttp.create())
        }
    }

}
