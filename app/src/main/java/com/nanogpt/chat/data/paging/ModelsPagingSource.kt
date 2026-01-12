package com.nanogpt.chat.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.nanogpt.chat.data.remote.dto.ModelDto

/**
 * PagingSource for in-memory list of models.
 * Since we fetch all models at once from the API and cache them,
 * this PagingSource simply pages over the cached list with applied filters.
 */
class ModelsPagingSource(
    private val models: List<ModelDto>,
    private val filter: (ModelDto) -> Boolean
) : PagingSource<Int, ModelDto>() {

    companion object {
        const val PAGE_SIZE = 20
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ModelDto> {
        val page = params.key ?: 0
        val pageSize = params.loadSize

        // Apply filter to get the filtered list
        val filteredModels = models.filter(filter)

        // Calculate the range for this page
        val fromIndex = page * pageSize
        val toIndex = minOf(fromIndex + pageSize, filteredModels.size)

        // Check if we're out of bounds
        if (fromIndex >= filteredModels.size) {
            return LoadResult.Page(
                data = emptyList(),
                prevKey = if (page == 0) null else page - 1,
                nextKey = null
            )
        }

        // Get the page data
        val pageData = filteredModels.subList(fromIndex, toIndex)

        return LoadResult.Page(
            data = pageData,
            prevKey = if (page == 0) null else page - 1,
            nextKey = if (toIndex < filteredModels.size) page + 1 else null
        )
    }

    override fun getRefreshKey(state: PagingState<Int, ModelDto>): Int? {
        // Try to find the page key of the closest item to anchor position
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
