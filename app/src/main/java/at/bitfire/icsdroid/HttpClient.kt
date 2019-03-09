package at.bitfire.icsdroid

import android.content.Context
import at.bitfire.cert4android.CertTlsSocketFactory
import at.bitfire.cert4android.CustomCertManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.internal.tls.OkHostnameVerifier
import java.util.concurrent.TimeUnit

class HttpClient(
        context: Context,
        foreground: Boolean
) {

    private val certManager = CustomCertManager(context, appInForeground = foreground)

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
            .addNetworkInterceptor(UserAgentInterceptor)
            .followRedirects(false)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(CertTlsSocketFactory(null, certManager))
            .hostnameVerifier(certManager.hostnameVerifier(OkHostnameVerifier.INSTANCE))
            .build()


    object UserAgentInterceptor : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request().newBuilder()
                    .header("User-Agent", Constants.USER_AGENT)
            return chain.proceed(request.build())
        }

    }

}