package com.nenah.cinetracker.data

data class PoiskKinoPageDto(
    val docs: List<PoiskKinoMovieDto> = emptyList()
)

data class PoiskKinoMovieDto(
    val id: Int = 0,
    val name: String? = null,
    val alternativeName: String? = null,
    val enName: String? = null,
    val type: String? = null,
    val year: Int? = null,
    val description: String? = null,
    val shortDescription: String? = null,
    val status: String? = null,
    val isSeries: Boolean? = null,
    val movieLength: Int? = null,
    val seriesLength: Int? = null,
    val totalSeriesLength: Int? = null,
    val poster: PoiskKinoImageDto? = null,
    val backdrop: PoiskKinoImageDto? = null,
    val rating: PoiskKinoRatingDto? = null,
    val genres: List<PoiskKinoNameDto> = emptyList(),
    val countries: List<PoiskKinoNameDto> = emptyList(),
    val seasonsInfo: List<PoiskKinoSeasonInfoDto> = emptyList(),
    val similarMovies: List<PoiskKinoLinkedMovieDto> = emptyList(),
    val sequelsAndPrequels: List<PoiskKinoLinkedMovieDto> = emptyList()
)

data class PoiskKinoLinkedMovieDto(
    val id: Int = 0,
    val name: String? = null,
    val alternativeName: String? = null,
    val enName: String? = null,
    val type: String? = null,
    val year: Int? = null,
    val poster: PoiskKinoImageDto? = null,
    val rating: PoiskKinoRatingDto? = null
)

data class PoiskKinoImageDto(
    val url: String? = null,
    val previewUrl: String? = null
)

data class PoiskKinoRatingDto(
    val kp: Double? = null,
    val imdb: Double? = null,
    val tmdb: Double? = null
)

data class PoiskKinoNameDto(
    val name: String? = null
)

data class PoiskKinoSeasonInfoDto(
    val number: Int? = null,
    val episodesCount: Int? = null
)

data class PoiskKinoTokenInfoDto(
    val requestsLimit: Int = 0,
    val requestsUsed: Int = 0,
    val requestsRemaining: Int = 0
)
