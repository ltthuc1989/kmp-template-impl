package me.ltthuc.kmp.feature.billing.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import me.ltthuc.kmp.core.billing.model.ProductInfo
import me.ltthuc.kmp.core.billing.model.SubscriptionPlan
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.paywall_plan_bundle_badge
import me.ltthuc.kmp.core.resource.paywall_plan_bundle_label
import me.ltthuc.kmp.core.resource.paywall_plan_level_badge
import me.ltthuc.kmp.core.resource.paywall_plan_level_label
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PaywallPlanSelector(
    products: ImmutableList<ProductInfo>,
    selectedPlan: SubscriptionPlan,
    onPlanSelected: (SubscriptionPlan) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        products.forEach { product ->
            PaywallPlanCard(
                modifier = Modifier.weight(1f),
                product = product,
                isSelected = selectedPlan == product.plan,
                onClick = { onPlanSelected(product.plan) },
            )
        }
    }
}

@Composable
private fun PaywallPlanCard(
    product: ProductInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "scale",
    )

    Card(
        modifier = modifier
            .scale(scale)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp),
                    )
                } else {
                    Modifier
                },
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Surface(
                color = if (product.isBundle) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                },
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(
                    modifier = Modifier.padding(
                        horizontal = 6.dp,
                        vertical = 2.dp,
                    ),
                    text = if (product.isBundle) {
                        stringResource(Res.string.paywall_plan_bundle_badge)
                    } else {
                        stringResource(Res.string.paywall_plan_level_badge)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (product.isBundle) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSecondary
                    },
                )
            }

            Text(
                text = if (product.isBundle) {
                    stringResource(Res.string.paywall_plan_bundle_label)
                } else {
                    stringResource(Res.string.paywall_plan_level_label)
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )

            Text(
                text = product.priceString,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
