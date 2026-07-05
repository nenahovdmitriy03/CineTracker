package com.nenah.cinetracker

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.palette.graphics.Palette
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.LocalImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.nenah.cinetracker.data.TmdbNetwork
import com.nenah.cinetracker.model.ApiQuota
import com.nenah.cinetracker.model.CineAppTheme
import com.nenah.cinetracker.model.ContinueWatchingItem
import com.nenah.cinetracker.model.EpisodeInfo
import com.nenah.cinetracker.model.HomeFeed
import com.nenah.cinetracker.model.MediaCollection
import com.nenah.cinetracker.model.MediaDetail
import com.nenah.cinetracker.model.MediaItem
import com.nenah.cinetracker.model.MediaKind
import com.nenah.cinetracker.model.MediaRatings
import com.nenah.cinetracker.model.SeasonInfo
import com.nenah.cinetracker.model.TrackStatus
import com.nenah.cinetracker.model.TrackedTitle
import com.nenah.cinetracker.model.TrackerEvent
import com.nenah.cinetracker.model.TrackerStats
import com.nenah.cinetracker.ui.AiChatMessage
import com.nenah.cinetracker.ui.CineUiState
import com.nenah.cinetracker.ui.CineViewModel
import com.nenah.cinetracker.ui.theme.CineTrackerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CineTrackerTheme(dynamicColor = false) {
                CineTrackerApp()
            }
        }
    }
}

@Composable
fun CineTrackerApp(viewModel: CineViewModel = viewModel()) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .okHttpClient { TmdbNetwork.createImageClient(context) }
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("tmdb_coil_cache"))
                    .maxSizePercent(0.08)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = remember(uiState.appTheme) { paletteFor(uiState.appTheme) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route.orEmpty()
    val showBottomBar = currentRoute in CineTab.entries.map { it.route }

    CompositionLocalProvider(
        LocalImageLoader provides imageLoader,
        LocalCinePalette provides palette
    ) {
        Scaffold(
            containerColor = CineColors.Background,
            contentWindowInsets = WindowInsets(0.dp),
            bottomBar = {
                if (showBottomBar) {
                    CineNavigationBar(
                        currentRoute = currentRoute.ifBlank { CineTab.Home.route },
                        onTabSelected = { tab ->
                            navController.navigate(tab.route) {
                                popUpTo(CineTab.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            CineNavHost(
                navController = navController,
                uiState = uiState,
                onOpenItem = { item ->
                    viewModel.rememberDetailItem(item)
                    navController.navigate(item.detailRoute())
                },
                onBack = { navController.popBackStack() },
                onSearchQueryChanged = viewModel::updateSearchQuery,
                onManualLinkChanged = viewModel::updateManualLink,
                onAddManualLink = viewModel::addManualLinkToPlan,
                onAiChatInputChanged = viewModel::updateAiChatInput,
                onSendAiChatMessage = viewModel::sendAiChatMessage,
                onOpenDetail = viewModel::openDetail,
                onRefreshHome = viewModel::refreshHome,
                onSetStatus = viewModel::setSelectedStatus,
                onSetRating = viewModel::setSelectedRating,
                onSetEpisodeRating = viewModel::setEpisodeRating,
                onLoadSeasonDetails = viewModel::loadSeasonDetails,
                onRemoveFromTracker = viewModel::removeSelectedFromTracker,
                onRefreshQuota = viewModel::refreshApiQuota,
                onRollRoulette = viewModel::rollRoulette,
                onClearRoulette = viewModel::clearRoulette,
                onCreateCollection = viewModel::createCollection,
                onCreateCollectionForSelected = viewModel::createCollectionForSelected,
                onToggleSelectedCollection = viewModel::toggleSelectedCollection,
                onLoadCollectionItems = viewModel::loadCollectionItems,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun CineNavHost(
    navController: NavHostController,
    uiState: CineUiState,
    onOpenItem: (MediaItem) -> Unit,
    onBack: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onManualLinkChanged: (String) -> Unit,
    onAddManualLink: () -> Unit,
    onAiChatInputChanged: (String) -> Unit,
    onSendAiChatMessage: () -> Unit,
    onOpenDetail: (MediaKind, Int) -> Unit,
    onRefreshHome: () -> Unit,
    onSetStatus: (TrackStatus) -> Unit,
    onSetRating: (Int) -> Unit,
    onSetEpisodeRating: (Int, Int, Int, Int?) -> Unit,
    onLoadSeasonDetails: (Int) -> Unit,
    onRemoveFromTracker: () -> Unit,
    onRefreshQuota: () -> Unit,
    onRollRoulette: () -> Unit,
    onClearRoulette: () -> Unit,
    onCreateCollection: (String) -> Unit,
    onCreateCollectionForSelected: (String) -> Unit,
    onToggleSelectedCollection: (Long) -> Unit,
    onLoadCollectionItems: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = CineTab.Home.route,
        modifier = modifier
    ) {
        composable(CineTab.Home.route) {
            HomeScreen(
                uiState = uiState,
                onOpenItem = onOpenItem,
                onOpenSection = { section -> navController.navigate("section/${section.key}") },
                onRefresh = onRefreshHome
            )
        }
        composable(
            route = "section/{sectionKey}",
            arguments = listOf(navArgument("sectionKey") { type = NavType.StringType })
        ) { entry ->
            val sectionKey = entry.arguments?.getString("sectionKey").orEmpty()
            SectionScreen(
                section = uiState.homeFeed?.sectionByKey(sectionKey),
                trackedTitles = uiState.trackedTitles,
                onBack = onBack,
                onOpenItem = onOpenItem
            )
        }
        composable(CineTab.Search.route) {
            SearchScreen(
                uiState = uiState,
                onQueryChanged = onSearchQueryChanged,
                onManualLinkChanged = onManualLinkChanged,
                onAddManualLink = onAddManualLink,
                onOpenAiChat = {
                    navController.navigate(AiChatRoute) {
                        launchSingleTop = true
                    }
                },
                onOpenItem = onOpenItem
            )
        }
        composable(AiChatRoute) {
            AiChatScreen(
                uiState = uiState,
                onBack = onBack,
                onInputChanged = onAiChatInputChanged,
                onSend = onSendAiChatMessage
            )
        }
        composable(CineTab.Library.route) {
            LibraryScreen(
                viewModel = viewModel(),
                onCreateCollection = onCreateCollection,
                onOpenCollection = { id, name -> navController.navigate("collection/$id/$name") },
                onOpenItem = onOpenItem
            )
        }
        composable(
            route = "collection/{id}/{name}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { entry ->
            val collectionId = entry.arguments?.getLong("id") ?: 0L
            val collectionName = entry.arguments?.getString("name").orEmpty()
            LaunchedEffect(collectionId) {
                onLoadCollectionItems(collectionId)
            }
            CollectionScreen(
                name = collectionName,
                items = uiState.currentCollectionItems,
                onBack = onBack,
                onOpenItem = onOpenItem
            )
        }
        composable(CineTab.Profile.route) {
            ProfileScreen(
                isTmdbConfigured = uiState.isTmdbConfigured,
                stats = uiState.trackerStats
            )
        }
        composable(
            route = "detail/{kind}/{id}",
            arguments = listOf(
                navArgument("kind") { type = NavType.StringType },
                navArgument("id") { type = NavType.IntType }
            )
        ) { entry ->
            val kind = MediaKind.fromRoute(entry.arguments?.getString("kind").orEmpty())
            val id = entry.arguments?.getInt("id") ?: 0
            LaunchedEffect(kind, id) {
                onOpenDetail(kind, id)
            }
            DetailScreen(
                detail = uiState.selectedDetail,
                isLoading = uiState.isDetailLoading,
                currentStatus = uiState.selectedTrackedTitle?.status,
                personalRating = uiState.selectedTrackedTitle?.personalRating,
                episodeRatings = uiState.episodeRatings,
                collections = uiState.collections,
                selectedCollectionIds = uiState.selectedCollectionIds,
                trackedTitles = uiState.trackedTitles,
                onBack = onBack,
                onOpenItem = onOpenItem,
                onSetStatus = onSetStatus,
                onSetRating = onSetRating,
                onSetEpisodeRating = onSetEpisodeRating,
                onLoadSeasonDetails = onLoadSeasonDetails,
                onCreateCollection = onCreateCollectionForSelected,
                onToggleCollection = onToggleSelectedCollection,
                onRemoveFromTracker = onRemoveFromTracker
            )
        }
    }
}

@Composable
private fun HomeScreen(
    uiState: CineUiState,
    onOpenItem: (MediaItem) -> Unit,
    onOpenSection: (HomeSection) -> Unit,
    onRefresh: () -> Unit
) {
    val feed = uiState.homeFeed
    var selectedFilter by rememberSaveable { mutableStateOf(HomeFilter.All) }
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp

    Box(modifier = Modifier.fillMaxSize().background(CineColors.Background)) {
        // Фоновый градиент для глубины
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(CineColors.Gold.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(200f, -100f),
                        radius = 800f
                    )
                )
        )

        if (uiState.isHomeLoading || feed == null) {
            HomeSkeleton()
            return@Box
        }

        val hero = feed.heroFor(selectedFilter)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = topPadding, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Header(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it }
                )
            }
            feed.message?.let { message ->
                item { InfoBanner(message) }
            }
            if (!feed.hasContent) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        EmptyLibraryCard(
                            title = "Каталог не загрузился",
                            subtitle = "Проверь сеть или VPN и попробуй ещё раз."
                        )
                        Button(
                            onClick = onRefresh,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CineColors.Gold,
                                contentColor = CineColors.OnGold
                            )
                        ) {
                            Text("Обновить", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                if (hero != null) {
                    item {
                        FeaturedTitleCard(
                            item = hero,
                            onClick = { onOpenItem(hero) }
                        )
                    }
                }
                val sections = feed.sectionsFor(selectedFilter)
                if (sections.isEmpty()) {
                    item {
                        EmptyLibraryCard(
                            title = "Пока пусто",
                            subtitle = "TMDb не вернул элементы для выбранного фильтра."
                        )
                    }
                }
                sections.forEach { section ->
                    item {
                        MediaRail(
                            title = section.title,
                            action = "Все",
                            items = section.items,
                            trackedTitles = uiState.trackedTitles,
                            onActionClick = { onOpenSection(section) },
                            onOpenItem = onOpenItem
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionScreen(
    section: HomeSection?,
    trackedTitles: List<TrackedTitle>,
    onBack: () -> Unit,
    onOpenItem: (MediaItem) -> Unit
) {
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 14.dp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CineColors.Background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = topPadding, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(CineColors.Card.copy(alpha = 0.92f))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Назад",
                            tint = CineColors.PrimaryText
                        )
                    }
                    Text(
                        text = section?.title ?: "Раздел",
                        color = CineColors.PrimaryText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (section == null || section.items.isEmpty()) {
                item {
                    EmptyLibraryCard(
                        title = "Раздел пуст",
                        subtitle = "Пока здесь нет фильмов или сериалов."
                    )
                }
            } else {
                items(section.items, key = { it.mediaKey() }) { item ->
                    val personalRating = trackedTitles.find { it.item.id == item.id && it.item.kind == item.kind }?.personalRating
                    SectionListCard(
                        item = item,
                        personalRating = personalRating,
                        onClick = { onOpenItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSkeleton() {
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 20.dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 20.dp, top = topPadding, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(Modifier.height(40.dp))
        Box(Modifier.fillMaxWidth().height(240.dp).shimmerEffect())
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1.2f).height(160.dp).shimmerEffect())
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.fillMaxWidth().height(74.dp).shimmerEffect())
                Box(Modifier.fillMaxWidth().height(74.dp).shimmerEffect())
            }
        }
        Box(Modifier.width(150.dp).height(24.dp).shimmerEffect())
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(3) { Box(Modifier.width(148.dp).height(214.dp).shimmerEffect()) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreen(
    uiState: CineUiState,
    onQueryChanged: (String) -> Unit,
    onManualLinkChanged: (String) -> Unit,
    onAddManualLink: () -> Unit,
    onOpenAiChat: () -> Unit,
    onOpenItem: (MediaItem) -> Unit
) {
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 20.dp
    val manualLinkSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showManualLinkSheet by rememberSaveable { mutableStateOf(false) }

    if (showManualLinkSheet) {
        ModalBottomSheet(
            onDismissRequest = { showManualLinkSheet = false },
            sheetState = manualLinkSheetState,
            containerColor = CineColors.Background,
            contentColor = CineColors.PrimaryText
        ) {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ManualLinkCard(
                    link = uiState.manualLink,
                    isLoading = uiState.isManualAddLoading,
                    message = uiState.manualAddMessage,
                    onLinkChanged = onManualLinkChanged,
                    onAdd = onAddManualLink
                )
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CineColors.Background),
        contentPadding = PaddingValues(start = 20.dp, top = topPadding, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            SearchHeader(
                onOpenAiChat = onOpenAiChat,
                onOpenManualLink = { showManualLinkSheet = true }
            )
        }
        item {
            TextField(
                value = uiState.searchQuery,
                onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        tint = CineColors.MutedText
                    )
                },
                placeholder = { Text("Название фильма или сериала") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CineColors.Card,
                    unfocusedContainerColor = CineColors.Card,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = CineColors.PrimaryText,
                    unfocusedTextColor = CineColors.PrimaryText,
                    focusedPlaceholderColor = CineColors.MutedText,
                    unfocusedPlaceholderColor = CineColors.MutedText
                )
            )
        }
        if (uiState.isSearchLoading) {
            item { LoadingInline() }
        }
        items(uiState.searchResults) { item ->
            SearchResultCard(item = item, onClick = { onOpenItem(item) })
        }
    }
}

@Composable
private fun AiChatScreen(
    uiState: CineUiState,
    onBack: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 14.dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CineColors.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = topPadding, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Назад",
                    tint = CineColors.PrimaryText
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI-чат",
                    color = CineColors.PrimaryText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Подбор фильмов и сериалов",
                    color = CineColors.MutedText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            AiBadge()
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.aiChatMessages.isEmpty()) {
                item {
                    AiChatEmptyState(onSuggestionClick = onInputChanged)
                }
            }
            items(uiState.aiChatMessages) { message ->
                AiChatBubble(message = message)
            }
            if (uiState.isAiChatLoading) {
                item {
                    AiChatLoadingBubble()
                }
            }
        }

        AiChatInputBar(
            input = uiState.aiChatInput,
            isLoading = uiState.isAiChatLoading,
            onInputChanged = onInputChanged,
            onSend = onSend
        )
    }
}

@Composable
private fun AiChatEmptyState(onSuggestionClick: (String) -> Unit) {
    val suggestions = listOf(
        "Посоветуй фильм на вечер",
        "Хочу сериал на выходные",
        "Что посмотреть из фантастики?"
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CineColors.Card,
        border = BorderStroke(1.dp, CineColors.Gold.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Что посмотрим?",
                color = CineColors.PrimaryText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "AI учитывает твой список, оценки и текущий каталог.",
                color = CineColors.MutedText,
                style = MaterialTheme.typography.bodyMedium
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(suggestions) { suggestion ->
                    Surface(
                        onClick = { onSuggestionClick(suggestion) },
                        shape = RoundedCornerShape(18.dp),
                        color = CineColors.Background.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, CineColors.Stroke)
                    ) {
                        Text(
                            text = suggestion,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            color = CineColors.PrimaryText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiChatBubble(message: AiChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(if (message.isUser) 0.82f else 0.9f),
            shape = RoundedCornerShape(
                topStart = 22.dp,
                topEnd = 22.dp,
                bottomStart = if (message.isUser) 22.dp else 6.dp,
                bottomEnd = if (message.isUser) 6.dp else 22.dp
            ),
            color = if (message.isUser) CineColors.Gold else CineColors.Card,
            border = if (message.isUser) null else BorderStroke(1.dp, CineColors.Stroke)
        ) {
            if (message.isUser) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                    color = CineColors.OnGold,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            } else {
                AiFormattedText(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp)
                )
            }
        }
    }
}

@Composable
private fun AiFormattedText(
    text: String,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val cleanedText = remember(text) { text.cleanAiResponseText() }
    val urlRegex = remember { Regex("""https?://[^\s)]+""") }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        cleanedText.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isBlank()) {
                Spacer(Modifier.height(4.dp))
            } else {
                val urls = urlRegex.findAll(line).map { it.value.trimEnd('.', ',') }.toList()
                val textWithoutUrls = urls.fold(line) { current, url -> current.replace(url, "") }
                    .replace("  ", " ")
                    .trim()

                if (textWithoutUrls.isNotBlank()) {
                    val isTitleLine = line.matches(Regex("""^\d+\..+""")) || line.endsWith(":")
                    Text(
                        text = textWithoutUrls,
                        color = if (isTitleLine) CineColors.PrimaryText else CineColors.SoftText,
                        style = if (isTitleLine) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isTitleLine) FontWeight.ExtraBold else FontWeight.Normal,
                        lineHeight = 20.sp
                    )
                }

                urls.forEach { url ->
                    Surface(
                        onClick = { uriHandler.openUri(url) },
                        shape = RoundedCornerShape(14.dp),
                        color = CineColors.Gold.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, CineColors.Gold.copy(alpha = 0.34f))
                    ) {
                        Text(
                            text = url.linkLabel(),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = CineColors.Gold,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun String.cleanAiResponseText(): String {
    return lines()
        .joinToString("\n") { line ->
            line.trim()
                .replace("**", "")
                .replace(Regex("""^\*\s+"""), "• ")
                .replace(Regex("""^-\s+"""), "• ")
                .replace("Причина:", "Почему:")
        }
        .trim()
}

private fun String.linkLabel(): String {
    return when {
        contains("kinopoisk", ignoreCase = true) -> "Открыть в Кинопоиске"
        contains("themoviedb", ignoreCase = true) || contains("tmdb", ignoreCase = true) -> "Открыть в TMDb"
        else -> "Открыть ссылку"
    }
}

@Composable
private fun AiChatLoadingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = CineColors.Card,
            border = BorderStroke(1.dp, CineColors.Stroke)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = CineColors.Gold
                )
                Text(
                    text = "AI думает...",
                    color = CineColors.MutedText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun AiChatInputBar(
    input: String,
    isLoading: Boolean,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CineColors.Background,
        border = BorderStroke(1.dp, CineColors.Stroke)
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                top = 12.dp,
                end = 16.dp,
                bottom = 16.dp + bottomPadding
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextField(
                value = input,
                onValueChange = onInputChanged,
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
                maxLines = 4,
                shape = RoundedCornerShape(20.dp),
                placeholder = { Text("Спроси, что посмотреть") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CineColors.Card,
                    unfocusedContainerColor = CineColors.Card,
                    disabledContainerColor = CineColors.Card,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = CineColors.PrimaryText,
                    unfocusedTextColor = CineColors.PrimaryText,
                    disabledTextColor = CineColors.MutedText,
                    focusedPlaceholderColor = CineColors.MutedText,
                    unfocusedPlaceholderColor = CineColors.MutedText
                )
            )
            Button(
                onClick = onSend,
                enabled = input.isNotBlank() && !isLoading,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CineColors.Gold,
                    contentColor = CineColors.OnGold,
                    disabledContainerColor = CineColors.Card,
                    disabledContentColor = CineColors.MutedText
                )
            ) {
                Text("Отправить", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SearchHeader(
    onOpenAiChat: () -> Unit,
    onOpenManualLink: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Поиск",
            color = CineColors.PrimaryText,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        AiBadge(onClick = onOpenAiChat)
        KinopoiskBadge(onClick = onOpenManualLink)
    }
}

@Composable
private fun KinopoiskBadge(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFF8A00),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "КП",
                color = Color(0xFF111111),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun AiBadge(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = if (onClick == null) modifier else modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = CineColors.Gold,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(CineColors.OnGold)
            )
            Text(
                text = "AI",
                color = CineColors.OnGold,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun AiRecommendationCard(
    recommendations: List<MediaItem>,
    onOpenItem: (MediaItem) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CineColors.Card,
        border = BorderStroke(1.dp, CineColors.Gold.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "AI-подборка",
                        color = CineColors.PrimaryText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "По твоему списку, оценкам и текущему каталогу",
                        color = CineColors.MutedText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                AiBadge()
            }

            if (recommendations.isEmpty()) {
                Text(
                    text = "Добавь несколько фильмов в список или дождись загрузки каталога.",
                    modifier = Modifier.padding(horizontal = 18.dp),
                    color = CineColors.MutedText,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp)
                ) {
                    items(recommendations, key = { it.mediaKey() }) { item ->
                        PosterCard(
                            item = item,
                            onClick = { onOpenItem(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualLinkCard(
    link: String,
    isLoading: Boolean,
    message: String?,
    onLinkChanged: (String) -> Unit,
    onAdd: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CineColors.Card,
        border = BorderStroke(1.dp, CineColors.Stroke)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_bookmark),
                    contentDescription = null,
                    tint = CineColors.Gold,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Добавить по ссылке",
                    color = CineColors.PrimaryText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            TextField(
                value = link,
                onValueChange = onLinkChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                placeholder = { Text("https://www.kinopoisk.ru/series/1355059/") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CineColors.Background.copy(alpha = 0.5f),
                    unfocusedContainerColor = CineColors.Background.copy(alpha = 0.5f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = CineColors.PrimaryText,
                    unfocusedTextColor = CineColors.PrimaryText,
                    focusedPlaceholderColor = CineColors.MutedText,
                    unfocusedPlaceholderColor = CineColors.MutedText
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = message ?: "Kinopoisk и TMDb",
                    color = messageColor(message),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = onAdd,
                    enabled = link.isNotBlank() && !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CineColors.Gold,
                        contentColor = Color(0xFF171008),
                        disabledContainerColor = CineColors.Stroke,
                        disabledContentColor = CineColors.MutedText
                    )
                ) {
                    Text(if (isLoading) "..." else "Добавить")
                }
            }
        }
    }
}

@Composable
private fun messageColor(message: String?): Color {
    return when {
        message == null -> CineColors.MutedText
        message.startsWith("Добавлено") -> CineColors.Mint
        else -> CineColors.Coral
    }
}

@Composable
private fun LibraryScreen(
    viewModel: CineViewModel,
    onCreateCollection: (String) -> Unit,
    onOpenCollection: (Long, String) -> Unit,
    onOpenItem: (MediaItem) -> Unit
) {
    var newCollectionName by remember { mutableStateOf("") }
    var visibleLimit by remember { mutableStateOf(LibraryPageSize) }
    val gridState = rememberLazyGridState()
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 20.dp
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val trackedTitles = uiState.trackedTitles
    val visibleTitles by viewModel.filteredLibraryTitles.collectAsStateWithLifecycle()
    val selectedStatus = uiState.libraryStatusFilter
    val collections = uiState.collections

    val pagedTitles = remember(visibleTitles, visibleLimit) {
        visibleTitles.take(visibleLimit)
    }
    val watchingCount = remember(trackedTitles) { trackedTitles.count { it.status == TrackStatus.Watching } }
    val plannedCount = remember(trackedTitles) { trackedTitles.count { it.status == TrackStatus.Planned } }
    val watchedCount = remember(trackedTitles) { trackedTitles.count { it.status == TrackStatus.Watched } }
    val ratedCount = remember(trackedTitles) { trackedTitles.count { it.personalRating != null } }

    LaunchedEffect(gridState, visibleTitles.size, visibleLimit) {
        if (visibleLimit >= visibleTitles.size) return@LaunchedEffect
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .map { lastVisibleIndex -> lastVisibleIndex >= visibleLimit - 8 }
            .distinctUntilChanged()
            .collect { shouldLoadMore ->
                if (shouldLoadMore) {
                    visibleLimit = (visibleLimit + LibraryPageSize).coerceAtMost(visibleTitles.size)
                }
            }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .background(CineColors.Background),
        contentPadding = PaddingValues(start = 20.dp, top = topPadding, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            LibraryMinimalHeader(
                total = trackedTitles.size,
                watching = watchingCount,
                planned = plannedCount,
                watched = watchedCount,
                rated = ratedCount
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            CollectionsBlock(
                collections = collections,
                newCollectionName = newCollectionName,
                onNameChanged = { newCollectionName = it },
                onOpenCollection = onOpenCollection,
                onCreateCollection = {
                    onCreateCollection(newCollectionName)
                    newCollectionName = ""
                }
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            LibraryStatusFilterRow(
                selectedStatus = selectedStatus,
                totalCount = trackedTitles.size,
                watchingCount = watchingCount,
                plannedCount = plannedCount,
                watchedCount = watchedCount,
                onStatusSelected = { viewModel.setLibraryStatus(it) }
            )
        }
        if (visibleTitles.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyLibraryCard(
                    title = if (trackedTitles.isEmpty()) "Список пуст" else "В этой категории пусто",
                    subtitle = if (trackedTitles.isEmpty()) {
                        "Открой карточку фильма или сериала и выбери статус просмотра."
                    } else {
                        "Попробуй другой статус просмотра."
                    }
                )
            }
        }
        gridItems(
            items = pagedTitles,
            key = { it.item.mediaKey() },
            contentType = { it.status.routeValue }
        ) { trackedTitle ->
            LibraryPosterCard(
                trackedTitle = trackedTitle,
                onClick = { onOpenItem(trackedTitle.item) }
            )
        }
    }
}

@Composable
private fun LibraryMinimalHeader(
    total: Int,
    watching: Int,
    planned: Int,
    watched: Int,
    rated: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Мой список",
                    color = CineColors.PrimaryText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "$total тайтлов · $rated оценок",
                    color = CineColors.MutedText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LibraryMiniStat("Смотрю", watching.toString(), CineColors.Mint, Modifier.weight(1f))
            LibraryMiniStat("План", planned.toString(), CineColors.Gold, Modifier.weight(1f))
            LibraryMiniStat("Готово", watched.toString(), CineColors.Coral, Modifier.weight(1f))
        }
    }
}

@Composable
private fun LibraryMiniStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(14.dp),
        color = CineColors.Card.copy(alpha = 0.74f),
        border = BorderStroke(1.dp, CineColors.Stroke.copy(alpha = 0.72f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = value,
                color = CineColors.PrimaryText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Text(
                text = label,
                color = CineColors.MutedText,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LibraryHeaderCard(
    total: Int,
    watching: Int,
    planned: Int,
    watched: Int,
    rated: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = CineColors.Card,
        border = BorderStroke(1.dp, CineColors.Gold.copy(alpha = 0.22f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            CineColors.Gold.copy(alpha = 0.18f),
                            CineColors.Card,
                            CineColors.Mint.copy(alpha = 0.08f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Мой список",
                            color = CineColors.PrimaryText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "$total тайтлов · $rated оценок",
                            color = CineColors.SoftText,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(CineColors.Background.copy(alpha = 0.46f))
                            .border(1.dp, CineColors.Gold.copy(alpha = 0.32f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_bookmark),
                            contentDescription = null,
                            tint = CineColors.Gold,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LibraryMetricPill("Смотрю", watching.toString(), CineColors.Mint, Modifier.weight(1f))
                    LibraryMetricPill("План", planned.toString(), CineColors.Gold, Modifier.weight(1f))
                    LibraryMetricPill("Готово", watched.toString(), CineColors.Coral, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LibraryMetricPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(76.dp),
        shape = RoundedCornerShape(20.dp),
        color = CineColors.Background.copy(alpha = 0.52f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.34f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = value,
                    color = color,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
            Text(
                text = label,
                color = CineColors.MutedText,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProfileScreen(
    isTmdbConfigured: Boolean,
    stats: TrackerStats
) {
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 20.dp
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CineColors.Background),
        contentPadding = PaddingValues(start = 20.dp, top = topPadding, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProfileSummaryCard(
                isTmdbConfigured = isTmdbConfigured,
                stats = stats
            )
        }
        item {
            ProfileProgressCard(stats = stats)
        }
    }
}

@Composable
private fun ProfileSummaryCard(isTmdbConfigured: Boolean, stats: TrackerStats) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = CineColors.Card,
        border = BorderStroke(1.dp, CineColors.Stroke)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            CineColors.Gold.copy(alpha = 0.16f),
                            CineColors.Card,
                            CineColors.Mint.copy(alpha = 0.08f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Кинопрофиль",
                            color = CineColors.PrimaryText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (isTmdbConfigured) "Каталог подключен · данные обновляются" else "Каталог офлайн · работаем с библиотекой",
                            color = CineColors.SoftText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_user),
                            contentDescription = null,
                            tint = CineColors.Gold,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileStatTile(
                        label = "Тайтлов",
                        value = stats.total.toString(),
                        color = CineColors.Gold,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatTile(
                        label = "Время",
                        value = stats.watchedMinutes.formatWatchTime(),
                        color = CineColors.Mint,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileMiniChip(label = "Оценок", value = stats.ratedTitles.toString())
                    ProfileMiniChip(label = "Серий", value = stats.ratedEpisodes.toString())
                    ProfileMiniChip(label = "Коллекций", value = stats.collections.toString())
                }
            }
        }
    }
}

@Composable
private fun ProfileProgressCard(stats: TrackerStats) {
    val total = stats.total.coerceAtLeast(1)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CineColors.Card,
        border = BorderStroke(1.dp, CineColors.Stroke)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Прогресс просмотра",
                color = CineColors.PrimaryText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            ProfileProgressRow("Смотрю", stats.watching, total, CineColors.Mint)
            ProfileProgressRow("В плане", stats.planned, total, CineColors.Gold)
            ProfileProgressRow("Просмотрено", stats.watched, total, CineColors.Coral)
        }
    }
}

@Composable
private fun ProfileProgressRow(label: String, value: Int, total: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = CineColors.SoftText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value.toString(),
                color = color,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = { (value.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = color,
            trackColor = CineColors.Background.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun ProfileStatTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                color = color,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                color = CineColors.MutedText,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProfileMiniChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CineColors.Background.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, CineColors.Stroke)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = value,
                color = CineColors.PrimaryText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = CineColors.MutedText,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun EventTimelineCard(events: List<TrackerEvent>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CineColors.Card,
        border = BorderStroke(1.dp, CineColors.Stroke)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Последние действия",
                    color = CineColors.PrimaryText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${events.size}",
                    color = CineColors.Gold,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            if (events.isEmpty()) {
                Text(
                    text = "Здесь появятся оценки, статусы и добавления в коллекции.",
                    color = CineColors.MutedText,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                events.take(8).forEach { event ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = CineColors.Background.copy(alpha = 0.44f),
                        border = BorderStroke(1.dp, CineColors.Stroke)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(CineColors.Gold)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = event.message,
                                    color = CineColors.PrimaryText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = event.createdAt.formatEventTime(),
                                    color = CineColors.MutedText,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiQuotaCard(
    quota: ApiQuota?,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CineColors.Card,
        border = BorderStroke(1.dp, CineColors.Stroke)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Лимит PoiskKino",
                        color = CineColors.PrimaryText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = quota?.let { "Осталось ${it.remaining} из ${it.limit}" } ?: "Данные пока недоступны",
                        color = CineColors.MutedText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !isLoading,
                    border = BorderStroke(1.dp, CineColors.Stroke),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CineColors.Gold)
                ) {
                    Text(if (isLoading) "..." else "Обновить")
                }
            }
            LinearProgressIndicator(
                progress = { quota?.usedFraction ?: 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = when {
                    quota == null -> CineColors.Stroke
                    quota.remaining < 25 -> CineColors.Coral
                    else -> CineColors.Gold
                },
                trackColor = CineColors.Background.copy(alpha = 0.55f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CompactStat(label = "Использовано", value = quota?.used?.toString() ?: "-", color = CineColors.Coral, modifier = Modifier.weight(1f))
                CompactStat(label = "Осталось", value = quota?.remaining?.toString() ?: "-", color = CineColors.Mint, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CompactStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(86.dp),
        shape = RoundedCornerShape(18.dp),
        color = CineColors.Background.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, CineColors.Stroke)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Column {
                Text(
                    text = value,
                    color = CineColors.PrimaryText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = label,
                    color = CineColors.MutedText,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ThemeSettingsCard(
    selectedTheme: CineAppTheme,
    onThemeSelected: (CineAppTheme) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = CineColors.Card,
        border = BorderStroke(1.dp, CineColors.Stroke)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Тема",
                color = CineColors.PrimaryText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(CineAppTheme.entries) { theme ->
                    ThemeChoiceButton(
                        theme = theme,
                        selected = theme == selectedTheme,
                        onClick = { onThemeSelected(theme) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeChoiceButton(
    theme: CineAppTheme,
    selected: Boolean,
    onClick: () -> Unit
) {
    val preview = paletteFor(theme)
    Surface(
        modifier = Modifier
            .width(142.dp)
            .height(74.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) CineColors.Gold else CineColors.Background.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, if (selected) CineColors.Gold else CineColors.Stroke)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(preview.background, preview.gold, preview.mint).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, preview.stroke, CircleShape)
                    )
                }
            }
            Text(
                text = theme.title,
                color = if (selected) CineColors.OnGold else CineColors.PrimaryText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LibraryKindFilterRow(
    selectedKind: MediaKind?,
    movieCount: Int,
    showCount: Int,
    onKindSelected: (MediaKind?) -> Unit
) {
    val filters: List<MediaKind?> = listOf(null, MediaKind.Movie, MediaKind.Tv)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(filters) { kind ->
            val selected = selectedKind == kind
            val count = when (kind) {
                MediaKind.Movie -> movieCount
                MediaKind.Tv -> showCount
                null -> movieCount + showCount
            }
            LibraryFilterChip(
                text = "${kind?.label ?: "Все"} $count",
                selected = selected,
                onClick = { onKindSelected(kind) }
            )
        }
    }
}

@Composable
private fun LibraryStatusFilterRow(
    selectedStatus: TrackStatus?,
    totalCount: Int,
    watchingCount: Int,
    plannedCount: Int,
    watchedCount: Int,
    onStatusSelected: (TrackStatus?) -> Unit
) {
    val filters: List<TrackStatus?> = listOf(null) + TrackStatus.entries
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(filters) { status ->
            val selected = selectedStatus == status
            val count = when (status) {
                TrackStatus.Watching -> watchingCount
                TrackStatus.Planned -> plannedCount
                TrackStatus.Watched -> watchedCount
                null -> totalCount
            }
            LibraryFilterChip(
                text = "${status?.title ?: "Все"} $count",
                selected = selected,
                onClick = { onStatusSelected(status) }
            )
        }
    }
}

@Composable
private fun LibraryFilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) CineColors.Gold.copy(alpha = 0.14f) else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (selected) CineColors.Gold.copy(alpha = 0.52f) else CineColors.Stroke.copy(alpha = 0.64f)
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            color = if (selected) CineColors.Gold else CineColors.SoftText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CollectionScreen(
    name: String,
    items: List<MediaItem>,
    onBack: () -> Unit,
    onOpenItem: (MediaItem) -> Unit
) {
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CineColors.Background)
    ) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, top = topPadding, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), "Назад", tint = CineColors.PrimaryText)
            }
            Text(
                text = name,
                color = CineColors.PrimaryText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                SearchResultCard(item) { onOpenItem(item) }
            }
        }
    }
}

@Composable
private fun CollectionsBlock(
    collections: List<MediaCollection>,
    newCollectionName: String,
    onNameChanged: (String) -> Unit,
    onOpenCollection: (Long, String) -> Unit,
    onCreateCollection: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextField(
                value = newCollectionName,
                onValueChange = onNameChanged,
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text("Новая коллекция", style = MaterialTheme.typography.bodyMedium) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CineColors.Card,
                    unfocusedContainerColor = CineColors.Card,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = CineColors.PrimaryText,
                    unfocusedTextColor = CineColors.PrimaryText
                )
            )
            IconButton(
                onClick = onCreateCollection,
                enabled = newCollectionName.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (newCollectionName.isNotBlank()) CineColors.Gold else CineColors.Card)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_plus),
                    contentDescription = null,
                    tint = if (newCollectionName.isNotBlank()) CineColors.OnGold else CineColors.MutedText
                )
            }
        }
        if (collections.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(collections, key = { it.id }) { collection ->
                    Surface(
                        onClick = { onOpenCollection(collection.id, collection.name) },
                        shape = RoundedCornerShape(16.dp),
                        color = CineColors.Card,
                        border = BorderStroke(1.dp, CineColors.Stroke)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_folder),
                                contentDescription = null,
                                tint = CineColors.Gold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = collection.name,
                                color = CineColors.PrimaryText,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryCard(title: String, subtitle: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CineColors.Card,
        border = BorderStroke(1.dp, CineColors.Stroke)
    ) {
        Row(
            modifier = Modifier.padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CineLoaderAnimation(modifier = Modifier.size(58.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title,
                    color = CineColors.PrimaryText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = CineColors.MutedText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun LibraryPosterCard(trackedTitle: TrackedTitle, onClick: () -> Unit) {
    val item = trackedTitle.item
    val accent = statusColor(trackedTitle.status)
    val serviceRating = item.ratings.primaryScore
        .takeIf { it > 0.0 }
        ?: item.rating.takeIf { it > 0.0 }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box {
            PosterArt(
                item = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f),
                imageWidthPx = 320,
                imageHeightPx = 480
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(12.dp),
                color = CineColors.Background.copy(alpha = 0.82f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
            ) {
                Text(
                    text = trackedTitle.personalRating?.let { "Моя $it" } ?: serviceRating.formatRating(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    color = trackedTitle.personalRating?.let(::ratingColor) ?: CineColors.Gold,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
        }

        Column(
            modifier = Modifier.height(64.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.title,
                color = CineColors.PrimaryText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Text(
                    text = listOf(item.year, item.kind.label).filter { it.isNotBlank() }.joinToString(" · "),
                    color = CineColors.MutedText,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LibraryTitleCardMinimal(trackedTitle: TrackedTitle, onClick: () -> Unit) {
    val item = trackedTitle.item
    val accent = statusColor(trackedTitle.status)
    val progress = trackedTitle.progress.coerceIn(0f, 1f)
    val progressPercent = (progress * 100).toInt()
    val serviceRating = item.ratings.primaryScore
        .takeIf { it > 0.0 }
        ?: item.rating.takeIf { it > 0.0 }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = CineColors.Card.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, CineColors.Stroke.copy(alpha = 0.78f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PosterArt(
                item = item,
                modifier = Modifier
                    .width(68.dp)
                    .fillMaxHeight(),
                imageWidthPx = 136,
                imageHeightPx = 204
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.title,
                    color = CineColors.PrimaryText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOf(item.year, item.kind.label).filter { it.isNotBlank() }.joinToString(" · "),
                    color = CineColors.MutedText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MinimalStatusLabel(status = trackedTitle.status, color = accent)
                    Text(
                        text = "★ ${serviceRating.formatRating()}",
                        color = CineColors.MutedText,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                    trackedTitle.personalRating?.let { rating ->
                        Text(
                            text = "Моя $rating",
                            color = ratingColor(rating),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (trackedTitle.status != TrackStatus.Planned) {
                        Text(
                            text = "$progressPercent%",
                            color = accent,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { if (trackedTitle.status == TrackStatus.Planned) 0f else progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape),
                    color = accent,
                    trackColor = CineColors.Background.copy(alpha = 0.68f)
                )
            }
        }
    }
}

@Composable
private fun MinimalStatusLabel(status: TrackStatus, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = status.title,
            color = CineColors.SoftText,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LibraryTitleCardRedesigned(trackedTitle: TrackedTitle, onClick: () -> Unit) {
    val item = trackedTitle.item
    val statusAccent = statusColor(trackedTitle.status)
    val progressPercent = (trackedTitle.progress.coerceIn(0f, 1f) * 100).toInt()
    val serviceRating = item.ratings.primaryScore
        .takeIf { it > 0.0 }
        ?: item.rating.takeIf { it > 0.0 }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(174.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = CineColors.Card,
        border = BorderStroke(1.dp, statusAccent.copy(alpha = 0.26f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            statusAccent.copy(alpha = 0.12f),
                            CineColors.Card,
                            CineColors.Card
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(statusAccent)
                )
                PosterArt(
                    item = item,
                    modifier = Modifier
                        .width(92.dp)
                        .fillMaxHeight(),
                    imageWidthPx = 184,
                    imageHeightPx = 276
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusBadge(trackedTitle.status)
                        Text(
                            text = listOf(item.year, item.kind.label).filter { it.isNotBlank() }.joinToString(" · "),
                            color = CineColors.MutedText,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = item.title,
                        color = CineColors.PrimaryText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.overview.ifBlank { "Описание пока не загружено." },
                        color = CineColors.SoftText,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LibraryInfoPill(
                            label = item.ratings.primarySource ?: "Рейтинг",
                            value = serviceRating.formatRating(),
                            color = CineColors.Gold,
                            modifier = Modifier.weight(1f)
                        )
                        LibraryInfoPill(
                            label = "Моя",
                            value = trackedTitle.personalRating?.toString() ?: "—",
                            color = trackedTitle.personalRating?.let(::ratingColor) ?: CineColors.MutedText,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (trackedTitle.status == TrackStatus.Planned) "В плане" else "$progressPercent%",
                            color = statusAccent,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { if (trackedTitle.status == TrackStatus.Planned) 0f else trackedTitle.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = statusAccent,
                        trackColor = CineColors.Background.copy(alpha = 0.72f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryInfoPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(14.dp),
        color = CineColors.Background.copy(alpha = 0.54f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.24f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                color = color,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                color = CineColors.MutedText,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LibraryTitleCard(trackedTitle: TrackedTitle, onClick: () -> Unit) {
    val item = trackedTitle.item
    val progressPercent = (trackedTitle.progress.coerceIn(0f, 1f) * 100).toInt()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(142.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CineColors.Card),
        border = BorderStroke(1.dp, statusColor(trackedTitle.status).copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PosterArt(
                item = item,
                modifier = Modifier
                    .width(82.dp)
                    .fillMaxHeight(),
                imageWidthPx = 164,
                imageHeightPx = 246
            )
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        modifier = Modifier.weight(1f),
                        color = CineColors.PrimaryText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    StatusBadge(trackedTitle.status)
                }
                Text(
                    text = listOf(item.year, item.kind.label).filter { it.isNotBlank() }.joinToString(" · "),
                    color = CineColors.MutedText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.overview,
                    color = CineColors.SoftText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RatingBadge(
                        score = item.rating,
                        personal = trackedTitle.personalRating,
                        source = item.ratings.primarySource
                    )
                    if (trackedTitle.status != TrackStatus.Planned) {
                        Text(
                            text = "$progressPercent%",
                            color = statusColor(trackedTitle.status),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { trackedTitle.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = statusColor(trackedTitle.status),
                    trackColor = CineColors.Stroke
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(
    detail: MediaDetail?,
    isLoading: Boolean,
    currentStatus: TrackStatus?,
    personalRating: Int?,
    episodeRatings: Map<String, Int>,
    collections: List<MediaCollection>,
    selectedCollectionIds: Set<Long>,
    trackedTitles: List<TrackedTitle>,
    onBack: () -> Unit,
    onOpenItem: (MediaItem) -> Unit,
    onSetStatus: (TrackStatus) -> Unit,
    onSetRating: (Int) -> Unit,
    onSetEpisodeRating: (Int, Int, Int, Int?) -> Unit,
    onLoadSeasonDetails: (Int) -> Unit,
    onCreateCollection: (String) -> Unit,
    onToggleCollection: (Long) -> Unit,
    onRemoveFromTracker: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showRatingSheet by remember { mutableStateOf(false) }
    var showCollectionSheet by remember { mutableStateOf(false) }
    var episodeToRate by remember { mutableStateOf<EpisodeInfo?>(null) }
    val statusTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    if (showRatingSheet) {
        detail?.item?.let { ratingItem ->
            RatingPickerSheet(
                item = ratingItem,
                currentRating = personalRating,
                trackedTitles = trackedTitles,
                sheetState = sheetState,
                onDismiss = { showRatingSheet = false },
                onOpenItem = { sameRatedItem ->
                    showRatingSheet = false
                    onOpenItem(sameRatedItem)
                },
                onRatingSelected = {
                    onSetRating(it)
                }
            )
        }
    }

    episodeToRate?.let { episode ->
        ModalBottomSheet(
            onDismissRequest = { episodeToRate = null },
            sheetState = sheetState,
            containerColor = CineColors.Card,
            dragHandle = {
                Box(
                    Modifier
                        .padding(vertical = 12.dp)
                        .size(36.dp, 4.dp)
                        .clip(CircleShape)
                        .background(CineColors.Stroke)
                )
            }
        ) {
            Box(Modifier.padding(bottom = 40.dp, start = 20.dp, end = 20.dp, top = 10.dp)) {
                PersonalRatingRow(
                    rating = episodeRatings[episode.key],
                    onRatingSelected = { rating ->
                        onSetEpisodeRating(
                            episode.seasonNumber,
                            episode.episodeNumber,
                            rating,
                            episode.runtimeMinutes
                        )
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            episodeToRate = null
                        }
                    }
                )
            }
        }
    }

    if (showCollectionSheet) {
        CollectionPickerSheet(
            collections = collections,
            selectedIds = selectedCollectionIds,
            onDismiss = { showCollectionSheet = false },
            onCreateCollection = onCreateCollection,
            onToggleCollection = onToggleCollection
        )
    }

    if (isLoading || detail == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CineColors.Background)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(start = 16.dp, top = statusTopPadding + 16.dp)
                    .clip(CircleShape)
                    .background(CineColors.Card.copy(alpha = 0.92f))
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Назад",
                    tint = CineColors.PrimaryText
                )
            }
            CineLoaderAnimation(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(96.dp)
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CineColors.Background),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
            ) {
                BackdropArt(item = detail.item, dynamicAccent = true)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, CineColors.Background),
                                startY = 80f
                            )
                        )
                )
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(start = 16.dp, top = statusTopPadding + 16.dp)
                        .clip(CircleShape)
                        .background(CineColors.Card.copy(alpha = 0.86f))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = "Назад",
                        tint = CineColors.PrimaryText
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                PosterArt(
                    item = detail.item,
                    modifier = Modifier
                        .width(126.dp)
                        .aspectRatio(0.68f),
                    dynamicAccent = true
                )
                Spacer(Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = detail.item.title,
                            modifier = Modifier.weight(1f),
                            color = CineColors.PrimaryText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        RatingIconButton(
                            personalRating = personalRating,
                            onClick = { showRatingSheet = true }
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { RatingBadge(score = detail.item.rating, source = detail.item.ratings.primarySource) }
                    }
                    RatingsStrip(ratings = detail.item.ratings)
                    Text(
                        text = listOf(detail.item.kind.label, detail.item.year, detail.runtimeText, detail.statusText).joinToString(" • "),
                        color = CineColors.MutedText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 22.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(CineColors.Card.copy(alpha = 0.92f))
                    .border(1.dp, CineColors.Stroke.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(detail.genres.ifEmpty { listOf("Кино") }) { genre ->
                        TextPill(genre)
                    }
                }
                TrackerActionRow(
                    currentStatus = currentStatus,
                    onSetStatus = onSetStatus,
                    onOpenCollections = { showCollectionSheet = true },
                    onRemoveFromTracker = onRemoveFromTracker
                )

                EpisodeTrackerBlock(
                    seasons = detail.seasons,
                    episodeRatings = episodeRatings,
                    onRateEpisode = { episodeToRate = it },
                    onSeasonSelected = onLoadSeasonDetails
                )
            }
        }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "Описание",
                    color = CineColors.PrimaryText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = detail.item.overview,
                    color = CineColors.SoftText,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp
                )
                RelatedTitlesBlock(
                    detail = detail,
                    trackedTitles = trackedTitles,
                    onOpenItem = onOpenItem
                )
            }
        }
    }
}

@Composable
private fun RelatedTitlesBlock(
    detail: MediaDetail,
    trackedTitles: List<TrackedTitle>,
    onOpenItem: (MediaItem) -> Unit
) {
    if (detail.prequels.isEmpty() && detail.sequels.isEmpty() && detail.related.isEmpty()) {
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (detail.prequels.isNotEmpty()) {
            MediaRail(
                title = "Приквелы",
                action = "",
                items = detail.prequels,
                trackedTitles = trackedTitles,
                onOpenItem = onOpenItem
            )
        }
        if (detail.sequels.isNotEmpty()) {
            MediaRail(
                title = "Сиквелы",
                action = "",
                items = detail.sequels,
                trackedTitles = trackedTitles,
                onOpenItem = onOpenItem
            )
        }
        if (detail.related.isNotEmpty()) {
            MediaRail(
                title = if (detail.item.kind == MediaKind.Tv) "Похожие сериалы" else "Похожие фильмы",
                action = "",
                items = detail.related,
                trackedTitles = trackedTitles,
                onOpenItem = onOpenItem
            )
        }
    }
}

@Composable
private fun Header(
    selectedFilter: HomeFilter,
    onFilterSelected: (HomeFilter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Добрый вечер",
                    color = CineColors.MutedText,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "Что смотрим сегодня?",
                    color = CineColors.PrimaryText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(HomeFilter.entries) { filter ->
                FilterPill(
                    text = filter.title,
                    selected = filter == selectedFilter,
                    onClick = { onFilterSelected(filter) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouletteDialog(
    item: MediaItem,
    onDismiss: () -> Unit,
    onOpen: (MediaItem) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CineColors.Card,
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .size(36.dp, 4.dp)
                    .clip(CircleShape)
                    .background(CineColors.Stroke)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "🎲 Кино-рулетка",
                color = CineColors.Gold,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CineColors.Background)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PosterArt(item = item, modifier = Modifier.width(100.dp).fillMaxHeight())
                    Spacer(Modifier.width(16.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = item.title,
                            color = CineColors.PrimaryText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        RatingBadge(score = item.rating, source = item.ratings.primarySource)
                        Text(
                            text = item.overview,
                            color = CineColors.MutedText,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CineColors.Stroke)
                ) {
                    Text("Закрыть", color = CineColors.PrimaryText)
                }
                Button(
                    onClick = { onOpen(item) },
                    modifier = Modifier.weight(1.5f).height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CineColors.Gold)
                ) {
                    Text("Открыть", color = CineColors.OnGold, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RatingPickerSheet(
    item: MediaItem,
    currentRating: Int?,
    trackedTitles: List<TrackedTitle>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onOpenItem: (MediaItem) -> Unit,
    onRatingSelected: (Int) -> Unit
) {
    var selectedRating by remember(item.id, item.kind, currentRating) {
        mutableStateOf(currentRating ?: item.rating.toInt().takeIf { it in 1..10 } ?: 7)
    }
    val ratedItems = remember(trackedTitles, selectedRating, item.id, item.kind) {
        val sameRated = trackedTitles
            .filter { it.personalRating == selectedRating }
            .map { it.item }
            .filterNot { it.id == item.id && it.kind == item.kind }
        (listOf(item) + sameRated)
            .distinctBy { "${it.kind.routeValue}:${it.id}" }
            .take(12)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF101012),
        dragHandle = null,
        modifier = Modifier.fillMaxHeight()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(bottom = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 14.dp, top = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Оценить",
                        color = CineColors.PrimaryText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Text(
                            text = "×",
                            color = CineColors.MutedText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            item {
                PosterArt(
                    item = item,
                        modifier = Modifier
                            .width(132.dp)
                            .height(196.dp)
                )
            }
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.title,
                        color = CineColors.PrimaryText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Text(
                        text = "${item.year} · ${item.kind.label.lowercase()}",
                        color = CineColors.MutedText,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            item {
                RatingScorePicker(
                    selectedRating = selectedRating,
                    onRatingSelected = { score ->
                        selectedRating = score
                        onRatingSelected(score)
                    }
                )
            }
            item {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Ваша оценка: $selectedRating",
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                        color = CineColors.PrimaryText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            item {
                SameRatingRail(
                    rating = selectedRating,
                    ratedItems = ratedItems,
                    onOpenItem = onOpenItem
                )
            }
        }
    }
}

@Composable
private fun RatingScorePicker(
    selectedRating: Int,
    onRatingSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(1..5, 6..10).forEach { rowScores ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowScores.forEach { score ->
                    val isSelected = score == selectedRating
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(62.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) ratingColor(score) else Color.White.copy(alpha = 0.08f))
                            .border(
                                1.dp,
                                if (isSelected) ratingColor(score) else Color.White.copy(alpha = 0.12f),
                                RoundedCornerShape(18.dp)
                            )
                            .clickable { onRatingSelected(score) }
                            .bounceClick(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = score.toString(),
                            color = if (isSelected) ratingContentColor(score) else Color.White,
                            fontSize = if (isSelected) 26.sp else 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SameRatingRail(
    rating: Int,
    ratedItems: List<MediaItem>,
    onOpenItem: (MediaItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Ваши фильмы и сериалы на $rating",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            color = CineColors.MutedText,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )
        if (ratedItems.isEmpty()) {
            Text(
                text = "Здесь появятся тайтлы с такой же оценкой.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                color = CineColors.MutedText,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                items(ratedItems, key = { "${it.kind.routeValue}:${it.id}" }) { ratedItem ->
                    Column(
                        modifier = Modifier
                            .width(118.dp)
                            .clickable { onOpenItem(ratedItem) },
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PosterArt(
                            item = ratedItem,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(174.dp)
                        )
                        Text(
                            text = ratedItem.title,
                            color = CineColors.PrimaryText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalRatingRow(rating: Int?, onRatingSelected: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Ваша оценка",
            color = CineColors.PrimaryText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 20.dp)
        ) {
            items(10) { index ->
                val score = index + 1
                val isSelected = rating == score
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) CineColors.Gold else CineColors.Card)
                        .border(
                            1.dp,
                            if (isSelected) CineColors.Gold else CineColors.Stroke,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onRatingSelected(score) }
                        .bounceClick(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = score.toString(),
                        color = if (isSelected) CineColors.Background else CineColors.PrimaryText,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeTrackerBlock(
    seasons: List<SeasonInfo>,
    episodeRatings: Map<String, Int>,
    onRateEpisode: (EpisodeInfo) -> Unit,
    onSeasonSelected: (Int) -> Unit
) {
    if (seasons.isEmpty()) return
    var isExpanded by remember { mutableStateOf(false) }

    var selectedSeasonNumber by remember(seasons) { mutableStateOf(seasons.first().number) }
    val selectedSeason = seasons.firstOrNull { it.number == selectedSeasonNumber } ?: seasons.first()
    val episodeCount = seasons.sumOf { it.episodeCount }
    LaunchedEffect(selectedSeason.number, isExpanded) {
        if (isExpanded) {
            onSeasonSelected(selectedSeason.number)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            shape = RoundedCornerShape(20.dp),
            color = CineColors.Card,
            border = BorderStroke(1.dp, CineColors.Stroke)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "Сезоны и серии",
                        color = CineColors.PrimaryText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${seasons.size} сез. · $episodeCount эп.",
                        color = CineColors.MutedText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = if (isExpanded) "Скрыть" else "Показать",
                    color = CineColors.Gold,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (!isExpanded) return@Column

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(seasons, key = { it.number }) { season ->
                LibraryFilterChip(
                    text = "${season.title} · ${season.episodeCount}",
                    selected = season.number == selectedSeason.number,
                    onClick = { selectedSeasonNumber = season.number }
                )
            }
        }
        if (selectedSeason.episodes.isEmpty()) {
            Text(
                text = "Список серий пока недоступен.",
                color = CineColors.MutedText,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                selectedSeason.episodes.forEach { episode ->
                    EpisodeRow(
                        episode = episode,
                        rating = episodeRatings[episode.key],
                        onClick = { onRateEpisode(episode) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: EpisodeInfo,
    rating: Int?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = CineColors.Card,
        border = BorderStroke(1.dp, if (rating != null) CineColors.Gold else CineColors.Stroke)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(86.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CineColors.Background.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                if (episode.posterUrl != null) {
                    AsyncImage(
                        model = episode.posterUrl,
                        contentDescription = episode.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "E${episode.episodeNumber}",
                        color = CineColors.PrimaryText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    color = CineColors.PrimaryText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = listOfNotNull(
                        "S${episode.seasonNumber} E${episode.episodeNumber}",
                        episode.airDate.takeIf { it.isNotBlank() },
                        episode.runtimeMinutes?.let { "$it мин" }
                    ).joinToString(" • "),
                    color = CineColors.MutedText,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = episode.overview.ifBlank { "Описание серии пока не добавлено." },
                    color = CineColors.SoftText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (episode.serviceRating != null) {
                    Text(
                        text = "TMDb ${episode.serviceRating.formatRating()}",
                        color = CineColors.Gold,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (rating != null) {
                RatingBadge(score = 0.0, personal = rating)
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_star),
                    contentDescription = "Оценить серию",
                    tint = CineColors.MutedText,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionPickerSheet(
    collections: List<MediaCollection>,
    selectedIds: Set<Long>,
    onDismiss: () -> Unit,
    onCreateCollection: (String) -> Unit,
    onToggleCollection: (Long) -> Unit
) {
    var newCollectionName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CineColors.Card,
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .size(36.dp, 4.dp)
                    .clip(CircleShape)
                    .background(CineColors.Stroke)
            )
        }
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Коллекции",
                color = CineColors.PrimaryText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextField(
                    value = newCollectionName,
                    onValueChange = { newCollectionName = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("Новая коллекция") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CineColors.Background.copy(alpha = 0.45f),
                        unfocusedContainerColor = CineColors.Background.copy(alpha = 0.45f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = CineColors.PrimaryText,
                        unfocusedTextColor = CineColors.PrimaryText,
                        focusedPlaceholderColor = CineColors.MutedText,
                        unfocusedPlaceholderColor = CineColors.MutedText
                    )
                )
                IconButton(
                    onClick = {
                        onCreateCollection(newCollectionName)
                        newCollectionName = ""
                    },
                    enabled = newCollectionName.isNotBlank(),
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (newCollectionName.isNotBlank()) CineColors.Gold else CineColors.Background.copy(alpha = 0.45f))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_plus),
                        contentDescription = "Создать",
                        tint = if (newCollectionName.isNotBlank()) CineColors.OnGold else CineColors.MutedText
                    )
                }
            }
            if (collections.isEmpty()) {
                Text(
                    text = "Создай первую коллекцию и добавь сюда фильм или сериал.",
                    color = CineColors.MutedText,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                collections.forEach { collection ->
                    val selected = collection.id in selectedIds
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleCollection(collection.id) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) CineColors.Gold.copy(alpha = 0.18f) else CineColors.Background.copy(alpha = 0.38f),
                        border = BorderStroke(1.dp, if (selected) CineColors.Gold else CineColors.Stroke)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_folder),
                                contentDescription = null,
                                tint = if (selected) CineColors.Gold else CineColors.MutedText,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = collection.name,
                                    color = CineColors.PrimaryText,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${collection.itemCount} в коллекции",
                                    color = CineColors.MutedText,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = if (selected) "Добавлено" else "Добавить",
                                color = if (selected) CineColors.Gold else CineColors.SoftText,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackerActionRow(
    currentStatus: TrackStatus?,
    onSetStatus: (TrackStatus) -> Unit,
    onOpenCollections: () -> Unit,
    onRemoveFromTracker: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TrackerStatusButton(
                status = TrackStatus.Watching,
                label = "Смотрю",
                icon = R.drawable.ic_play,
                currentStatus = currentStatus,
                onSetStatus = onSetStatus,
                modifier = Modifier.weight(1f)
            )
            TrackerStatusButton(
                status = TrackStatus.Planned,
                label = "В план",
                icon = R.drawable.ic_bookmark,
                currentStatus = currentStatus,
                onSetStatus = onSetStatus,
                modifier = Modifier.weight(1f)
            )
            TrackerStatusButton(
                status = TrackStatus.Watched,
                label = "Готово",
                icon = R.drawable.ic_star,
                currentStatus = currentStatus,
                onSetStatus = onSetStatus,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenCollections),
                shape = RoundedCornerShape(14.dp),
                color = CineColors.Card,
                border = BorderStroke(1.dp, CineColors.Stroke)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_folder),
                        contentDescription = null,
                        tint = CineColors.Gold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "В коллекцию",
                        color = CineColors.PrimaryText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (currentStatus != null) {
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onRemoveFromTracker),
                    shape = RoundedCornerShape(14.dp),
                    color = CineColors.Card,
                    border = BorderStroke(1.dp, CineColors.Stroke)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = "Убрать из трекера",
                            tint = CineColors.Coral,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackerStatusButton(
    status: TrackStatus,
    label: String,
    @DrawableRes icon: Int,
    currentStatus: TrackStatus?,
    onSetStatus: (TrackStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = currentStatus == status
    if (selected) {
        Button(
            onClick = { onSetStatus(status) },
            modifier = modifier.height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CineColors.Gold,
                contentColor = CineColors.OnGold
            ),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else {
        OutlinedButton(
            onClick = { onSetStatus(status) },
            modifier = modifier.height(48.dp),
            border = BorderStroke(1.dp, CineColors.Stroke),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = CineColors.Gold,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                color = CineColors.PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InfoBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CineColors.Card,
        border = BorderStroke(1.dp, CineColors.Stroke)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            color = CineColors.SoftText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun FeaturedTitleCard(item: MediaItem, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(238.dp)
            .bounceClick()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CineColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            BackdropArt(item = item)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.2f),
                                Color(0xFF090A0E).copy(alpha = 0.95f)
                            ),
                            startY = 100f
                        )
                    )
                    .padding(22.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.BottomStart),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconPill(icon = mediaIcon(item.kind), text = "Премьера недели")
                    Text(
                        text = item.title,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.subtitle,
                        color = CineColors.SoftText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    RatingBadge(score = item.rating, source = item.ratings.primarySource)
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingRow(items: List<ContinueWatchingItem>, onOpenItem: (MediaItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader(title = "Продолжить", action = "Все")
        if (items.isEmpty()) {
            EmptyContinueCard()
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(items) { item ->
                    ContinueCard(item, onClick = { onOpenItem(item.item) })
                }
            }
        }
    }
}

@Composable
private fun EmptyContinueCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp),
        shape = RoundedCornerShape(22.dp),
        color = CineColors.Card,
        border = BorderStroke(1.dp, CineColors.Stroke)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Здесь будут текущие просмотры",
                color = CineColors.PrimaryText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Открой карточку и нажми «Смотрю».",
                color = CineColors.MutedText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ContinueCard(item: ContinueWatchingItem, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = Modifier
            .width(318.dp)
            .height(146.dp)
            .bounceClick()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CineColors.Card),
        border = BorderStroke(1.dp, CineColors.Stroke.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PosterArt(
                item = item.item,
                modifier = Modifier
                    .width(84.dp)
                    .height(120.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.item.title,
                    color = CineColors.PrimaryText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.episodeText,
                    color = CineColors.MutedText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = CineColors.Mint,
                    trackColor = CineColors.Stroke
                )
                Text(
                    text = item.timeLeft,
                    color = CineColors.SoftText,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun WatchStatusRow(stats: TrackerStats) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader(title = "Мой трекер", action = "Открыть")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Смотрю",
                value = stats.watching.toString(),
                color = CineColors.Mint,
                modifier = Modifier.weight(1.2f).fillMaxHeight()
            )
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "В плане",
                    value = stats.planned.toString(),
                    color = CineColors.Gold,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
                StatCard(
                    label = "Готово",
                    value = stats.watched.toString(),
                    color = CineColors.Coral,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.shadow(
            elevation = 8.dp,
            shape = RoundedCornerShape(24.dp),
            spotColor = color.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(24.dp),
        color = CineColors.Card,
        border = BorderStroke(1.dp, CineColors.Stroke)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .shadow(elevation = 6.dp, shape = CircleShape, spotColor = color, ambientColor = color)
                        .clip(CircleShape)
                        .background(color)
                )
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp).graphicsLayer { rotationZ = 180f },
                    tint = CineColors.MutedText.copy(alpha = 0.3f)
                )
            }
            Column {
                Text(
                    text = value,
                    color = CineColors.PrimaryText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = label,
                    color = CineColors.MutedText,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun MediaRail(
    title: String,
    action: String,
    items: List<MediaItem>,
    trackedTitles: List<TrackedTitle> = emptyList(),
    onActionClick: (() -> Unit)? = null,
    onOpenItem: (MediaItem) -> Unit
) {
    val personalRatings = remember(trackedTitles) {
        trackedTitles.associate { it.item.mediaKey() to it.personalRating }
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader(title = title, action = action, onActionClick = onActionClick)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(items, key = { it.mediaKey() }) { item ->
                PosterCard(item = item, personalRating = personalRatings[item.mediaKey()], onClick = { onOpenItem(item) })
            }
        }
    }
}

@Composable
private fun PosterCard(item: MediaItem, personalRating: Int? = null, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .bounceClick()
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, CineColors.Stroke, RoundedCornerShape(18.dp))
        ) {
            PosterArt(
                item = item,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.55f to Color.Transparent,
                            1f to CineColors.Background.copy(alpha = 0.55f)
                        )
                    )
            )
            Box(modifier = Modifier.padding(8.dp)) {
                RatingBadge(score = item.rating, personal = personalRating, source = item.ratings.primarySource)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.title,
                color = CineColors.PrimaryText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOf(item.year, item.kind.label).filter { it.isNotBlank() }.joinToString(" · "),
                color = CineColors.MutedText,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SearchResultCard(item: MediaItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CineColors.Card),
        border = BorderStroke(1.dp, CineColors.Stroke)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PosterArt(
                item = item,
                modifier = Modifier
                    .width(74.dp)
                    .height(108.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(mediaIcon(item.kind)),
                        contentDescription = null,
                        tint = CineColors.Gold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = item.subtitle,
                        color = CineColors.MutedText,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Text(
                    text = item.title,
                    color = CineColors.PrimaryText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.overview,
                    color = CineColors.SoftText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SectionListCard(item: MediaItem, personalRating: Int?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CineColors.Card),
        border = BorderStroke(1.dp, CineColors.Stroke)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PosterArt(
                item = item,
                modifier = Modifier
                    .width(86.dp)
                    .height(126.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (personalRating != null) {
                        RatingBadge(score = item.rating, personal = personalRating)
                    }
                    RatingBadge(score = item.rating, source = item.ratings.primarySource)
                }
                Text(
                    text = item.title,
                    color = CineColors.PrimaryText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.subtitle,
                    color = CineColors.MutedText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.overview,
                    color = CineColors.SoftText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PosterArt(
    item: MediaItem,
    modifier: Modifier = Modifier,
    dynamicAccent: Boolean = false,
    imageWidthPx: Int = 360,
    imageHeightPx: Int = 540,
    loadImage: Boolean = true
) {
    val context = LocalContext.current
    var imageFailed by remember(item.posterUrl) { mutableStateOf(false) }
    val accent = rememberImageAccent(item.posterUrl, item.id, enabled = dynamicAccent && loadImage)
    val posterModel = remember(context, item.posterUrl, imageWidthPx, imageHeightPx, loadImage) {
        item.posterUrl?.takeIf { loadImage }?.let { posterUrl ->
            ImageRequest.Builder(context)
                .data(posterUrl)
                .size(imageWidthPx, imageHeightPx)
                .crossfade(false)
                .build()
        }
    }
    val posterShape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .clip(posterShape)
            .background(Brush.linearGradient(gradientFor(item.id)))
            .border(1.dp, accent.copy(alpha = 0.58f), posterShape)
    ) {
        if (posterModel != null && !imageFailed) {
            AsyncImage(
                model = posterModel,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onError = { state ->
                    imageFailed = true
                    Log.w("CineImages", "Poster failed: ${item.posterUrl}", state.result.throwable)
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, accent.copy(alpha = 0.26f)),
                        startY = 90f
                    )
                )
        )

        if (!loadImage || item.posterUrl == null || imageFailed) {
            Text(
                text = item.title.shortPosterTitle(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
        }
        Icon(
            painter = painterResource(mediaIcon(item.kind)),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(18.dp),
            tint = Color.White.copy(alpha = 0.88f)
        )
    }
}

@Composable
private fun BackdropArt(item: MediaItem, dynamicAccent: Boolean = false) {
    var imageFailed by remember(item.backdropUrl) { mutableStateOf(false) }
    val accent = rememberImageAccent(item.backdropUrl ?: item.posterUrl, item.id, enabled = dynamicAccent)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(gradientFor(item.id)))
    ) {
        if (item.backdropUrl != null && !imageFailed) {
            AsyncImage(
                model = item.backdropUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onError = { state ->
                    imageFailed = true
                    Log.w("CineImages", "Backdrop failed: ${item.backdropUrl}", state.result.throwable)
                }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.28f),
                            Color.Transparent,
                            CineColors.Background.copy(alpha = 0.22f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onActionClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = CineColors.PrimaryText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (action.isNotBlank()) {
            Text(
                text = action,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = onActionClick != null) { onActionClick?.invoke() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = CineColors.Gold,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (selected) CineColors.Gold else CineColors.Card,
        border = BorderStroke(1.dp, if (selected) CineColors.Gold else CineColors.Stroke)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = if (selected) CineColors.OnGold else CineColors.SoftText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TextPill(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = CineColors.Card,
        border = BorderStroke(1.dp, CineColors.Stroke)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = CineColors.SoftText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun IconPill(@DrawableRes icon: Int, text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = CineColors.SoftText,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = text,
                color = CineColors.SoftText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun StatusBadge(status: TrackStatus) {
    Surface(
        shape = RoundedCornerShape(50),
        color = statusColor(status).copy(alpha = 0.16f),
        border = BorderStroke(1.dp, statusColor(status).copy(alpha = 0.45f))
    ) {
        Text(
            text = status.title,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = CineColors.PrimaryText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RatingsStrip(ratings: MediaRatings) {
    if (!ratings.hasAny) return

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { RatingSourceChip(source = "КП", score = ratings.kp) }
        item { RatingSourceChip(source = "IMDb", score = ratings.imdb) }
        item { RatingSourceChip(source = "TMDb", score = ratings.tmdb) }
    }
}

@Composable
private fun RatingSourceChip(source: String, score: Double?) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CineColors.Background.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, CineColors.Stroke)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = source,
                color = CineColors.MutedText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = score.formatRating(),
                color = if (score != null) CineColors.PrimaryText else CineColors.MutedText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun RatingBadge(score: Double, personal: Int? = null, source: String? = null) {
    val isPersonal = personal != null
    val personalColor = personal?.let(::ratingColor) ?: CineColors.Gold
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isPersonal) personalColor else Color(0xFF22252D)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                painter = painterResource(if (isPersonal) R.drawable.ic_star else R.drawable.ic_star),
                contentDescription = null,
                tint = if (isPersonal) ratingContentColor(personal ?: 7) else CineColors.Gold,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = if (isPersonal) {
                    personal.toString()
                } else {
                    listOfNotNull(source, score.takeIf { it > 0.0 }?.let { String.format("%.1f", it) } ?: "—")
                        .joinToString(" ")
                },
                color = if (isPersonal) ratingContentColor(personal ?: 7) else Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun RatingIconButton(personalRating: Int?, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(44.dp)
            .bounceClick()
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = personalRating?.let(::ratingColor) ?: CineColors.Card,
        border = BorderStroke(1.dp, personalRating?.let(::ratingColor) ?: CineColors.Stroke)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (personalRating != null) {
                Text(
                    text = personalRating.toString(),
                    color = ratingContentColor(personalRating),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_star),
                    contentDescription = "Оценить",
                    tint = CineColors.Gold,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun ratingColor(score: Int): Color {
    return when {
        score >= 7 -> Color(0xFF31C84B)
        score >= 5 -> Color(0xFFFFC857)
        else -> Color(0xFFE64D4D)
    }
}

private fun ratingContentColor(score: Int): Color {
    return if (score in 5..6) Color(0xFF15100A) else Color.White
}

private fun Double?.formatRating(): String {
    return this?.takeIf { it > 0.0 }?.let { String.format("%.1f", it) } ?: "—"
}

private fun Int.formatWatchTime(): String {
    val hours = this / 60
    val minutes = this % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}ч ${minutes}м"
        hours > 0 -> "${hours}ч"
        else -> "${minutes}м"
    }
}

private fun Long.formatEventTime(): String {
    return SimpleDateFormat("dd.MM HH:mm", Locale.forLanguageTag("ru-RU")).format(Date(this))
}

@Composable
private fun statusColor(status: TrackStatus): Color = when (status) {
    TrackStatus.Watching -> CineColors.Mint
    TrackStatus.Planned -> CineColors.Gold
    TrackStatus.Watched -> CineColors.Coral
}

@Composable
private fun CineNavigationBar(
    currentRoute: String,
    onTabSelected: (CineTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CineColors.Nav.copy(alpha = 0.97f),
        tonalElevation = 0.dp
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            CineTab.entries.forEach { tab ->
                val selected = currentRoute == tab.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onTabSelected(tab) },
                    icon = {
                        Icon(
                            painter = painterResource(tab.icon),
                            contentDescription = tab.title,
                            modifier = Modifier.size(if (selected) 24.dp else 22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = tab.title,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CineColors.OnGold,
                        selectedTextColor = CineColors.Gold,
                        indicatorColor = CineColors.Gold,
                        unselectedIconColor = CineColors.MutedText,
                        unselectedTextColor = CineColors.MutedText
                    )
                )
            }
        }
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "shimmer"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF1A1F2B),
                Color(0xFF252B3A),
                Color(0xFF1A1F2B),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        ),
        shape = RoundedCornerShape(24.dp)
    ).onGloballyPositioned { size = it.size }
}

fun Modifier.bounceClick(): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "bounce"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CineColors.Background),
        contentAlignment = Alignment.Center
    ) {
        CineLoaderAnimation(modifier = Modifier.size(96.dp))
    }
}

@Composable
private fun LoadingInline() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        CineLoaderAnimation(modifier = Modifier.size(58.dp))
    }
}

@Composable
private fun CineLoaderAnimation(modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.cine_loader))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier
    )
}

@Composable
private fun rememberImageAccent(imageUrl: String?, fallbackId: Int, enabled: Boolean): Color {
    val context = LocalContext.current
    val imageLoader = LocalImageLoader.current
    var accent by remember(imageUrl, fallbackId, enabled) {
        mutableStateOf(gradientFor(fallbackId).getOrElse(1) { Color(0xFFF2C76E) })
    }

    LaunchedEffect(imageUrl, imageLoader, enabled) {
        if (!enabled || imageUrl == null) {
            return@LaunchedEffect
        }

        runCatching {
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .size(160, 160)
                .build()
            val drawable = (imageLoader.execute(request) as? SuccessResult)?.drawable as? BitmapDrawable
            val swatches = drawable?.bitmap?.let { bitmap ->
                Palette.from(bitmap)
                    .maximumColorCount(14)
                    .generate()
            }
            val rgb = swatches?.vibrantSwatch?.rgb
                ?: swatches?.mutedSwatch?.rgb
                ?: swatches?.dominantSwatch?.rgb
            if (rgb != null) {
                accent = Color(rgb)
            }
        }.onFailure { error ->
            Log.w("CinePalette", "Palette failed: $imageUrl", error)
        }
    }

    return accent
}

private fun MediaItem.detailRoute(): String = "detail/${kind.routeValue}/$id"

@DrawableRes
private fun mediaIcon(kind: MediaKind): Int = when (kind) {
    MediaKind.Movie -> R.drawable.ic_movie
    MediaKind.Tv -> R.drawable.ic_tv
}

private fun String.shortPosterTitle(): String {
    val words = split(" ").filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> words.take(2).joinToString("\n") { it.take(6).uppercase() }
        else -> take(10).uppercase()
    }
}

private fun gradientFor(id: Int): List<Color> {
    val palettes = listOf(
        listOf(Color(0xFF0A2F35), Color(0xFF2D6A68), Color(0xFFF2C76E)),
        listOf(Color(0xFF3A2417), Color(0xFFC46B3C), Color(0xFF1D1A1F)),
        listOf(Color(0xFF18362E), Color(0xFF4BB17A), Color(0xFFF7D27A)),
        listOf(Color(0xFF201915), Color(0xFFB94E37), Color(0xFFFFD36B)),
        listOf(Color(0xFF11151B), Color(0xFF335C67), Color(0xFFE09F3E)),
        listOf(Color(0xFF2E2235), Color(0xFF7A4E68), Color(0xFFEFC7A4))
    )
    return palettes[abs(id) % palettes.size]
}

private enum class HomeFilter(val title: String) {
    All("Все"),
    Shows("Сериалы"),
    Movies("Фильмы"),
    Anime("Аниме"),
    New("Новинки")
}

private data class HomeSection(
    val key: String,
    val title: String,
    val items: List<MediaItem>
)

private fun HomeFeed.itemsFor(filter: HomeFilter): List<MediaItem> = when (filter) {
    HomeFilter.All -> (trending + newReleases + popularMovies + popularShows + anime).distinctForUi()
    HomeFilter.Shows -> (trending.filter { it.kind == MediaKind.Tv } + popularShows).distinctForUi()
    HomeFilter.Movies -> (trending.filter { it.kind == MediaKind.Movie } + popularMovies).distinctForUi()
    HomeFilter.Anime -> anime.distinctForUi()
    HomeFilter.New -> newReleases.distinctForUi()
}

private fun HomeFeed.heroFor(filter: HomeFilter): MediaItem? {
    return when (filter) {
        HomeFilter.All -> hero ?: itemsFor(filter).firstOrNull()
        else -> itemsFor(filter).firstOrNull()
    }
}

private fun HomeFeed.sectionsFor(filter: HomeFilter): List<HomeSection> {
    val sections = when (filter) {
        HomeFilter.All -> listOf(
            HomeSection("popular", "Популярное", trending),
            HomeSection("new", "Новинки", newReleases),
            HomeSection("movies", "Фильмы", popularMovies),
            HomeSection("shows", "Сериалы", popularShows),
            HomeSection("anime", "Аниме", anime)
        )
        HomeFilter.Shows -> listOf(
            HomeSection("shows-trending", "Сериалы в тренде", trending.filter { it.kind == MediaKind.Tv }),
            HomeSection("shows-popular", "Популярные сериалы", popularShows),
            HomeSection("shows-new", "Новые серии", newReleases.filter { it.kind == MediaKind.Tv })
        )
        HomeFilter.Movies -> listOf(
            HomeSection("movies-trending", "Фильмы в тренде", trending.filter { it.kind == MediaKind.Movie }),
            HomeSection("movies-popular", "Популярные фильмы", popularMovies),
            HomeSection("movies-new", "Новинки кино", newReleases.filter { it.kind == MediaKind.Movie })
        )
        HomeFilter.Anime -> listOf(HomeSection("anime", "Аниме", anime))
        HomeFilter.New -> listOf(HomeSection("new", "Новинки", newReleases))
    }

    return sections
        .map { it.copy(items = it.items.distinctForUi()) }
        .filter { it.items.isNotEmpty() }
}

private fun HomeFeed.sectionByKey(key: String): HomeSection? {
    return HomeFilter.entries
        .flatMap { sectionsFor(it) }
        .firstOrNull { it.key == key }
}

private fun MediaItem.matches(filter: HomeFilter, allowedKeys: Set<String>): Boolean = when (filter) {
    HomeFilter.All -> true
    HomeFilter.Shows -> kind == MediaKind.Tv
    HomeFilter.Movies -> kind == MediaKind.Movie
    HomeFilter.Anime,
    HomeFilter.New -> mediaKey() in allowedKeys
}

private fun CineUiState.aiRecommendationItems(): List<MediaItem> {
    val feed = homeFeed
    val feedItems = (if (feed == null) {
        searchResults
    } else {
        feed.trending +
            feed.newReleases +
            feed.popularMovies +
            feed.popularShows +
            feed.anime +
            searchResults
    }).distinctForUi()

    if (feedItems.isEmpty()) return emptyList()

    val trackedKeys = trackedTitles.map { it.item.mediaKey() }.toSet()
    val positiveTitles = trackedTitles.filter { tracked ->
        tracked.personalRating?.let { it >= 7 } == true ||
            tracked.status == TrackStatus.Watched ||
            tracked.status == TrackStatus.Watching
    }
    val preferredKind = positiveTitles
        .groupingBy { it.item.kind }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
    val favoriteWords = positiveTitles
        .flatMap { tracked -> tracked.item.title.aiKeywords() + tracked.item.overview.aiKeywords() }
        .toSet()
    val trendingKeys = feed?.trending.orEmpty().map { it.mediaKey() }.toSet()
    val newKeys = feed?.newReleases.orEmpty().map { it.mediaKey() }.toSet()

    val freshCandidates = feedItems.filterNot { it.mediaKey() in trackedKeys }
    val candidates = freshCandidates.ifEmpty { feedItems }

    return candidates
        .sortedByDescending { item ->
            var score = item.ratings.primaryScore.takeIf { it > 0.0 } ?: item.rating
            if (item.kind == preferredKind) score += 1.4
            if (item.mediaKey() in trendingKeys) score += 0.8
            if (item.mediaKey() in newKeys) score += 0.5
            score += item.title.aiKeywords().intersect(favoriteWords).size * 0.35
            if (item.mediaKey() in trackedKeys) score -= 4.0
            score
        }
        .take(10)
}

private fun String.aiKeywords(): Set<String> {
    val stopWords = setOf(
        "фильм",
        "сериал",
        "сезон",
        "история",
        "когда",
        "после",
        "свой",
        "свои",
        "жизнь"
    )
    return lowercase(Locale.forLanguageTag("ru-RU"))
        .split(Regex("[^а-яa-z0-9]+"))
        .filter { it.length >= 4 && it !in stopWords }
        .toSet()
}

private fun MediaItem.mediaKey(): String = "${kind.routeValue}:$id"

private fun List<MediaItem>.distinctForUi(): List<MediaItem> = distinctBy { it.mediaKey() }

private const val LibraryPageSize = 60
private const val AiChatRoute = "ai-chat"

private enum class CineTab(
    val route: String,
    val title: String,
    val icon: Int
) {
    Home("home", "Главная", R.drawable.ic_home),
    Search("search", "Поиск", R.drawable.ic_search),
    Library("library", "Список", R.drawable.ic_bookmark),
    Profile("profile", "Профиль", R.drawable.ic_user)
}

private data class CinePalette(
    val background: Color,
    val card: Color,
    val nav: Color,
    val stroke: Color,
    val primaryText: Color,
    val softText: Color,
    val mutedText: Color,
    val gold: Color,
    val mint: Color,
    val coral: Color,
    val onGold: Color
)

private val LocalCinePalette = staticCompositionLocalOf { paletteFor(CineAppTheme.Cinema) }

private fun paletteFor(theme: CineAppTheme): CinePalette = when (theme) {
    CineAppTheme.Cinema -> CinePalette(
        background = Color(0xFF07080C),
        card = Color(0xFF12151D),
        nav = Color(0xFF0C0F15),
        stroke = Color(0x1FFFFFFF),
        primaryText = Color(0xFFF7F2EA),
        softText = Color(0xFFD9CDBB),
        mutedText = Color(0xFF8B93A1),
        gold = Color(0xFFF5C86A),
        mint = Color(0xFF6FE0B4),
        coral = Color(0xFFFF8A6B),
        onGold = Color(0xFF15100A)
    )
    CineAppTheme.Light -> CinePalette(
        background = Color(0xFFFAF6EF),
        card = Color(0xFFFFFFFF),
        nav = Color(0xFFF4EDE2),
        stroke = Color(0x1F000000),
        primaryText = Color(0xFF171310),
        softText = Color(0xFF44392E),
        mutedText = Color(0xFF83786C),
        gold = Color(0xFFA96F12),
        mint = Color(0xFF13715A),
        coral = Color(0xFFBF4E38),
        onGold = Color(0xFFFFFBF2)
    )
    CineAppTheme.Emerald -> CinePalette(
        background = Color(0xFF050D0A),
        card = Color(0xFF101915),
        nav = Color(0xFF0A120E),
        stroke = Color(0x1F5BE0A5),
        primaryText = Color(0xFFF3F8F0),
        softText = Color(0xFFCBDCC9),
        mutedText = Color(0xFF88998F),
        gold = Color(0xFFDCC069),
        mint = Color(0xFF5BE0A5),
        coral = Color(0xFFFF8F75),
        onGold = Color(0xFF10130B)
    )
}

private object CineColors {
    val Background: Color
        @Composable get() = LocalCinePalette.current.background
    val Card: Color
        @Composable get() = LocalCinePalette.current.card
    val Nav: Color
        @Composable get() = LocalCinePalette.current.nav
    val Stroke: Color
        @Composable get() = LocalCinePalette.current.stroke
    val PrimaryText: Color
        @Composable get() = LocalCinePalette.current.primaryText
    val SoftText: Color
        @Composable get() = LocalCinePalette.current.softText
    val MutedText: Color
        @Composable get() = LocalCinePalette.current.mutedText
    val Gold: Color
        @Composable get() = LocalCinePalette.current.gold
    val Mint: Color
        @Composable get() = LocalCinePalette.current.mint
    val Coral: Color
        @Composable get() = LocalCinePalette.current.coral
    val OnGold: Color
        @Composable get() = LocalCinePalette.current.onGold
}

@Preview(showBackground = true, backgroundColor = 0xFF090A0E)
@Composable
private fun CineTrackerPreview() {
    CineTrackerTheme(dynamicColor = false) {
        CineTrackerApp()
    }
}
