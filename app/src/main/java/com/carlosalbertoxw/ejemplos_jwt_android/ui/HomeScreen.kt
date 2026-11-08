package com.carlosalbertoxw.ejemplos_jwt_android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.carlosalbertoxw.ejemplos_jwt_android.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel, onCreateProduct: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    TextButton(onClick = vm::logout) {
                        Text(stringResource(R.string.home_logout_short))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text(stringResource(R.string.home_tab_session)) },
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text(stringResource(R.string.home_tab_products)) },
                )
            }
            when (tab) {
                0 -> SessionTab(vm)
                1 -> ProductsTab(vm, onCreateProduct)
            }
        }
    }
}

/** Mensajes de éxito y error compartidos por ambas pestañas. */
@Composable
private fun Messages(vm: HomeViewModel) {
    val state = vm.uiState
    state.message?.let {
        Text(it.asText(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
    }
    state.error?.let {
        Text(it.asText(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SessionTab(vm: HomeViewModel) {
    val state = vm.uiState

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Messages(vm)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.session_me_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                val me = state.me
                if (me == null) {
                    Text(stringResource(R.string.loading))
                } else {
                    val missing = stringResource(R.string.value_missing)
                    Text(stringResource(R.string.session_me_id, me.id ?: missing))
                    Text(stringResource(R.string.session_me_username, me.username ?: missing))
                    Text(stringResource(R.string.session_me_role, me.role ?: missing))
                    Text(
                        stringResource(
                            R.string.session_me_expires,
                            formatUnixSeconds(me.tokenExpiresAt) ?: missing,
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.session_payload_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    state.accessTokenPayload ?: stringResource(R.string.value_missing),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.session_payload_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row {
            OutlinedButton(onClick = vm::refreshTokens) {
                Text(stringResource(R.string.session_refresh_tokens))
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = vm::logout) {
                Text(stringResource(R.string.session_logout))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.session_refresh_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductsTab(vm: HomeViewModel, onCreateProduct: () -> Unit) {
    val state = vm.uiState

    PullToRefreshBox(
        isRefreshing = state.isLoadingProducts,
        onRefresh = vm::reloadProducts,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Messages(vm)

            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = !state.showPublicCatalog,
                    onClick = { vm.loadProducts(publicCatalog = false) },
                    label = { Text(stringResource(R.string.products_chip_full)) },
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = state.showPublicCatalog,
                    onClick = { vm.loadProducts(publicCatalog = true) },
                    label = { Text(stringResource(R.string.products_chip_public)) },
                )
            }
            Spacer(Modifier.height(8.dp))

            ProductListCard(products = state.products, isLoading = state.isLoadingProducts)

            Spacer(Modifier.height(16.dp))

            if (state.me?.role == "Admin") {
                Button(onClick = onCreateProduct) {
                    Text(stringResource(R.string.products_new))
                }
            } else {
                Text(
                    stringResource(R.string.products_admin_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatUnixSeconds(seconds: Long?): String? {
    if (seconds == null) return null
    val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    return format.format(Date(seconds * 1000))
}
