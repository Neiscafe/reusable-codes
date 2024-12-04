

import br.com.deltasul.app.delta360.domain.GenericError
import br.com.deltasul.app.delta360.domain.HttpError
import br.com.deltasul.app.delta360.domain.IsFailure
import br.com.deltasul.app.delta360.domain.IsSuccess
import br.com.deltasul.app.delta360.domain.ResultOf
import br.com.deltasul.app.delta360.domain.VendorCredentialsProvider
import com.squareup.moshi.Moshi
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okio.IOException
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

object OkHttpClient {
    fun retrofit(url: String) {
        Retrofit.Builder().client(client)
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .addCallAdapterFactory(ResultOfCallAdapterFactory()).baseUrl(url).build()
    }

    private var client = OkHttpClient.Builder().apply {
        /**
         * Antes de implementar, validar esses tempos
         */
        callTimeout(10, TimeUnit.SECONDS)
        readTimeout(10, TimeUnit.SECONDS)
        writeTimeout(10, TimeUnit.SECONDS)

        /**
         * Reutilizar conexões numa pool
         * Antes de implementar, fazer benchmarks
         */
//        connectionPool(ConnectionPool())
        addInterceptor(OAuthInterceptor())
        addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
    }.build()


    //Call<T> para ResultOf<T>
    private class ResultOfCallAdapterFactory : CallAdapter.Factory() {
        override fun get(type: Type, p1: Array<out Annotation>, p2: Retrofit): CallAdapter<*, *>? {
            val outerType = getRawType(type)
            return if (outerType == ResultOf::class.java) {
                val genericParameterizedType = getParameterUpperBound(0, type as ParameterizedType)
                val inner = getRawType(genericParameterizedType)
                ResultOfCallAdapter<Any>(inner)
            } else null
        }
    }

    private class ResultOfCallAdapter<T>(private val responseType: Type) :
        CallAdapter<T, ResultOf<Any>> {
        override fun responseType(): Type = responseType
        override fun adapt(call: Call<T>): ResultOf<Any> = try {
            IsSuccess(call.execute().body()!!)
        } catch (e: HttpException) {
            IsFailure(HttpError(e.code(), e.message()))
        } catch (e: Throwable) {
            IsFailure(GenericError(e))
        }
    }

    private class OAuthInterceptor : Interceptor {
        var accessToken: String? = null
        override fun intercept(chain: Interceptor.Chain): Response {
            val url = chain.request().url
            val includeTokenInRequest =
                accessToken != null && url.host == "hostdeltasul" && !url.encodedPath.contains("auth")
            return if (includeTokenInRequest) {
                val authedRequest = chain.addBearerToRequest(accessToken!!)
                var response = chain.proceed(authedRequest)
                if (response.code == 401) {
                    refreshOAuth()
                    val reauthedRequest = chain.addBearerToRequest(accessToken!!)
                    response = chain.proceed(reauthedRequest)
                }
                response
            } else chain.proceed(chain.request())
        }

        private fun Interceptor.Chain.addBearerToRequest(bearerToken: String) =
            request().newBuilder().header("Authorization", "Bearer $bearerToken").build()

        fun refreshOAuth() {
            with(VendorCredentialsProvider) {
                if (ip == null || username == null || password == null) throw IllegalArgumentException()
            }
            val request = Request.Builder().url("hostdeltasul/auth").post(
                FormBody.Builder().add("username", VendorCredentialsProvider.username!!)
                    .add("password", VendorCredentialsProvider.password!!).build()
            ).build()
            client.newCall(request).execute().use {
                if (!it.isSuccessful) throw IOException("Unexpected code $it")
                accessToken = "\"access_token\":\"([^\"]+)\"".toRegex()
                    .find(it.body.string())?.groupValues?.get(1)
            }
        }
    }
}
