# Compose UI

The `frontend-compose-ui` module implements the UI layer with Compose Multiplatform, using Jetpack Navigation 3, adaptive layouts, and native ViewModels.

## Navigation & Entry Ownership

1. **Navigation 3 `NavDisplay`**:
   - Navigation flows (Root navigation, Main navigation tabs, Feed/Favourite list-detail, and Me navigation) use Navigation 3's `NavDisplay`.
   - **Decorator Order**: Decorators are applied in strict order:

     ```kotlin
     entryDecorators = listOf(
       rememberSaveableStateHolderNavEntryDecorator(),
       rememberViewModelStoreNavEntryDecorator(),
     )
     ```

     This order guarantees state save/restore decorators wrap ViewModel store owners properly.
   - **Entry Initializer Pattern**: Inside each `entry<T>`, ViewModels are retrieved using the assisted factory inside the `viewModel` initializer block:

     ```kotlin
     entry<MyRoute>(clazzContentKey = { route -> route.key }) { route ->
       val vm: MyViewModel = viewModel(key = "my_vm_${route.key}") {
         val savedStateHandle = createSavedStateHandle()
         dependencies.myViewModelFactory.create(savedStateHandle, ...)
       }
       MyScreen(vm)
     }
     ```

   - **Back Navigation**: `NavDisplay` uses its direct `onBack` parameter (e.g., `onBack = meNavViewModel::pop`) to route system or stack back events directly to the owning navigator ViewModel.
2. **Adaptive List-Detail Scene**:
   - `ArticlesTwoPaneSceneStrategy` switches between a two-pane layout (250dp list pane on wide screens) and a single-pane fallback on compact screens.
   - Retains active ViewModel instances across resize/rotation and clears removed entries.

### Navigation 3 dependency compatibility

Navigation 3 remains on `1.1.1`. Version `1.1.2` leaves the list blank after switching from two panes to compact mode and popping the detail entry, as covered by `ArticlesNavDisplayOwnershipTest`. The failure persists after waiting for the destination to appear; reverting only Navigation 3 restores all six JVM UI tests with the other dependency updates retained. Renovate excludes only `1.1.2`, allowing later releases to be tested normally. Keep the ownership test enabled when evaluating those releases.

## Root Composition Locals

Platforms configure root owners consistently:

- On Android, the hosting Activity provides AndroidX architecture component owners natively.
- On non-Android targets (Desktop and Web), `DefaultRootCompositionLocalsProvider` supplies explicit, consistent root `LocalViewModelStoreOwner` and `LocalSavedStateRegistryOwner` instances via `RootOwnersHolder`.
- On Web, `ComposeViewport` natively maps browser focus and document visibility to `LocalLifecycleOwner`.

## Local Utils

Spacing and padding settings are defined in the [`LocalSpace`](../../conduit-frontend/frontend-compose-ui/src/commonMain/kotlin/mikufan/cx/conduit/frontend/ui/theme/Space.kt) composition local. When defining spacing and padding using `.dp` units, always use values from `LocalSpace` (or formulas derived from them) instead of hardcoding raw values.

## UI Guidance

### Padding

The root Composable only contains a `Surface` with `fillMaxSize()` modifier and `background` color set to `MaterialTheme.colorScheme.background` (see [`MainUI.kt`](../../conduit-frontend/frontend-compose-ui/src/commonMain/kotlin/mikufan/cx/conduit/frontend/ui/MainUI.kt)), so there is no default padding from the root Composable. Each screen must specify its own padding.

When using layouts such as `Column`, `Row`, `LazyVerticalGrid`, etc., first specify base spacing using built-in parameters (`verticalArrangement` and `horizontalArrangement`). Use `Spacer` with `LocalSpace` if you need custom spacing between specific items.

For `Column` and `Row`, prefer to only set padding in the primary direction. For example, a `Row` should set `modifier.padding(horizontal = LocalSpace.current...)` and a `Column` should set `modifier.padding(vertical = LocalSpace.current...)`. This provides maximum layout flexibility. If a single page uses a `Column` layout and requires padding on all 4 sides, the `Column` itself sets vertical padding, while horizontal padding is applied on child Composables or wrapping `Box`/`Row` elements.

When using lazy layouts (`LazyRow`, `LazyColumn`, `LazyVerticalGrid`), prefer `contentPadding` over `modifier.padding()` so content can scroll underneath system bars and navigation rails. `LazyRow` sets horizontal content padding, `LazyColumn` sets vertical content padding, and `LazyVerticalGrid` sets both horizontal and vertical content padding.

### Edge to Edge (WindowInsets)

The Android app enables `enableEdgeToEdge()` and does not use a single global WindowInsets padding on the root Composable. Instead, each screen manages its own WindowInsets padding (typically `WindowInsets.safeDrawing` and `WindowInsets.ime`).

When applying `WindowInsets` padding, check whether parent layouts already apply padding (e.g., `Scaffold` inner padding). If so, use `consumeWindowInsets` so insets are not double-counted.

For lazy layouts with a `contentPadding` parameter, combine `WindowInsets` and `LocalSpace` padding by using `.asPaddingValues()` (e.g. `WindowInsets.safeDrawing.asPaddingValues()`) and computing `max()` with `LocalSpace` padding on each dimension.

## State Observation & Previews

### `State<T>` and `derivedStateOf` Usage

When observing ViewModel state in Composables, retrieve the state using `val state by viewModel.state.collectAsState()`.
Any sub-field retrieved from state should use `remember` and `derivedStateOf` to prevent unnecessary recomposition:

```kotlin
val emailState: State<String> = remember { derivedStateOf { state.email } }
```

When passing retrieved fields to child Composables, prefer passing the `State<T>` variable rather than the raw value `T`. This allows Compose to skip recomposition of parent Composables when only the child reading `State<T>` needs updating.

### Preview Contracts

Screens expose lightweight plain state/intent Composable overloads (e.g. `LandingPage(state, labels, onSend)`, `MainNavScaffold(state, onSend) { ... }`, `AuthPage(stateFlow, labelsFlow, onSend)`). Tooling previews and UI snapshot tests instantiate these contracts directly with `MutableStateFlow`, avoiding mock DI graphs or synthetic ViewModel instances.
