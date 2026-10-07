package mikufan.cx.conduit.frontend.logic.component.main

import com.arkivanov.mvikotlin.core.store.Bootstrapper
import com.arkivanov.mvikotlin.core.store.Executor
import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.coroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.coroutineExecutorFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigKStore
import mikufan.cx.conduit.frontend.logic.repo.kstore.UserConfigState

class MainNavStoreFactory(
  private val storeFactory: StoreFactory,
  private val userConfigKStore: UserConfigKStore,
  private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) {

  fun createStore(
    initState: MainNavState = MainNavState.notLoggedIn(),
    restoredSnapshot: MainNavSavedSnapshot? = null,
    autoInit: Boolean = true
  ): Store<MainNavIntent, MainNavState, Nothing> {
    val initialGeneration = restoredSnapshot?.generation ?: 0L
    val effectiveInitState = initState.with(
      isReady = false,
      generation = initialGeneration,
    )

    val executor = createExecutor(restoredSnapshot)

    val reducer = createReducer()

    return storeFactory.create(
      name = "MainNavStore",
      autoInit = autoInit,
      initialState = effectiveInitState,
      bootstrapper = createBootstrapper(),
      executorFactory = executor,
      reducer = reducer,
    )
  }

  private fun createReducer(): Reducer<MainNavState, Msg> = Reducer<MainNavState, Msg> { msg ->
    when (msg) {
      is Msg.Reconcile -> MainNavState.notLoggedIn().with(
        menuItems = msg.menuItems,
        pageIndex = msg.pageIndex,
        isReady = true,
        generation = msg.generation,
      )

      is Msg.LiveSwitchToNotLoggedIn -> MainNavState.notLoggedIn(
        pageIndex = 0,
        isReady = true,
        generation = msg.newGeneration,
      )

      is Msg.LiveSwitchToLoggedIn -> MainNavState.loggedIn(
        username = msg.username,
        pageIndex = 0,
        isReady = true,
        generation = msg.newGeneration,
      )

      is Msg.MenuIndexSwitching -> with(
        pageIndex = msg.targetIndex,
        generation = msg.newGeneration,
      )
    }
  }

  private fun createExecutor(restoredSnapshot: MainNavSavedSnapshot?): () -> Executor<MainNavIntent, Action, MainNavState, Msg, Nothing> =
    coroutineExecutorFactory<MainNavIntent, Action, MainNavState, Msg, Nothing>(dispatcher) {
      onAction<Action.UserConfigEmitted> { action ->
        val currentState = state()
        if (!currentState.isReady) {
          val msg = reconcileInitialState(action.userConfig, restoredSnapshot)
          log.info { "Reconciled initial MainNav state: isReady=true, pageIndex=${msg.pageIndex}, generation=${msg.generation}" }
          dispatch(msg)
        } else {
          when (val userConfig = action.userConfig) {
            is UserConfigState.Landing,
            is UserConfigState.OnUrl -> {
              if (currentState.isLoggedIn) {
                log.info { "User logged out; resetting MainNav to guest Feed" }
                dispatch(Msg.LiveSwitchToNotLoggedIn(newGeneration = currentState.generation + 1L))
              }
            }

            is UserConfigState.OnLogin -> {
              val targetUsername = userConfig.userInfo.username
              if (!currentState.isLoggedIn || currentState.currentUsername != targetUsername) {
                log.info { "User switched/logged in as $targetUsername; resetting to Feed" }
                dispatch(
                  Msg.LiveSwitchToLoggedIn(
                    targetUsername,
                    newGeneration = currentState.generation + 1L
                  )
                )
              }
            }
          }
        }
      }

      onIntent<MainNavIntent.MenuIndexSwitching> { intent ->
        val currentState = state()
        if (!currentState.isReady) {
          log.warn { "Ignored MenuIndexSwitching intent before MainNav is reconciled" }
          return@onIntent
        }

        val targetIndex = intent.targetIndex
        require(targetIndex in 0 until currentState.menuItems.size) {
          "Target index $targetIndex is out of bounds. Valid range: 0-${currentState.menuItems.size - 1}, menuItems: ${currentState.menuItems}"
        }
        if (currentState.pageIndex != targetIndex) {
          log.info { "Switching to page at index $targetIndex" }
          dispatch(
            Msg.MenuIndexSwitching(
              targetIndex,
              newGeneration = currentState.generation + 1L
            )
          )
        }
      }
    }

  private fun createBootstrapper(): Bootstrapper<Action> =
    coroutineBootstrapper(dispatcher) {
      launch {
        userConfigKStore.userConfigFlow.collect { userConfigState ->
          dispatch(Action.UserConfigEmitted(userConfigState))
        }
      }
    }

  private fun reconcileInitialState(
    userConfig: UserConfigState,
    restoredSnapshot: MainNavSavedSnapshot?,
  ): Msg.Reconcile {
    val (authoritativeUsername, menuItems) = when (userConfig) {
      is UserConfigState.OnLogin -> userConfig.userInfo.username to MainNavState.loggedIn(userConfig.userInfo.username).menuItems
      is UserConfigState.Landing,
      is UserConfigState.OnUrl -> null to MainNavState.notLoggedIn().menuItems
    }

    val matchingIndex = if (restoredSnapshot != null && restoredSnapshot.accountUsername == authoritativeUsername) {
      menuItems.indexOfFirst { MainNavTab.fromMenuItem(it) == restoredSnapshot.selectedTab }.takeIf { it >= 0 }
    } else {
      null
    }

    return if (matchingIndex != null) {
      Msg.Reconcile(
        menuItems = menuItems,
        pageIndex = matchingIndex,
        generation = restoredSnapshot!!.generation,
      )
    } else {
      val fallbackGen = (restoredSnapshot?.generation ?: 0L) + 1L
      Msg.Reconcile(
        menuItems = menuItems,
        pageIndex = 0,
        generation = fallbackGen,
      )
    }
  }

  private sealed interface Action {
    data class UserConfigEmitted(val userConfig: UserConfigState) : Action
  }

  private sealed interface Msg {
    data class Reconcile(
      val menuItems: List<MainNavMenuItem>,
      val pageIndex: Int,
      val generation: Long,
    ) : Msg
    data class LiveSwitchToNotLoggedIn(val newGeneration: Long) : Msg
    data class LiveSwitchToLoggedIn(val username: String, val newGeneration: Long) : Msg
    data class MenuIndexSwitching(val targetIndex: Int, val newGeneration: Long) : Msg
  }
}

private val log = KotlinLogging.logger { }
