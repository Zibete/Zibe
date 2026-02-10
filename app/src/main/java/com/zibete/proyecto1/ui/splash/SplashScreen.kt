package com.zibete.proyecto1.ui.splash

import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.UiTags.SPLASH_SCREEN
import com.zibete.proyecto1.ui.components.ZibeCircularProgress
import com.zibete.proyecto1.ui.theme.ZibeTheme

@Composable
fun SplashScreen() {

    val zibeColors = LocalZibeExtendedColors.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = zibeColors.contentDarkBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag(SPLASH_SCREEN),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Image(
                painter = painterResource(id = R.mipmap.logo_zibe),
                contentDescription = null,
                modifier = Modifier.size(220.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            ZibeCircularProgress(
                size = 48.dp,
                strokeWidth = 4.dp
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = false)
@Composable
fun SplashScreenPreview() {
    ZibeTheme {
        SplashScreen()
    }
}
