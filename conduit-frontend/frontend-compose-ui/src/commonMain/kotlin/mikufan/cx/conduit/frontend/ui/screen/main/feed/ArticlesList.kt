package mikufan.cx.conduit.frontend.ui.screen.main.feed

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.compose.rememberConstraintsSizeResolver
import coil3.request.ImageRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticleBasicInfo
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticleInfo
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticlesListIntent
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticlesListLabel
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticlesListState
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticlesListViewModel
import mikufan.cx.conduit.frontend.logic.component.main.feed.LoadMoreState
import mikufan.cx.conduit.frontend.ui.common.BouncingDotsLoading
import mikufan.cx.conduit.frontend.ui.common.ProfileImage
import mikufan.cx.conduit.frontend.ui.theme.LocalSpace

@Composable
fun ArticlesList(
  viewModel: ArticlesListViewModel,
  modifier: Modifier = Modifier,
) {
  ArticlesList(
    state = viewModel.state,
    labels = viewModel.labels,
    onSend = viewModel::send,
    modifier = modifier,
  )
}

/**
 * Plain state/intent Composable contract for Articles list.
 */
@Composable
fun ArticlesList(
  state: StateFlow<ArticlesListState>,
  labels: Flow<ArticlesListLabel>,
  onSend: (ArticlesListIntent) -> Unit,
  modifier: Modifier = Modifier,
) {
  val articlesListState = state.collectAsState()
  val collectedThumbInfosState = remember {
    derivedStateOf { articlesListState.value.collectedThumbInfos }
  }
  val loadMoreStateState = remember {
    derivedStateOf { articlesListState.value.loadMoreState }
  }

  val gridState = rememberLazyGridState()

  ArticlesListLoadEffect(
    onLoadMore = { onSend(ArticlesListIntent.LoadMore) },
    itemsState = collectedThumbInfosState,
    loadStateState = loadMoreStateState,
    gridState = gridState,
  )
  ArticlesListGrid(
    itemsState = collectedThumbInfosState,
    loadStateState = loadMoreStateState,
    gridState = gridState,
    onItemClick = { detail -> onSend(ArticlesListIntent.ClickOnArticle(detail)) },
    modifier = modifier,
  )

  // Handle error label: label and show error message as pop up
  ArticlesListErrorAlert(labels)
}

@Composable
private fun ArticlesListLoadEffect(
  onLoadMore: () -> Unit,
  itemsState: State<List<ArticleInfo>>,
  loadStateState: State<LoadMoreState>,
  gridState: LazyGridState
) {
  val reachLoadThresholdState = remember {
    derivedStateOf {
      val info = gridState.layoutInfo.visibleItemsInfo
      // use itemsState.value.lastIndex instead of itemsState.value.size -1 to avoid IndexOutOfBoundsException when list is empty
      info.isEmpty() || (itemsState.value.isNotEmpty() && info.last().index > itemsState.value.lastIndex - 5)
    }
  }

  // TODO: handle loaded all articles: add loaded all state enum
  // this requires changes in store where if the returned list is empty,
  // set to a new state, LoadedAll
  // LoadedAll != Loaded so it effectively stop the LaunchedEffect


  // start loading upon the first visit
  LaunchedEffect(Unit) {
    onLoadMore()
  }

  // launch effect for loading more
  // use reachLoadMoreThreshold and if statement with reachLoadMoreThreshold
  // to check if the user is about to reach the end of the list, so that we can start loading more.
  // use size to check if loading once is not enough, and we still reachLoadMoreThreshold,
  // so that we can start loading more again.
  // until reachLoadMoreThreshold becomes false
  // or loading more returns 0 items, meaning we loaded all items
  LaunchedEffect(
    reachLoadThresholdState.value,
    itemsState.value.size // Use size here as a trigger for re-evaluation when new items are loaded
  ) {
    // guarding with gridState.layoutInfo.visibleItemsInfo.isNotEmpty()
    // so that the first launch effect will do the initial load.
    // although we could merge two launch effects by simply removing this guard.
    if (loadStateState.value == LoadMoreState.Loaded
      && reachLoadThresholdState.value
      && gridState.layoutInfo.visibleItemsInfo.isNotEmpty()
    ) {
      // must make sure that when this function returned,
      // the state is already not ArticlesListIntent.Loaded
      onLoadMore()
    }
  }
}

@Composable
private fun ArticlesListGrid(
  itemsState: State<List<ArticleInfo>>,
  loadStateState: State<LoadMoreState>,
  gridState: LazyGridState,
  onItemClick: (ArticleBasicInfo) -> Unit,
  modifier: Modifier = Modifier,
) {
  val space = LocalSpace.current
  val safePadding = WindowInsets.safeDrawing.asPaddingValues()
  val layoutDir = LocalLayoutDirection.current

  val isLoadingState = remember {
    derivedStateOf { loadStateState.value == LoadMoreState.Loading }
  }

  val contentPaddingState = remember {
    derivedStateOf {
      PaddingValues(
        top = space.vertical.padding + safePadding.calculateTopPadding(),
        bottom = space.vertical.padding + safePadding.calculateBottomPadding(),
        start = space.horizontal.padding + safePadding.calculateStartPadding(layoutDir),
        end = space.horizontal.padding + safePadding.calculateEndPadding(layoutDir)
      )
    }
  }

  LazyVerticalGrid(
    state = gridState,
    modifier = modifier,
    columns = GridCells.Adaptive(minSize = space.horizontal.maxContentSpace / 2),
    horizontalArrangement = Arrangement.spacedBy(space.horizontal.spacing),
    verticalArrangement = Arrangement.spacedBy(space.vertical.spacing),
    contentPadding = contentPaddingState.value
  ) {
    items(
      items = itemsState.value,
      key = { "${it.slug}${it.createdAt}" },
      contentType = { "article" }
    ) { item ->
      ArticleCard(
        article = item,
        modifier = Modifier.clickable { onItemClick(item.toBasicInfo()) }
      )
    }
    if (isLoadingState.value) {
      item(
        key = "loading-more",
        span = { GridItemSpan(maxLineSpan) },
        contentType = "loading-more"
      ) {
        BouncingDotsLoading()
      }
    }
  }
}

@Composable
private fun ArticleCard(
  article: ArticleInfo,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .height(LocalSpace.current.horizontal.maxContentSpace / 3.33f),
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(LocalSpace.current.vertical.spacingSmall),
      modifier = Modifier.padding(
        horizontal = LocalSpace.current.horizontal.padding,
        vertical = LocalSpace.current.vertical.padding
      )
    ) {
      // Top row with author thumbnail and username
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalSpace.current.horizontal.spacingSmall)
      ) {
        ProfileImage(
          imageUrl = article.authorThumbnail,
          username = article.authorUsername,
          size = 40.dp
        )
        Text(
          text = article.authorUsername,
          style = MaterialTheme.typography.bodySmall
        )
      }

      // Article title - max 2 lines
      Text(
        text = article.title,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      // Article description - max 3 lines
      Text(
        text = article.description,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
      )

      // Spacer to push the date to the bottom
      Spacer(modifier = Modifier.weight(1f))

      // Date at the bottom
      Text(
        text = formatDate(article.createdAt),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

/**
 * Formats an Instant to a human-readable date string
 */
private fun formatDate(instant: Instant): String {
  val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
  val month = when (localDateTime.month.number) {
    1 -> "Jan"
    2 -> "Feb"
    3 -> "Mar"
    4 -> "Apr"
    5 -> "May"
    6 -> "Jun"
    7 -> "Jul"
    8 -> "Aug"
    9 -> "Sep"
    10 -> "Oct"
    11 -> "Nov"
    12 -> "Dec"
    else -> "Unknown"
  }
  return "$month ${localDateTime.day}, ${localDateTime.year}"
}


@Composable
fun ArticlesListErrorAlert(articlesListLabelFlow: Flow<ArticlesListLabel>) {
  val errorMsgState = remember { mutableStateOf("") }
  val showErrorAlert by remember { derivedStateOf { errorMsgState.value.isNotBlank() } }

  val scope = rememberCoroutineScope()
  scope.launch {
    articlesListLabelFlow.collect { label ->
      when (label) {
        is ArticlesListLabel.Failure -> {
          errorMsgState.value = label.message
        }
        else -> {}
      }
    }
  }

  if (showErrorAlert) {
    AlertDialog(
      onDismissRequest = { errorMsgState.value = "" },
      shape = MaterialTheme.shapes.large,
      tonalElevation = LocalSpace.current.vertical.spacingLarge,
      icon = {
        Icon(
          imageVector = Icons.Filled.Warning,
          contentDescription = "Error Icon",
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(LocalSpace.current.vertical.spacingLarge * 4)
        )
      },
      title = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "Error",
            style = MaterialTheme.typography.headlineSmall
          )
        }
      },
      text = {
        val scrollState = rememberScrollState()
        Text(
          text = errorMsgState.value,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier
            .heightIn(max = 600.dp)
            .verticalScroll(scrollState)
        )
      },
      confirmButton = {
        TextButton(
          onClick = { errorMsgState.value = "" }
        ) {
          Text("OK")
        }
      },
    )
  }
}
