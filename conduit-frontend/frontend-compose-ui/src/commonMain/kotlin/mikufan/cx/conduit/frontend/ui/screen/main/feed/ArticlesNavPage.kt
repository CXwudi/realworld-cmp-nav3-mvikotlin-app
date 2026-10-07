package mikufan.cx.conduit.frontend.ui.screen.main.feed

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import mikufan.cx.conduit.frontend.logic.AppDependencies
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticleDetailViewModel
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticlesListViewModel
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticlesNavRoute
import mikufan.cx.conduit.frontend.logic.component.main.feed.ArticlesNavViewModel

/**
 * Native CMP screen for Feed / Favourite list-detail navigation.
 *
 * Uses Navigation 3 [NavDisplay] with [ArticlesTwoPaneSceneStrategy] to provide adaptive
 * list-detail layout:
 * - Compact width: Single pane (fallback [SinglePaneSceneStrategy]); detail replaces list,
 *   with list preserved in backstack underneath.
 * - Medium / Expanded width: List fills content until detail is opened; when detail is active,
 *   renders 250.dp list on the start side and detail occupying the remainder.
 *
 * Scopes child ViewModels per entry using [rememberSaveableStateHolderNavEntryDecorator]
 * followed by [rememberViewModelStoreNavEntryDecorator].
 */
@Composable
fun ArticlesNavPage(
  articlesNavViewModel: ArticlesNavViewModel,
  dependencies: AppDependencies,
  modifier: Modifier = Modifier,
) {
  val navState by articlesNavViewModel.state.collectAsState()

  val windowAdaptiveInfo = currentWindowAdaptiveInfo()
  val isDualPane = remember(windowAdaptiveInfo) {
    windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
  }

  val sceneStrategy = remember(isDualPane) {
    ArticlesTwoPaneSceneStrategy<ArticlesNavRoute>(isDualPane)
  }

  NavDisplay(
    backStack = navState.stack,
    modifier = modifier,
    onBack = articlesNavViewModel::pop,
    sceneStrategies = listOf(sceneStrategy),
    entryDecorators = listOf(
      rememberSaveableStateHolderNavEntryDecorator(),
      rememberViewModelStoreNavEntryDecorator(),
    ),
    entryProvider = entryProvider {
      entry<ArticlesNavRoute.List>(
        clazzContentKey = { listRoute -> "List_${listRoute.entryId}" },
      ) { listRoute ->
        val listViewModel: ArticlesListViewModel = viewModel(
          key = "ArticlesList_${listRoute.entryId}",
        ) {
          dependencies.articlesListViewModelFactory.create(
            searchFilter = articlesNavViewModel.searchFilter,
            navigator = articlesNavViewModel,
          )
        }
        ArticlesList(listViewModel)
      }

      entry<ArticlesNavRoute.Detail>(
        clazzContentKey = { detailRoute -> "Detail_${detailRoute.entryId}" },
      ) { detailRoute ->
        val detailViewModel: ArticleDetailViewModel = viewModel(
          key = "ArticleDetail_${detailRoute.entryId}",
        ) {
          dependencies.articleDetailViewModelFactory.create(
            basicInfo = detailRoute.basicInfo,
            entryId = detailRoute.entryId,
            navigator = articlesNavViewModel,
          )
        }
        ArticleContent(detailViewModel)
      }
    },
  )
}
