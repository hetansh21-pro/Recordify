package com.audiovault.app.ui.recordings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.audiovault.app.data.repository.SortOption

@Composable
fun SortFilterBar(
    currentSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortOptions = listOf(
        SortOption.NEWEST_FIRST to "Newest",
        SortOption.OLDEST_FIRST to "Oldest",
        SortOption.DURATION_LONGEST to "Longest",
        SortOption.DURATION_SHORTEST to "Shortest",
        SortOption.TITLE_A_TO_Z to "A - Z"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sortOptions.forEach { (option, label) ->
            val isSelected = currentSort == option
            FilterChip(
                selected = isSelected,
                onClick = { onSortSelected(option) },
                label = { Text(text = label, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}
