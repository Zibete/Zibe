package com.zibete.proyecto1.ui.editprofile

import com.zibete.proyecto1.core.designsystem.R as DsR
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.components.ZibeBottomSheet
import com.zibete.proyecto1.ui.components.ZibeButtonPrimary
import com.zibete.proyecto1.ui.components.ZibeButtonSecondary
import com.zibete.proyecto1.ui.theme.ZibeTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileWelcomeBottomSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit
) {
    val zibeColors = LocalZibeExtendedColors.current

    val pages = listOf(
        EditProfileWelcomePage(
            R.string.editprofile_welcome_pager_1_title,
            R.string.editprofile_welcome_pager_1_body
        ),
        EditProfileWelcomePage(
            R.string.editprofile_welcome_pager_2_title,
            R.string.editprofile_welcome_pager_2_body
        ),
        EditProfileWelcomePage(
            R.string.editprofile_welcome_pager_3_title,
            R.string.editprofile_welcome_pager_3_body
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    ZibeBottomSheet(
        isOpen = isOpen,
        onCancel = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Brand Title
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = zibeColors.lightText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = dimensionResource(DsR.dimen.element_spacing_medium))
            )

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(DsR.dimen.bottom_padding))
            ) { index ->
                val page = pages[index]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = stringResource(id = page.titleRes),
                        style = MaterialTheme.typography.headlineSmall,
                        color = zibeColors.lightText,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(DsR.dimen.element_spacing_xxs)))
                    Text(
                        text = stringResource(id = page.bodyRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = zibeColors.hintText,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(DsR.dimen.element_spacing_xxs)))

            // Dots Indicator
            DotsIndicator(
                totalDots = pages.size,
                selectedIndex = pagerState.currentPage
            )

            Spacer(modifier = Modifier.height(dimensionResource(DsR.dimen.element_spacing_medium)))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(DsR.dimen.element_spacing_small))
            ) {
                ZibeButtonSecondary(
                    text = stringResource(R.string.action_previous),
                    onClick = {
                        scope.launch {
                            if (pagerState.currentPage > 0) {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = pagerState.currentPage > 0
                )

                ZibeButtonPrimary(
                    text = if (pagerState.currentPage == pages.size - 1) {
                        stringResource(R.string.action_continue)
                    } else {
                        stringResource(R.string.action_next)
                    },
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DotsIndicator(totalDots: Int, selectedIndex: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalDots) { i ->
            val isSelected = i == selectedIndex
            val size by animateDpAsState(if (isSelected) 10.dp else 8.dp, label = "dotSize")
            val color by animateColorAsState(
                if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
                label = "dotColor"
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(size)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditProfileWelcomeBottomSheetPreview() {
    ZibeTheme {
        EditProfileWelcomeBottomSheet(
            isOpen = true,
            onDismiss = {}
        )
    }
}


