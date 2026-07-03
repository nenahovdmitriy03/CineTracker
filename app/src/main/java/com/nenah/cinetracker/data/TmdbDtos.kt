package com.nenah.cinetracker.data

import com.google.gson.annotations.SerializedName

data class TmdbPageDto(
    val results: List<TmdbMediaDto> = emptyList()
)

data class TmdbMediaDto(
    val id: Int = 0,
    @SerializedName("media_type") val mediaType: String? = null,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    @SerializedName("vote_average") val voteAverage: Double? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("first_air_date") val firstAirDate: String? = null
)

data class TmdbDetailDto(
    val id: Int = 0,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    @SerializedName("vote_average") val voteAverage: Double? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
    val runtime: Int? = null,
    @SerializedName("episode_run_time") val episodeRunTime: List<Int>? = null,
    val genres: List<TmdbGenreDto> = emptyList(),
    val status: String? = null,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int? = null,
    val seasons: List<TmdbSeasonDto> = emptyList(),
    @SerializedName("belongs_to_collection") val belongsToCollection: TmdbCollectionSummaryDto? = null
)

data class TmdbGenreDto(
    val id: Int = 0,
    val name: String = ""
)

data class TmdbSeasonDto(
    @SerializedName("season_number") val seasonNumber: Int = 0,
    val name: String? = null,
    @SerializedName("episode_count") val episodeCount: Int = 0
)

data class TmdbSeasonDetailDto(
    @SerializedName("season_number") val seasonNumber: Int = 0,
    val name: String? = null,
    val episodes: List<TmdbEpisodeDto> = emptyList()
)

data class TmdbEpisodeDto(
    @SerializedName("season_number") val seasonNumber: Int = 0,
    @SerializedName("episode_number") val episodeNumber: Int = 0,
    val name: String? = null,
    val overview: String? = null,
    @SerializedName("still_path") val stillPath: String? = null,
    @SerializedName("vote_average") val voteAverage: Double? = null,
    @SerializedName("air_date") val airDate: String? = null,
    val runtime: Int? = null
)

data class TmdbCollectionSummaryDto(
    val id: Int = 0,
    val name: String? = null
)

data class TmdbCollectionDto(
    val id: Int = 0,
    val name: String? = null,
    val parts: List<TmdbMediaDto> = emptyList()
)
