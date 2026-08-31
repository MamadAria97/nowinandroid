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

package com.google.samples.apps.nowinandroid.core.data.repository

import com.google.samples.apps.nowinandroid.core.common.network.Dispatcher
import com.google.samples.apps.nowinandroid.core.common.network.NiaDispatchers.IO
import com.google.samples.apps.nowinandroid.core.database.dao.NewsArticleDao
import com.google.samples.apps.nowinandroid.core.database.model.NewsArticleEntity
import com.google.samples.apps.nowinandroid.core.database.model.asExternalModel
import com.google.samples.apps.nowinandroid.core.model.data.NewsArticle
import com.google.samples.apps.nowinandroid.core.model.data.NewsFeedException
import com.google.samples.apps.nowinandroid.core.network.NewsNetworkDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OfflineFirstNewsFeedRepository @Inject constructor(
    private val newsArticleDao: NewsArticleDao,
    private val newsNetworkDataSource: NewsNetworkDataSource,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : NewsFeedRepository {

    private val _isSyncing = MutableStateFlow(false)
    override val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    override fun observeArticles(category: String?): Flow<List<NewsArticle>> {
        val flow = if (category.isNullOrBlank() || category.equals("All", ignoreCase = true)) {
            newsArticleDao.observeArticles()
        } else {
            newsArticleDao.observeArticlesByCategory(category)
        }
        return flow
            .map { entities -> entities.map(NewsArticleEntity::asExternalModel) }
            .catch { emit(emptyList()) }
            .flowOn(ioDispatcher)
    }

    override fun observeCategories(): Flow<List<String>> {
        return newsArticleDao.observeArticles()
            .map { entities ->
                val distinctCategories = entities.map { it.category }.distinct().sorted()
                if (distinctCategories.isEmpty()) {
                    listOf("All", "Top", "Technology", "Business", "Entertainment", "Health", "Science", "Sports", "World", "Environment")
                } else {
                    listOf("All") + distinctCategories
                }
            }
            .catch {
                emit(listOf("All", "Top", "Technology", "Business", "Entertainment", "Health", "Science", "Sports", "World", "Environment"))
            }
            .flowOn(ioDispatcher)
    }

    override fun observeArticle(id: String): Flow<NewsArticle?> {
        return newsArticleDao.observeArticleById(id)
            .map { it?.asExternalModel() }
            .catch { emit(null) }
            .flowOn(ioDispatcher)
    }

    override suspend fun refreshNews(): Result<Unit> = withContext(ioDispatcher) {
        if (_isSyncing.value) {
            return@withContext Result.success(Unit)
        }
        _isSyncing.value = true
        try {
            val feedMap = try {
                newsNetworkDataSource.getNewsFeed()
            } catch (e: IOException) {
                return@withContext Result.failure(NewsFeedException.NetworkException(cause = e))
            } catch (e: Exception) {
                return@withContext Result.failure(NewsFeedException.RemoteServerException(cause = e))
            }

            if (feedMap.isEmpty() || feedMap.values.all { it.isEmpty() }) {
                return@withContext Result.failure(NewsFeedException.EmptyResponseException())
            }

            val now = Clock.System.now()
            val entities = feedMap.flatMap { (category, articles) ->
                articles.map { networkArticle ->
                    val articleId = generateArticleId(networkArticle.link, networkArticle.title)
                    NewsArticleEntity(
                        id = articleId,
                        title = networkArticle.title,
                        link = networkArticle.link,
                        ogImage = networkArticle.og,
                        source = networkArticle.source,
                        sourceIcon = networkArticle.sourceIcon,
                        category = category,
                        fetchedAt = now,
                    )
                }
            }

            try {
                if (entities.isNotEmpty()) {
                    newsArticleDao.upsertArticles(entities)
                }
            } catch (e: Exception) {
                return@withContext Result.failure(NewsFeedException.LocalStorageException(cause = e))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    private fun generateArticleId(link: String, title: String): String {
        val input = link.ifBlank { title }
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
