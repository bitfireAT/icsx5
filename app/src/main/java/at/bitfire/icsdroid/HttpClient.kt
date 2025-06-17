/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.content.Context
import at.bitfire.cert4android.CustomCertManager
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.brotli.BrotliInterceptor
import okhttp3.internal.tls.OkHostnameVerifier
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext

class HttpClient private constructor(
    context: Context
) {

    companion object {
        private var INSTANCE: HttpClient? = null

        private val appInForeground = MutableStateFlow(false)

        @Synchronized
        fun get(context: Context): HttpClient {
            INSTANCE?.let { return it }
            return HttpClient(context.applicationContext)
                .also { INSTANCE = it }
        }

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

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addNetworkInterceptor(BrotliInterceptor)
        .addNetworkInterceptor(UserAgentInterceptor)
        .followRedirects(false)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .sslSocketFactory(sslContext.socketFactory, certManager)
        .hostnameVerifier(certManager.HostnameVerifier(OkHostnameVerifier))
        .build()


    object UserAgentInterceptor : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
                .newBuilder()
                .header("User-Agent", Constants.USER_AGENT)
            return chain.proceed(request.build())
        }

    }

}
