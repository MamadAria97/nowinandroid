/*
 * Copyright 2025 The Android Open Source Project
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

import com.google.samples.apps.nowinandroid.core.model.data.NewsArticle

sealed interface NewsFeedUiState {
    data object Loading : NewsFeedUiState

    data class Success(
        val articles: List<NewsArticle>,
        val categories: List<String>,
        val selectedCategory: String,
        val isRefreshing: Boolean = false,
        val isOffline: Boolean = false,
    ) : NewsFeedUiState

    data class Empty(
        val categories: List<String>,
        val selectedCategory: String,
        val isRefreshing: Boolean = false,
        val isOffline: Boolean = false,
    ) : NewsFeedUiState

    data class Error(
        val message: String? = null,
        val isOffline: Boolean = false,
    ) : NewsFeedUiState
}

sealed interface NewsDetailUiState {
    data object Loading : NewsDetailUiState
    data class Success(val article: NewsArticle) : NewsDetailUiState
    data object Error : NewsDetailUiState
}
