package com.nenah.cinetracker.data

import android.content.Context
import android.util.Log
import com.nenah.cinetracker.BuildConfig
import okhttp3.Cache
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.net.InetAddress
import java.net.UnknownHostException

interface TmdbService {
    @GET("trending/all/week")
    suspend fun trending(
        @Query("language") language: String = "ru-RU"
    ): TmdbPageDto

    @GET("movie/popular")
    suspend fun popularMovies(
        @Query("language") language: String = "ru-RU",
        @Query("region") region: String = "RU"
    ): TmdbPageDto

    @GET("tv/popular")
    suspend fun popularShows(
        @Query("language") language: String = "ru-RU"
    ): TmdbPageDto

    @GET("movie/now_playing")
    suspend fun nowPlayingMovies(
        @Query("language") language: String = "ru-RU",
        @Query("region") region: String = "RU"
    ): TmdbPageDto

    @GET("tv/on_the_air")
    suspend fun onTheAirShows(
        @Query("language") language: String = "ru-RU"
    ): TmdbPageDto

    @GET("discover/tv")
    suspend fun animeShows(
        @Query("language") language: String = "ru-RU",
        @Query("with_genres") withGenres: String = "16",
        @Query("with_original_language") originalLanguage: String = "ja",
        @Query("sort_by") sortBy: String = "popularity.desc"
    ): TmdbPageDto

    @GET("discover/movie")
    suspend fun animeMovies(
        @Query("language") language: String = "ru-RU",
        @Query("with_genres") withGenres: String = "16",
        @Query("with_original_language") originalLanguage: String = "ja",
        @Query("sort_by") sortBy: String = "popularity.desc"
    ): TmdbPageDto

    @GET("search/multi")
    suspend fun search(
        @Query("query") query: String,
        @Query("language") language: String = "ru-RU",
        @Query("include_adult") includeAdult: Boolean = false
    ): TmdbPageDto

    @GET("movie/{id}")
    suspend fun movieDetails(
        @Path("id") id: Int,
        @Query("language") language: String = "ru-RU"
    ): TmdbDetailDto

    @GET("collection/{id}")
    suspend fun collectionDetails(
        @Path("id") id: Int,
        @Query("language") language: String = "ru-RU"
    ): TmdbCollectionDto

    @GET("movie/{id}/recommendations")
    suspend fun movieRecommendations(
        @Path("id") id: Int,
        @Query("language") language: String = "ru-RU"
    ): TmdbPageDto

    @GET("tv/{id}")
    suspend fun tvDetails(
        @Path("id") id: Int,
        @Query("language") language: String = "ru-RU"
    ): TmdbDetailDto

    @GET("tv/{id}/season/{seasonNumber}")
    suspend fun tvSeasonDetails(
        @Path("id") id: Int,
        @Path("seasonNumber") seasonNumber: Int,
        @Query("language") language: String = "ru-RU"
    ): TmdbSeasonDetailDto

    @GET("tv/{id}/recommendations")
    suspend fun tvRecommendations(
        @Path("id") id: Int,
        @Query("language") language: String = "ru-RU"
    ): TmdbPageDto
}

object TmdbNetwork {
    fun create(readAccessToken: String, baseUrl: String): TmdbService {
        val normalizedBaseUrl = baseUrl.normalizedTmdbApiBaseUrl(TMDB_API_HOST)
        val useSystemDns = !normalizedBaseUrl.isOfficialTmdbHost(TMDB_API_HOST)
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = createBaseClient(useSystemDns = useSystemDns)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                    .addHeader("accept", "application/json")
                if (readAccessToken.isNotBlank()) {
                    requestBuilder.addHeader("Authorization", "Bearer $readAccessToken")
                }
                val request = requestBuilder.build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbService::class.java)
    }

    fun createImageClient(context: Context): OkHttpClient {
        val useSystemDns = !BuildConfig.TMDB_IMAGE_BASE_URL.isOfficialTmdbHost(TMDB_IMAGE_HOST)
        return createBaseClient(useSystemDns = useSystemDns)
            .cache(Cache(context.cacheDir.resolve("tmdb_image_http_cache"), 80L * 1024L * 1024L))
            .build()
    }

    private fun createBaseClient(useSystemDns: Boolean = false): OkHttpClient.Builder {
        val builder = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .addHeader("User-Agent", "CineTracker Android debug")
                        .build()
                )
            }

        if (useSystemDns) {
            return builder
        }

        val bootstrapClient = OkHttpClient.Builder().build()
        val dnsProviders = listOf(
            DohProvider(
                name = "Google",
                dns = createDoh(
                    client = bootstrapClient,
                    url = "https://dns.google/dns-query",
                    bootstrapHosts = listOf("8.8.8.8", "8.8.4.4")
                )
            ),
            DohProvider(
                name = "Cloudflare",
                dns = createDoh(
                    client = bootstrapClient,
                    url = "https://cloudflare-dns.com/dns-query",
                    bootstrapHosts = listOf("1.1.1.1", "1.0.0.1")
                )
            ),
            DohProvider(
                name = "Quad9",
                dns = createDoh(
                    client = bootstrapClient,
                    url = "https://dns.quad9.net/dns-query",
                    bootstrapHosts = listOf("9.9.9.9", "149.112.112.112")
                )
            )
        )

        val resilientDns = Dns { hostname ->
            var lastError: Throwable? = null
            for (provider in dnsProviders) {
                val addresses = runCatching {
                    provider.dns.lookup(hostname).filterNot { it.isBlockedAddress() }
                }.onFailure { lastError = it }.getOrDefault(emptyList())

                Log.d(
                    "TmdbNetwork",
                    "DNS ${provider.name} $hostname -> ${addresses.joinToString { it.hostAddress.orEmpty() }}"
                )

                if (addresses.isNotEmpty()) {
                    return@Dns addresses
                }
            }

            val systemAddresses = runCatching {
                Dns.SYSTEM.lookup(hostname).filterNot { it.isBlockedAddress() }
            }.onFailure { lastError = it }.getOrDefault(emptyList())

            Log.d(
                "TmdbNetwork",
                "DNS System $hostname -> ${systemAddresses.joinToString { it.hostAddress.orEmpty() }}"
            )

            if (systemAddresses.isNotEmpty()) {
                return@Dns systemAddresses
            }

            throw UnknownHostException("No public DNS result for $hostname").apply {
                if (lastError != null) initCause(lastError)
            }
        }

        return builder.dns(resilientDns)
    }

    private fun createDoh(
        client: OkHttpClient,
        url: String,
        bootstrapHosts: List<String>
    ): DnsOverHttps {
        return DnsOverHttps.Builder()
            .client(client)
            .url(url.toHttpUrl())
            .bootstrapDnsHosts(bootstrapHosts.map(InetAddress::getByName))
            .includeIPv6(false)
            .build()
    }

    private data class DohProvider(
        val name: String,
        val dns: Dns
    )

    private fun InetAddress.isBlockedAddress(): Boolean {
        return isAnyLocalAddress || isLoopbackAddress || isLinkLocalAddress || isSiteLocalAddress
    }

    private const val TMDB_API_HOST = "api.themoviedb.org"
    private const val TMDB_IMAGE_HOST = "image.tmdb.org"
}

private fun String.ensureTrailingSlash(): String = if (endsWith("/")) this else "$this/"

private fun String.normalizedTmdbApiBaseUrl(officialHost: String): String {
    val base = ensureTrailingSlash()
    if (base.isOfficialTmdbHost(officialHost)) return base

    val url = runCatching { base.toHttpUrl() }.getOrNull() ?: return base
    val pathSegments = url.pathSegments.filter { it.isNotBlank() }
    if (pathSegments.lastOrNull() == "3") return base

    return url.newBuilder()
        .addPathSegment("3")
        .build()
        .toString()
        .ensureTrailingSlash()
}

private fun String.isOfficialTmdbHost(host: String): Boolean {
    return runCatching { ensureTrailingSlash().toHttpUrl().host == host }.getOrDefault(false)
}
