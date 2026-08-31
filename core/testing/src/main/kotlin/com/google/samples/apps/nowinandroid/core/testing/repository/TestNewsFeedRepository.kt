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

package com.google.samples.apps.nowinandroid.core.testing.repository

import com.google.samples.apps.nowinandroid.core.data.repository.NewsFeedRepository
import com.google.samples.apps.nowinandroid.core.model.data.NewsArticle
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class TestNewsFeedRepository : NewsFeedRepository {

    private val articlesFlow: MutableSharedFlow<List<NewsArticle>> =
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val _isSyncing = MutableStateFlow(false)
    override val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    var shouldThrowOnRefresh = false

    override fun observeArticles(category: String?): Flow<List<NewsArticle>> =
        articlesFlow.map { list ->
            if (category.isNullOrBlank() || category.equals("All", ignoreCase = true)) {
                list
            } else {
                list.filter { it.category.equals(category, ignoreCase = true) }
            }
        }

    override fun observeCategories(): Flow<List<String>> =
        articlesFlow.map { list ->
            val distinct = list.map { it.category }.distinct().sorted()
            if (distinct.isEmpty()) {
                listOf("All", "Top", "Technology", "Business")
            } else {
                listOf("All") + distinct
            }
        }

    override fun observeArticle(id: String): Flow<NewsArticle?> =
        articlesFlow.map { list -> list.find { it.id == id } }

    override suspend fun refreshNews(): Result<Unit> {
        if (shouldThrowOnRefresh) {
            return Result.failure(RuntimeException("Simulated network failure"))
        }
        return Result.success(Unit)
    }

    fun sendArticles(articles: List<NewsArticle>) {
        articlesFlow.tryEmit(articles)
    }

    fun setSyncing(syncing: Boolean) {
        _isSyncing.value = syncing
    }
}
