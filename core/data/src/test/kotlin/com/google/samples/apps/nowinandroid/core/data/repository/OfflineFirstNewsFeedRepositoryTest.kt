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

import com.google.samples.apps.nowinandroid.core.data.testdoubles.TestNewsArticleDao
import com.google.samples.apps.nowinandroid.core.data.testdoubles.TestNewsNetworkDataSource
import com.google.samples.apps.nowinandroid.core.model.data.NewsFeedException
import com.google.samples.apps.nowinandroid.core.network.model.NetworkNewsArticle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OfflineFirstNewsFeedRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dao: TestNewsArticleDao
    private lateinit var network: TestNewsNetworkDataSource
    private lateinit var repository: OfflineFirstNewsFeedRepository

    @Before
    fun setup() {
        dao = TestNewsArticleDao()
        network = TestNewsNetworkDataSource()
        repository = OfflineFirstNewsFeedRepository(
            newsArticleDao = dao,
            newsNetworkDataSource = network,
            ioDispatcher = testDispatcher,
        )
    }

    @Test
    fun refreshNews_fetches_and_persists_in_local_db() = runTest(testDispatcher) {
        network.newsFeedResponse = mapOf(
            "Technology" to listOf(
                NetworkNewsArticle(
                    link = "https://example.com/tech1",
                    title = "Tech News 1",
                    source = "TechSource",
                    og = "https://example.com/image1.jpg",
                ),
            ),
            "Business" to listOf(
                NetworkNewsArticle(
                    link = "https://example.com/biz1",
                    title = "Business News 1",
                    source = "BizSource",
                    og = "https://example.com/image2.jpg",
                ),
            ),
        )

        val result = repository.refreshNews()
        assertTrue(result.isSuccess)

        val allArticles = repository.observeArticles().first()
        assertEquals(2, allArticles.size)

        val techArticles = repository.observeArticles("Technology").first()
        assertEquals(1, techArticles.size)
        assertEquals("Tech News 1", techArticles.first().title)

        val singleArticle = repository.observeArticle(techArticles.first().id).first()
        assertNotNull(singleArticle)
        assertEquals("Tech News 1", singleArticle?.title)
    }

    @Test
    fun refreshNews_on_network_failure_preserves_existing_cache() = runTest(testDispatcher) {
        network.newsFeedResponse = mapOf(
            "World" to listOf(
                NetworkNewsArticle(
                    link = "https://example.com/world1",
                    title = "World News 1",
                    source = "WorldSource",
                ),
            ),
        )

        val firstRefresh = repository.refreshNews()
        assertTrue(firstRefresh.isSuccess)

        val cachedArticles = repository.observeArticles().first()
        assertEquals(1, cachedArticles.size)

        network.shouldThrowError = true
        val secondRefresh = repository.refreshNews()
        assertTrue(secondRefresh.isFailure)

        val preservedArticles = repository.observeArticles().first()
        assertEquals(1, preservedArticles.size)
        assertEquals("World News 1", preservedArticles.first().title)
    }

    @Test
    fun refreshNews_on_empty_response_returns_empty_response_exception() = runTest(testDispatcher) {
        network.newsFeedResponse = emptyMap()

        val result = repository.refreshNews()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NewsFeedException.EmptyResponseException)
    }
}
