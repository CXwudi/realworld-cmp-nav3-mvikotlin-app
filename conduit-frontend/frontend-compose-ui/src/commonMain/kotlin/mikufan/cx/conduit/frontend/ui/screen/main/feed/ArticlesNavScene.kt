package mikufan.cx.conduit.frontend.ui.screen.main.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/**
 * Custom two-pane [Scene] rendering a fixed 250.dp list on the start side and the remainder detail on the end side.
 *
 * Guarantees:
 * - [entries] contains both [listEntry] and [detailEntry].
 * - [previousEntries] contains only [listEntry] on back.
 * - Each entry is invoked at most once within the scene.
 * - Stable equality and hashcode based on data class properties.
 */
@Immutable
data class ArticlesTwoPaneScene<T : Any>(
  override val key: Any,
  val listEntry: NavEntry<T>,
  val detailEntry: NavEntry<T>,
  override val previousEntries: List<NavEntry<T>> = listOf(listEntry),
) : Scene<T> {
  override val entries: List<NavEntry<T>> = listOf(listEntry, detailEntry)

  override val content: @Composable () -> Unit = {
    Row(modifier = Modifier.fillMaxSize()) {
      Box(modifier = Modifier.width(250.dp).fillMaxHeight()) {
        listEntry.Content()
      }
      Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
        detailEntry.Content()
      }
    }
  }
}

/**
 * Strategy calculating whether to display a two-pane layout or fall back to single pane.
 *
 * If [isDualPane] is true and at least two entries exist (list + detail), produces [ArticlesTwoPaneScene].
 * Otherwise, returns null to let NavDisplay fall back to standard SinglePaneSceneStrategy.
 */
@Immutable
class ArticlesTwoPaneSceneStrategy<T : Any>(
  val isDualPane: Boolean,
) : SceneStrategy<T> {
  override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
    if (!isDualPane || entries.size < 2) return null
    val listEntry = entries.first()
    val detailEntry = entries.last()
    return ArticlesTwoPaneScene(
      key = "TwoPane_${listEntry.contentKey}_${detailEntry.contentKey}",
      listEntry = listEntry,
      detailEntry = detailEntry,
      previousEntries = listOf(listEntry),
    )
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ArticlesTwoPaneSceneStrategy<*>) return false
    return isDualPane == other.isDualPane
  }

  override fun hashCode(): Int = isDualPane.hashCode()
}
