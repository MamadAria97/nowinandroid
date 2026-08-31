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

package com.google.samples.apps.nowinandroid.feature.news.impl.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.google.samples.apps.nowinandroid.core.navigation.Navigator
import com.google.samples.apps.nowinandroid.feature.news.api.navigation.NewsDetailNavKey
import com.google.samples.apps.nowinandroid.feature.news.api.navigation.NewsNavKey
import com.google.samples.apps.nowinandroid.feature.news.api.navigation.navigateToNewsDetail
import com.google.samples.apps.nowinandroid.feature.news.impl.NewsDetailScreen
import com.google.samples.apps.nowinandroid.feature.news.impl.NewsDetailViewModel
import com.google.samples.apps.nowinandroid.feature.news.impl.NewsDetailViewModel.Factory
import com.google.samples.apps.nowinandroid.feature.news.impl.NewsScreen

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.newsEntry(navigator: Navigator) {
    entry<NewsNavKey> {
        NewsScreen(
            onArticleClick = navigator::navigateToNewsDetail,
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.newsDetailEntry(navigator: Navigator) {
    entry<NewsDetailNavKey>(
        metadata = ListDetailSceneStrategy.detailPane(),
    ) { key ->
        val articleId = key.articleId
        NewsDetailScreen(
            viewModel = hiltViewModel<NewsDetailViewModel, Factory>(
                key = articleId,
            ) { factory ->
                factory.create(articleId)
            },
            onBackClick = { navigator.goBack() },
        )
    }
}
