package com.salpiras.citizendocs.core.ui.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/**
 * Base for every screen ViewModel.
 *
 * Three rules, and the whole architecture follows from them:
 *  - exactly one [state]. The old DocsListViewModel exposed both a `uiState` and a separate
 *    `documents` flow, each independently collecting the same Room query — two subscriptions
 *    that could disagree about whether the list was empty.
 *  - exactly one entry point, [onEvent]. The UI calls nothing else, so every possible user
 *    interaction is enumerable from the Event type alone.
 *  - [effects] for one-shot actions (navigate, open a file, show a snackbar). These must not
 *    be replayed on configuration change the way state is.
 *
 * Upstream flows are bound in `init` with `launchIn(viewModelScope)` rather than
 * `stateIn(WhileSubscribed)`. That keeps one database subscription alive for the ViewModel's
 * whole life instead of unsubscribing shortly after the screen leaves — negligible for a
 * local Room query, and it makes emissions deterministic in tests.
 */
abstract class MviViewModel<State, Event, Effect>(initialState: State) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects: Flow<Effect> = _effects.receiveAsFlow()

    protected val currentState: State get() = _state.value

    abstract fun onEvent(event: Event)

    protected fun setState(reduce: State.() -> State) = _state.update(reduce)

    protected fun sendEffect(effect: Effect) {
        _effects.trySend(effect)
    }
}
