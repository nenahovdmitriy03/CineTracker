package com.nenah.cinetracker.data

import android.content.Context
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.File
import java.util.concurrent.TimeUnit

interface PoiskKinoService {
    @GET("v1.5/token")
    suspend fun tokenInfo(): PoiskKinoTokenInfoDto

    @GET("v1.5/movie")
    suspend fun movies(
        @Query("limit") limit: Int = 12,
        @Query("page") page: Int = 1,
        @Query("type") type: List<String>? = null,
        @Query("year") year: String? = null,
        @Query("rating.kp") ratingKp: String? = null,
        @Query("votes.kp") votesKp: String? = null,
        @Query("notNullFields") notNullFields: List<String> = listOf("name", "poster.url"),
        @Query("sortField") sortField: List<String> = listOf("votes.kp"),
        @Query("sortType") sortType: List<String> = listOf("-1")
    ): PoiskKinoPageDto

    @GET("v1.4/movie/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("limit") limit: Int = 20,
        @Query("page") page: Int = 1
    ): PoiskKinoPageDto

    @GET("v1.4/movie/{id}")
    suspend fun details(
        @Path("id") id: Int
    ): PoiskKinoMovieDto
}

object PoiskKinoNetwork {
    fun create(apiKey: String, context: Context, baseUrl: String): PoiskKinoService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("X-API-KEY")
        }
        val cache = Cache(File(context.cacheDir, "poiskkino_http_cache"), 40L * 1024L * 1024L)
        val client = OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor { chain ->
                val cacheControl = CacheControl.Builder()
                    .maxAge(12, TimeUnit.HOURS)
                    .build()
                val builder = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .header("User-Agent", "CineTracker Android debug")
                    .cacheControl(cacheControl)

                if (apiKey.isNotBlank()) {
                    builder.header("X-API-KEY", apiKey)
                }

                chain.proceed(builder.build())
            }
            .addNetworkInterceptor { chain ->
                val requestPath = chain.request().url.encodedPath
                val maxAge = when {
                    requestPath.endsWith("/token") -> 60
                    requestPath.contains("/movie/search") -> 6 * 60 * 60
                    requestPath.contains("/movie/") -> 7 * 24 * 60 * 60
                    else -> 12 * 60 * 60
                }

                chain.proceed(chain.request()).newBuilder()
                    .header("Cache-Control", "public, max-age=$maxAge")
                    .build()
            }
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl.ensureTrailingSlash())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PoiskKinoService::class.java)
    }
}

private fun String.ensureTrailingSlash(): String = if (endsWith("/")) this else "$this/"
