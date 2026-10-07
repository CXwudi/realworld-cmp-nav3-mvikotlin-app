package mikufan.cx.conduit.frontend.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner

/**
 * Ensures that [LocalViewModelStoreOwner] and [LocalSavedStateRegistryOwner] are provided
 * at root level across all platforms.
 *
 * - Android: pass-through since ComponentActivity hosts both owners natively.
 * - Non-Android (Desktop, Web, iOS): provides root owners with correct lifecycle ordering
 *   (INITIALIZED -> attach/restore -> RESUMED) and clean teardown to DESTROYED on disposal.
 */
@Composable
expect fun ProvideRootCompositionLocals(content: @Composable () -> Unit)

/**
 * Shared non-Android root composition locals provider supplying a default
 * [ViewModelStoreOwner] and [SavedStateRegistryOwner] with correct lifecycle ordering.
 */
@Composable
fun DefaultRootCompositionLocalsProvider(content: @Composable () -> Unit) {
  val holder = remember { RootOwnersHolder() }

  DisposableEffect(holder) {
    onDispose {
      holder.dispose()
    }
  }

  CompositionLocalProvider(
    LocalViewModelStoreOwner provides holder.viewModelStoreOwner,
    LocalSavedStateRegistryOwner provides holder.savedStateRegistryOwner,
    content = content,
  )
}

private class DefaultSavedStateRegistryOwner : SavedStateRegistryOwner {
  override val lifecycle: Lifecycle
    field = LifecycleRegistry(this)

  private val controller = SavedStateRegistryController.create(this)
  override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

  init {
    // 1. Controller attach must happen when lifecycle is INITIALIZED
    controller.performAttach()
    // 2. Controller restore must happen before lifecycle moves to STARTED
    controller.performRestore(null)
    // 3. Move to CREATED -> STARTED -> RESUMED
    lifecycle.currentState = Lifecycle.State.RESUMED
  }

  fun teardown() {
    lifecycle.currentState = Lifecycle.State.DESTROYED
  }
}

private class RootOwnersHolder {
  val viewModelStoreOwner = object : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
  }

  val savedStateRegistryOwner = DefaultSavedStateRegistryOwner()

  fun dispose() {
    savedStateRegistryOwner.teardown()
    viewModelStoreOwner.viewModelStore.clear()
  }
}
