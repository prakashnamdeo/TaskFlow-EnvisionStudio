package com.taskflow.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.taskflow.BuildConfig
import com.taskflow.data.remote.TaskApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .serializeNulls()
            .create()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val fallbackInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request()
            try {
                chain.proceed(request)
            } catch (e: java.io.IOException) {
                val originalHost = request.url.host
                if (originalHost != "127.0.0.1" && originalHost != "10.0.2.2") {
                    val fallbackUrl = request.url.newBuilder().host("127.0.0.1").build()
                    val fallbackRequest = request.newBuilder().url(fallbackUrl).build()
                    try {
                        chain.proceed(fallbackRequest)
                    } catch (_: java.io.IOException) {
                        throw e
                    }
                } else {
                    throw e
                }
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(fallbackInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        val baseUrl = resolveBaseUrl()
        android.util.Log.i("NetworkModule", ">>> Connecting to Backend Base URL: $baseUrl (isEmulator: ${isEmulator()})")
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * Resolves the appropriate backend base URL automatically:
     * - Physical Device on Wi-Fi -> BuildConfig.BASE_URL (http://192.168.0.110:3000/)
     * - Android Emulator -> http://10.0.2.2:3000/
     */
    private fun resolveBaseUrl(): String {
        return if (isEmulator()) {
            "http://10.0.2.2:3000/"
        } else {
            BuildConfig.BASE_URL
        }
    }

    private fun isEmulator(): Boolean {
        return (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.FINGERPRINT.contains("sdk_gphone")
                || android.os.Build.HARDWARE.contains("goldfish")
                || android.os.Build.HARDWARE.contains("ranchu")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MODEL.contains("sdk_gphone")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || android.os.Build.PRODUCT.contains("sdk_google")
                || android.os.Build.PRODUCT.contains("google_sdk")
                || android.os.Build.PRODUCT.contains("sdk")
                || android.os.Build.PRODUCT.contains("sdk_x86")
                || android.os.Build.PRODUCT.contains("sdk_gphone")
                || android.os.Build.PRODUCT.contains("vbox86p")
                || android.os.Build.PRODUCT.contains("emulator")
                || android.os.Build.PRODUCT.contains("simulator")
                || android.os.Build.BOARD.contains("goldfish")
                || android.os.Build.BOARD.contains("ranchu")
                || java.io.File("/dev/socket/qemud").exists()
                || java.io.File("/dev/qemu_pipe").exists()
    }

    @Provides
    @Singleton
    fun provideTaskApiService(retrofit: Retrofit): TaskApiService {
        return retrofit.create(TaskApiService::class.java)
    }
}
