package at.bitfire.icsdroid

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit

object HttpClient {

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
            .addNetworkInterceptor(UserAgentInterceptor)
            .followRedirects(false)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()


    object UserAgentInterceptor : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request().newBuilder()
                    .header("User-Agent", Constants.USER_AGENT)
            return chain.proceed(request.build())
        }

    }

}