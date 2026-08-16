package me.ltthuc.kmp.feature.billing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import me.ltthuc.kmp.core.billing.model.ProductInfo
import me.ltthuc.kmp.core.billing.model.SubscriptionPlan
import me.ltthuc.kmp.core.model.Level
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.common_close
import me.ltthuc.kmp.core.resource.paywall_error_no_subscription_to_restore
import me.ltthuc.kmp.core.resource.paywall_error_purchase_failed
import me.ltthuc.kmp.core.ui.dialog.ParentalGateScreen
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import me.ltthuc.kmp.feature.billing.components.PaywallFeatureList
import me.ltthuc.kmp.feature.billing.components.PaywallFooter
import me.ltthuc.kmp.feature.billing.components.PaywallHeader
import me.ltthuc.kmp.feature.billing.components.PaywallPlanSelector
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Suppress("UnusedParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PaywallScreen(
    source: String,
    modifier: Modifier = Modifier,
    levelId: String? = null,
    gatedAlready: Boolean = false,
    viewModel: PaywallViewModel = koinViewModel(key = levelId) { parametersOf(levelId) },
) {
    val navBackStack = LocalNavBackStack.current
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val purchaseState by viewModel.purchaseState.collectAsStateWithLifecycle()
    val level by viewModel.level.collectAsStateWithLifecycle()
    val selectedPlan by viewModel.selectedPlan.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    // Purchases must sit behind the parental gate. If we were already gated (Pro icon path),
    // buy directly; otherwise (e.g. tapping a locked unit) gate at the purchase tap.
    var showPurchaseGate by remember { mutableStateOf(false) }

    val purchaseFailedMessage = stringResource(Res.string.paywall_error_purchase_failed)
    val noSubscriptionMessage = stringResource(Res.string.paywall_error_no_subscription_to_restore)

    LaunchedEffect(purchaseState) {
        when (purchaseState) {
            is PurchaseUiState.Success -> {
                navBackStack.removeLastOrNull()
            }

            is PurchaseUiState.PurchaseFailed -> {
                snackbarHostState.showSnackbar(purchaseFailedMessage)
            }

            is PurchaseUiState.NoSubscriptionToRestore -> {
                snackbarHostState.showSnackbar(noSubscriptionMessage)
            }

            is PurchaseUiState.Error -> {
                snackbarHostState.showSnackbar((purchaseState as PurchaseUiState.Error).message)
            }

            else -> {}
        }
    }

    Box(modifier = modifier) {
        AsyncLoadContents(
            modifier = Modifier.fillMaxSize(),
            screenState = screenState,
            retryAction = viewModel::fetch,
        ) { state ->
            PaywallContent(
                modifier = Modifier.fillMaxSize(),
                level = level,
                products = state.products,
                selectedPlan = selectedPlan,
                purchaseState = purchaseState,
                snackbarHostState = snackbarHostState,
                onPlanSelected = viewModel::selectPlan,
                onPurchaseClicked = {
                    if (gatedAlready) viewModel.purchase() else showPurchaseGate = true
                },
                onRestoreClicked = viewModel::restore,
                onBackClicked = { navBackStack.removeLastOrNull() },
            )
        }

        if (showPurchaseGate) {
            ParentalGateScreen(
                modifier = Modifier.fillMaxSize(),
                onPass = {
                    showPurchaseGate = false
                    viewModel.purchase()
                },
                onDismiss = { showPurchaseGate = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaywallContent(
    level: Level?,
    products: ImmutableList<ProductInfo>,
    selectedPlan: SubscriptionPlan,
    purchaseState: PurchaseUiState,
    snackbarHostState: SnackbarHostState,
    onPlanSelected: (SubscriptionPlan) -> Unit,
    onPurchaseClicked: () -> Unit,
    onRestoreClicked: () -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLoading = purchaseState is PurchaseUiState.Loading

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.common_close),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            PaywallHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )

            PaywallFeatureList(
                levelTitle = level?.title.orEmpty(),
                unitCount = level?.totalUnits ?: 0,
                modifier = Modifier.fillMaxWidth(),
            )

            PaywallPlanSelector(
                modifier = Modifier.fillMaxWidth(),
                products = products,
                selectedPlan = selectedPlan,
                onPlanSelected = onPlanSelected,
            )

            PaywallFooter(
                modifier = Modifier.fillMaxWidth(),
                isLoading = isLoading,
                onPurchaseClicked = onPurchaseClicked,
                onRestoreClicked = onRestoreClicked,
            )
        }
    }
}
