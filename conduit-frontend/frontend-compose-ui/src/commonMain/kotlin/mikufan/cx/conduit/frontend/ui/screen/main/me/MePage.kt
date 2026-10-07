package mikufan.cx.conduit.frontend.ui.screen.main.me

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.window.core.layout.WindowSizeClass
import kotlinx.coroutines.flow.StateFlow
import mikufan.cx.conduit.frontend.logic.component.main.me.MePageIntent
import mikufan.cx.conduit.frontend.logic.component.main.me.MePageState
import mikufan.cx.conduit.frontend.logic.component.main.me.MePageViewModel
import mikufan.cx.conduit.frontend.ui.common.ProfileImage
import mikufan.cx.conduit.frontend.ui.theme.LocalSpace

/**
 * The main composable for the Me page that displays the user's profile using native [MePageViewModel].
 *
 * @param viewModel The native ViewModel managing the Me page's state and logic
 * @param modifier Optional modifier for customizing the layout
 */
@Composable
fun MePage(viewModel: MePageViewModel, modifier: Modifier = Modifier) {
  MePage(
    state = viewModel.state,
    onSend = viewModel::send,
    modifier = modifier,
  )
}

/**
 * Plain state/intent Composable contract for Me page.
 */
@Composable
fun MePage(
  state: StateFlow<MePageState>,
  onSend: (MePageIntent) -> Unit,
  modifier: Modifier = Modifier,
) {
  MePageScaffold(
    onButtonClick = { onSend(MePageIntent.AddArticle) },
  ) { paddingValues ->
    MePageContent(
      stateFlow = state,
      onSend = onSend,
      paddingValues = paddingValues,
      modifier = modifier,
    )
  }
}

/**
 * A scaffold layout for the Me page that includes a floating action button for adding new articles.
 *
 * @param onButtonClick Callback invoked when the floating action button is clicked
 * @param modifier Optional modifier for customizing the layout
 * @param content The main content to display within the scaffold
 */
@Composable
private fun MePageScaffold(
  onButtonClick: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable (PaddingValues) -> Unit
) {
  Scaffold(
    floatingActionButton = {
      FloatingActionButton(onClick = onButtonClick) {
        Icon(Icons.Default.Add, contentDescription = "Write a new post")
      }
    },
    modifier = modifier,
    content = content
  )
}

/**
 * The main content of the Me page that displays the user's profile information and action buttons.
 * Includes loading, error, and loaded states.
 *
 * @param mePageComponent The component that manages the Me page's state and logic
 * @param paddingValues Padding values to apply to the content
 * @param modifier Optional modifier for customizing the layout
 */
@Composable
private fun MePageContent(
  stateFlow: StateFlow<MePageState>,
  onSend: (MePageIntent) -> Unit,
  paddingValues: PaddingValues,
  modifier: Modifier = Modifier,
) {

  val model by stateFlow.collectAsState()

  val parentColumnPadding = PaddingValues(
    top = LocalSpace.current.vertical.padding,
    bottom = max(paddingValues.calculateBottomPadding(), LocalSpace.current.vertical.padding),
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(LocalSpace.current.vertical.spacing),
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(parentColumnPadding)
      .safeDrawingPadding()
      .consumeWindowInsets(parentColumnPadding)
  ) {
    Box {
      when (model) {
        is MePageState.Loading -> {
          CircularProgressIndicator()
        }

        is MePageState.Error -> {
          val errMsg = (model as MePageState.Error).errorMsg
          Text(text = "Failed to load user: $errMsg")
        }

        is MePageState.Loaded -> {
          val model = (model as MePageState.Loaded)
          Profile(
            username = model.username,
            bio = model.bio,
            imageUrl = model.imageUrl
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(LocalSpace.current.vertical.spacingSmall))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(LocalSpace.current.vertical.spacingSmall))

    val editProfileButtonEnabled by remember { derivedStateOf { model is MePageState.Loaded } }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(LocalSpace.current.vertical.spacing),
    ) {
      Button(
        onClick = { onSend(MePageIntent.EditProfile) },
        enabled = editProfileButtonEnabled
      ) {
        Text("Edit Profile")
      }

      Button(onClick = { onSend(MePageIntent.Logout) }) {
        Text("Logout")
      }

      Button(onClick = { onSend(MePageIntent.SwitchServer) }) {
        Text("Switch Server")
      }
    }
  }
}

/**
 * Displays the user's profile information including their image, username, and bio.
 * Adapts its layout based on the window width:
 * - In expanded mode: Shows a horizontal layout with image on the left and details on the right
 * - In compact mode: Shows a vertical layout with image on top and details below
 *
 * @param username The user's display name
 * @param bio The user's biography text
 * @param imageUrl URL of the user's profile image
 * @param modifier Optional modifier for customizing the layout
 */
@Composable
private fun Profile(
  username: String,
  bio: String,
  imageUrl: String,
  modifier: Modifier = Modifier
) {
  val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
  val shouldExpand by remember(windowSizeClass) {
    derivedStateOf { windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) }
  }


  if (shouldExpand) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(LocalSpace.current.horizontal.spacing * 6),
      modifier = modifier.padding(horizontal = LocalSpace.current.horizontal.padding)
    ) {
      ProfileImage(imageUrl = imageUrl, username = username, size = 120.dp)
      Column(
        verticalArrangement = Arrangement.spacedBy(LocalSpace.current.vertical.spacing * 2),
        modifier = modifier
          .widthIn(max = LocalSpace.current.horizontal.maxContentSpace)
          .padding(vertical = LocalSpace.current.vertical.padding)
      ) {
        Text(
          text = username,
          style = MaterialTheme.typography.headlineMedium
        )
        ExpandableBioText(
          bio = bio,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  } else {
    Column(
      verticalArrangement = Arrangement.spacedBy(LocalSpace.current.vertical.spacing * 2),
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = modifier
        .widthIn(max = LocalSpace.current.horizontal.maxContentSpace)
        .padding(vertical = LocalSpace.current.vertical.padding)
    ) {
      ProfileImage(imageUrl = imageUrl, username = username, size = 120.dp)
      Spacer(modifier = Modifier.height(LocalSpace.current.vertical.spacing * 2)) // so this is doubled the spacing
      Text(
        text = username,
        style = MaterialTheme.typography.headlineMedium,
        modifier = modifier.padding(horizontal = LocalSpace.current.horizontal.padding)
      )
      ExpandableBioText(
        bio = bio,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = LocalSpace.current.horizontal.padding)
      )
    }
  }
}

/**
 * A text component that displays bio text with expand/collapse functionality.
 * Shows truncated text with a "Show more" button when the text exceeds the available space,
 * and provides smooth spring animation when expanding/collapsing.
 *
 * @param bio The biography text to display
 * @param style The text style to apply
 * @param color The text color
 * @param modifier Optional modifier for customizing the layout
 * @param textButtonPadding Padding to apply to the expand/collapse button
 * @param collapsedMaxLines Maximum number of lines to show when collapsed (default: 3)
 */
@Composable
private fun ExpandableBioText(
  bio: String,
  style: TextStyle,
  color: Color,
  modifier: Modifier = Modifier,
  textButtonPadding: PaddingValues = PaddingValues(0.dp),
  collapsedMaxLines: Int = 3
) {
  var isExpanded by remember { mutableStateOf(false) }
  var isOverflowing by remember { mutableStateOf(false) }
  val textButtonText by remember { derivedStateOf { if (isExpanded) "Collapse" else "Show more" } }

  Column(
    // we want the bio and the button to be closer
    verticalArrangement = Arrangement.spacedBy(-LocalSpace.current.vertical.spacingSmall),
  ) {
    Text(
      text = bio,
      style = style,
      color = color,
      modifier = modifier.animateContentSize(
        animationSpec = spring(
          stiffness = Spring.StiffnessMedium,
          visibilityThreshold = IntSize.VisibilityThreshold,
        )
      ),
      maxLines = if (isExpanded) Int.MAX_VALUE else collapsedMaxLines,
      overflow = TextOverflow.Ellipsis,
      onTextLayout = { layoutResult ->
        // Only update overflow state when collapsed to prevent it from becoming false on expand
        if (!isExpanded) {
          isOverflowing = layoutResult.didOverflowHeight
        }
      }
    )
    // Show the button if the text is known to overflow (even when expanded)
    if (isOverflowing) {
      TextButton(
        onClick = { isExpanded = !isExpanded },
        contentPadding = textButtonPadding,
        modifier = modifier
      ) {
        AnimatedContent(
          targetState = textButtonText,
        ) {
          Text(
            text = it,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Left,
          )
        }
      }
    }
  }
}

