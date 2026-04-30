package me.ltthuc.kmp.feature.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.common_next
import me.ltthuc.kmp.core.resource.onboarding_get_started
import me.ltthuc.kmp.core.resource.onboarding_page1_subtitle
import me.ltthuc.kmp.core.resource.onboarding_page1_title
import me.ltthuc.kmp.core.resource.onboarding_page2_subtitle
import me.ltthuc.kmp.core.resource.onboarding_page2_title
import me.ltthuc.kmp.core.resource.onboarding_page3_subtitle
import me.ltthuc.kmp.core.resource.onboarding_page3_title
import me.ltthuc.kmp.core.resource.onboarding_page4_subtitle
import me.ltthuc.kmp.core.resource.onboarding_page4_title
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import me.ltthuc.kmp.feature.onboarding.components.OnboardingPage
import me.ltthuc.kmp.feature.onboarding.components.OnboardingTopBar
import me.ltthuc.kmp.feature.onboarding.components.PageIndicator
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val PAGE_COUNT = 4
private const val LAST_PAGE_INDEX = PAGE_COUNT - 1

@Composable
internal fun OnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val navBackStack = LocalNavBackStack.current
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()

    val pages = remember { onboardingPages() }

    val completeAndGoHome: () -> Unit = {
        viewModel.completeOnboarding()
        navBackStack.clear()
        navBackStack.add(Destination.Home)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            OnboardingTopBar(
                showBack = pagerState.currentPage > 0,
                showTitle = pagerState.currentPage > 0,
                onBackClicked = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                },
                onSkipClicked = completeAndGoHome,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                OnboardingPage(data = pages[page])
            }

            PageIndicator(
                currentPage = pagerState.currentPage,
                pageCount = PAGE_COUNT,
                modifier = Modifier.padding(vertical = 24.dp),
            )

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 40.dp)
                    .height(64.dp),
                onClick = {
                    if (pagerState.currentPage < LAST_PAGE_INDEX) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        completeAndGoHome()
                    }
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                val isLastPage = pagerState.currentPage == LAST_PAGE_INDEX
                Text(
                    text = if (isLastPage) {
                        stringResource(Res.string.onboarding_get_started)
                    } else {
                        stringResource(Res.string.common_next)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (!isLastPage) {
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowForward,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

private fun onboardingPages(): List<OnboardingPageData> = listOf(
    OnboardingPageData(
        title = Res.string.onboarding_page1_title,
        subtitle = Res.string.onboarding_page1_subtitle,
        illustration = Icons.Filled.School,
        badge = Icons.Filled.Star,
    ),
    OnboardingPageData(
        title = Res.string.onboarding_page2_title,
        subtitle = Res.string.onboarding_page2_subtitle,
        illustration = Icons.Filled.Hearing,
        badge = Icons.Filled.MusicNote,
    ),
    OnboardingPageData(
        title = Res.string.onboarding_page3_title,
        subtitle = Res.string.onboarding_page3_subtitle,
        illustration = Icons.Filled.RecordVoiceOver,
        badge = Icons.Filled.Mic,
    ),
    OnboardingPageData(
        title = Res.string.onboarding_page4_title,
        subtitle = Res.string.onboarding_page4_subtitle,
        illustration = Icons.Filled.EmojiEvents,
        badge = Icons.Filled.WorkspacePremium,
    ),
)
