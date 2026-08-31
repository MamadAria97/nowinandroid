/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.nowinandroid.feature.news.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.nowinandroid.core.data.repository.NewsFeedRepository
import com.google.samples.apps.nowinandroid.core.data.util.NetworkMonitor
import com.google.samples.apps.nowinandroid.core.model.data.NewsFeedException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsFeedRepository: NewsFeedRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val selectedCategory = MutableStateFlow("All")
    private val _errorMessages = MutableSharedFlow<String>()
    val errorMessages: SharedFlow<String> = _errorMessages.asSharedFlow()

    val uiState: StateFlow<NewsFeedUiState> = combine(
        selectedCategory.flatMapLatest { category ->
            newsFeedRepository.observeArticles(category)
        },
        newsFeedRepository.observeCategories(),
        selectedCategory,
        newsFeedRepository.isSyncing,
        networkMonitor.isOnline,
    ) { articles, categories, currentCategory, isSyncing, isOnline ->
        when {
            articles.isEmpty() && isSyncing -> NewsFeedUiState.Loading
            articles.isEmpty() -> NewsFeedUiState.Empty(
                categories = categories,
                selectedCategory = currentCategory,
                isRefreshing = isSyncing,
                isOffline = !isOnline,
            )
            else -> NewsFeedUiState.Success(
                articles = articles,
                categories = categories,
                selectedCategory = currentCategory,
                isRefreshing = isSyncing,
                isOffline = !isOnline,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NewsFeedUiState.Loading,
    )

    init {
        refresh()
    }

    fun selectCategory(category: String) {
        selectedCategory.value = category
    }

    fun refresh() {
        viewModelScope.launch {
            val result = newsFeedRepository.refreshNews()
            result.onFailure { exception ->
                val friendlyMessage = when (exception) {
                    is NewsFeedException.NetworkException ->
                        "Network error: Unable to connect. Showing cached news."
                    is NewsFeedException.RemoteServerException ->
                        "Remote server error: News service is unavailable. Showing cached news."
                    is NewsFeedException.LocalStorageException ->
                        "Local storage error: Failed to save news locally."
                    is NewsFeedException.EmptyResponseException ->
                        "Empty response: No new articles received from server."
                    else ->
                        "Unable to refresh news. Showing cached news."
                }
                _errorMessages.emit(friendlyMessage)
            }
        }
    }
}
