package com.fitproject.droid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fitproject.droid.ui.theme.BWSColors
import com.fitproject.droid.ui.theme.BWSTypography
import com.fitproject.droid.ui.theme.FitProjectTheme

class HealthPermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitProjectTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Health Data Privacy", style = BWSTypography.Headline, color = BWSColors.TextPrimary)
                    Text(
                        "FitProject reads your daily steps and walking distance from Health Connect to show activity on the Summary tab. Data stays on your device unless you choose to share it elsewhere. When Google Fit is connected to Health Connect, your Fit activity syncs automatically.",
                        style = BWSTypography.Body,
                        color = BWSColors.TextSecondary
                    )
                }
            }
        }
    }
}