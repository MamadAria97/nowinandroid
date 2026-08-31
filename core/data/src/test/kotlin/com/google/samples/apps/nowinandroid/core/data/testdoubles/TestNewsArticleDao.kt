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

package com.google.samples.apps.nowinandroid.core.data.testdoubles

import com.google.samples.apps.nowinandroid.core.database.dao.NewsArticleDao
import com.google.samples.apps.nowinandroid.core.database.model.NewsArticleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class TestNewsArticleDao : NewsArticleDao {
    private val entitiesStateFlow = MutableStateFlow<List<NewsArticleEntity>>(emptyList())

    override fun observeArticles(): Flow<List<NewsArticleEntity>> = entitiesStateFlow

    override fun observeArticlesByCategory(category: String): Flow<List<NewsArticleEntity>> =
        entitiesStateFlow.map { list ->
            list.filter { it.category.equals(category, ignoreCase = true) }
        }

    override fun observeArticleById(id: String): Flow<NewsArticleEntity?> =
        entitiesStateFlow.map { list -> list.find { it.id == id } }

    override suspend fun getArticleById(id: String): NewsArticleEntity? =
        entitiesStateFlow.value.find { it.id == id }

    override suspend fun upsertArticles(articles: List<NewsArticleEntity>) {
        entitiesStateFlow.update { oldList ->
            val oldMap = oldList.associateBy { it.id }.toMutableMap()
            articles.forEach { oldMap[it.id] = it }
            oldMap.values.toList()
        }
    }

    override suspend fun clearArticles() {
        entitiesStateFlow.value = emptyList()
    }
}
