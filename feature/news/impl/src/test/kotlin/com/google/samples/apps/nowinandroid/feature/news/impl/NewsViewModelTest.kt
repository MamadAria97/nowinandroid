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
import com.google.samples.apps.nowinandroid.core.testing.repository.TestNewsFeedRepository
import com.google.samples.apps.nowinandroid.core.testing.util.MainDispatcherRule
import com.google.samples.apps.nowinandroid.core.testing.util.TestNetworkMonitor
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NewsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val newsFeedRepository = TestNewsFeedRepository()
    private val networkMonitor = TestNetworkMonitor()
    private lateinit var viewModel: NewsViewModel

    @Before
    fun setup() {
        viewModel = NewsViewModel(
            newsFeedRepository = newsFeedRepository,
            networkMonitor = networkMonitor,
        )
    }

    @Test
    fun stateIsInitiallyLoading() = runTest {
        assertEquals(NewsFeedUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun stateBecomesSuccessWhenArticlesAvailable() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect() }

        val sampleArticles = listOf(
            NewsArticle(
                id = "1",
                title = "Android 15 Released",
                link = "https://example.com/android15",
                ogImage = "https://example.com/image.jpg",
                source = "Google",
                sourceIcon = "https://example.com/favicon.ico",
                category = "Technology",
                fetchedAt = Clock.System.now(),
            ),
        )

        newsFeedRepository.sendArticles(sampleArticles)

        val state = viewModel.uiState.value
        assertTrue(state is NewsFeedUiState.Success)
        assertEquals(1, (state as NewsFeedUiState.Success).articles.size)
        assertEquals("Android 15 Released", state.articles.first().title)

        collectJob.cancel()
    }

    @Test
    fun stateBecomesEmptyWhenNoArticlesAvailable() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect() }

        newsFeedRepository.sendArticles(emptyList())

        val state = viewModel.uiState.value
        assertTrue(state is NewsFeedUiState.Empty)

        collectJob.cancel()
    }

    @Test
    fun categorySelectionFiltersArticles() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect() }

        val sampleArticles = listOf(
            NewsArticle(
                id = "1",
                title = "Tech Article",
                link = "https://example.com/tech",
                ogImage = null,
                source = "TechSource",
                sourceIcon = null,
                category = "Technology",
                fetchedAt = Clock.System.now(),
            ),
            NewsArticle(
                id = "2",
                title = "Biz Article",
                link = "https://example.com/biz",
                ogImage = null,
                source = "BizSource",
                sourceIcon = null,
                category = "Business",
                fetchedAt = Clock.System.now(),
            ),
        )

        newsFeedRepository.sendArticles(sampleArticles)

        viewModel.selectCategory("Business")

        val state = viewModel.uiState.value
        assertTrue(state is NewsFeedUiState.Success)
        val successState = state as NewsFeedUiState.Success
        assertEquals(1, successState.articles.size)
        assertEquals("Biz Article", successState.articles.first().title)

        collectJob.cancel()
    }
}
