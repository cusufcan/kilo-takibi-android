package com.yusufcanmercan.weight_track_app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.data.local.Weight
import java.text.DateFormat
import java.util.Date

/** ViewModel'i bağlayan ince kabuk. Çizimi `HomeContent` yapar. */
@Composable
fun HomeScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Box(modifier = Modifier.fillMaxSize()) {
        HomeContent(state = state, onEdit = onEdit, onDelete = viewModel::onDelete)
        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_title))
        }
    }
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onEdit: (Long) -> Unit,
    onDelete: (Weight) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.isLoading && state.items.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(R.string.home_empty))
        }
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(state.items, key = { it.id }) { weight ->
            WeightRow(weight = weight, onClick = { onEdit(weight.id) }, onDelete = { onDelete(weight) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun WeightRow(weight: Weight, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.weight_kg, weight.weight),
            style = MaterialTheme.typography.titleMedium,
        )
        // Gün bazında gösterim: saat kısmı anlamsız (eski kodda rastgele).
        Text(text = DateFormat.getDateInstance().format(Date(weight.timeStamp)))
        TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
    }
}
