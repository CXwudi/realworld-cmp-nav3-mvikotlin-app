package mikufan.cx.conduit.frontend.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

/**
 * Defines the spacing and padding system for the application UI.
 *
 * This file contains the data structures and utilities for managing consistent spacing
 * throughout the application. The spacing system adapts to different window sizes
 * (compact, medium, expanded) for both horizontal and vertical dimensions.
 *
 * The spacing system is provided through [LocalSpace] composition local and can be accessed
 * in composables via `LocalSpace.current`.
 */

/**
 * Container for both horizontal and vertical spacing configurations.
 *
 * This class combines [HorizontalSpace] and [VerticalSpace] to provide a complete
 * spacing system that can be used throughout the application.
 *
 * @property horizontal The horizontal spacing configuration
 * @property vertical The vertical spacing configuration
 */
data class Space(
  val horizontal: HorizontalSpace,
  val vertical: VerticalSpace,
)

/**
 * Defines horizontal spacing and padding values for UI components.
 *
 * Provides a set of standardized spacing and padding values for horizontal layouts.
 * These values are used for consistent spacing between and within UI components.
 *
 * @property spacing Standard horizontal spacing between UI elements
 * @property spacingSmall Smaller horizontal spacing for tighter layouts
 * @property spacingLarge Larger horizontal spacing for more spacious layouts
 * @property padding Standard horizontal padding within UI containers
 * @property paddingSmall Smaller horizontal padding for compact containers
 * @property paddingLarge Larger horizontal padding for spacious containers
 * @property maxContentSpace Maximum width for content containers (default: 600dp)
 */
data class HorizontalSpace(
  val spacing: Dp,
  val spacingSmall: Dp,
  val spacingLarge: Dp,
  val padding: Dp,
  val paddingSmall: Dp,
  val paddingLarge: Dp,
  val maxContentSpace: Dp = 600.dp
)

/**
 * Defines vertical spacing and padding values for UI components.
 *
 * Provides a set of standardized spacing and padding values for vertical layouts.
 * These values are used for consistent spacing between and within UI components.
 *
 * @property spacing Standard vertical spacing between UI elements
 * @property spacingSmall Smaller vertical spacing for tighter layouts
 * @property spacingLarge Larger vertical spacing for more spacious layouts
 * @property padding Standard vertical padding within UI containers
 * @property paddingSmall Smaller vertical padding for compact containers
 * @property paddingLarge Larger vertical padding for spacious containers
 */
data class VerticalSpace(
  val spacing: Dp,
  val spacingSmall: Dp,
  val spacingLarge: Dp,
  val padding: Dp,
  val paddingSmall: Dp,
  val paddingLarge: Dp,
)

/**
 * CompositionLocal to provide [Space] values down the composition tree.
 *
 * This allows any composable to access the current spacing configuration by using
 * `LocalSpace.current`. The spacing configuration is typically provided at the top level
 * of the application using [CompositionLocalProvider] in conjunction with [WithProperSize].
 *
 * @throws IllegalStateException if accessed without a provider in the composition
 */
val LocalSpace = compositionLocalOf<Space> { error("No LocalSpace provided") }

/**
 * Predefined [HorizontalSpace] configuration for compact window widths.
 *
 * Uses smaller spacing and padding values suitable for narrow screens like phones
 * in portrait orientation.
 */
private val compactHorizontalSpace = HorizontalSpace(
  spacing = 3.dp,
  spacingSmall = 2.dp,
  spacingLarge = 4.dp,
  padding = 3.dp,
  paddingSmall = 2.dp,
  paddingLarge = 4.dp
)

/**
 * Predefined [HorizontalSpace] configuration for medium window widths.
 *
 * Uses moderate spacing and padding values suitable for tablets or phones
 * in landscape orientation.
 */
private val mediumHorizontalSpace = HorizontalSpace(
  spacing = 4.dp,
  spacingSmall = 3.dp,
  spacingLarge = 6.dp,
  padding = 4.dp,
  paddingSmall = 3.dp,
  paddingLarge = 6.dp
)

/**
 * Predefined [HorizontalSpace] configuration for expanded window widths.
 *
 * Uses larger spacing and padding values suitable for desktop or large tablet screens.
 */
private val expandedHorizontalSpace = HorizontalSpace(
  spacing = 6.dp,
  spacingSmall = 4.dp,
  spacingLarge = 8.dp,
  padding = 6.dp,
  paddingSmall = 4.dp,
  paddingLarge = 8.dp
)

/**
 * Predefined [VerticalSpace] configuration for compact window heights.
 *
 * Uses smaller spacing and padding values suitable for devices with limited
 * vertical space, like phones in landscape orientation.
 */
private val compactVerticalSpace = VerticalSpace(
  spacing = 3.dp,
  spacingSmall = 2.dp,
  spacingLarge = 4.dp,
  padding = 3.dp,
  paddingSmall = 2.dp,
  paddingLarge = 4.dp
)

/**
 * Predefined [VerticalSpace] configuration for medium window heights.
 *
 * Uses moderate spacing and padding values suitable for most devices
 * in their standard orientation.
 */
private val medianVerticalSpace = VerticalSpace(
  spacing = 4.dp,
  spacingSmall = 3.dp,
  spacingLarge = 6.dp,
  padding = 4.dp,
  paddingSmall = 3.dp,
  paddingLarge = 6.dp
)

/**
 * Predefined [VerticalSpace] configuration for expanded window heights.
 *
 * Uses larger spacing and padding values suitable for devices with ample
 * vertical space, like desktop monitors or large tablets.
 */
private val expandedVerticalSpace = VerticalSpace(
  spacing = 6.dp,
  spacingSmall = 4.dp,
  spacingLarge = 8.dp,
  padding = 6.dp,
  paddingSmall = 4.dp,
  paddingLarge = 8.dp
)

/**
 * Utility object for calculating appropriate spacing based on window size.
 *
 * Provides methods to determine the appropriate spacing configuration based on
 * the current window size class, ensuring consistent spacing across different
 * device form factors.
 */
object SpacingDefaults {
  /**
   * Calculates the appropriate spacing configuration based on window size class.
   *
   * This method determines the horizontal spacing based on window width and
   * vertical spacing based on window height, then combines them into a single
   * [Space] object that can be used throughout the application.
   *
   * @param windowSizeClass The current window size classification
   * @return A [Space] object with appropriate horizontal and vertical spacing values
   */
  fun calculateSpacing(windowSizeClass: WindowSizeClass): Space {
    val horizontalSpace = when {
      windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> expandedHorizontalSpace
      windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> mediumHorizontalSpace
      else -> compactHorizontalSpace
    }

    val verticalSpace = when {
      windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND) -> expandedVerticalSpace
      windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) -> medianVerticalSpace
      else -> compactVerticalSpace
    }

    return Space(horizontal = horizontalSpace, vertical = verticalSpace)
  }
}
