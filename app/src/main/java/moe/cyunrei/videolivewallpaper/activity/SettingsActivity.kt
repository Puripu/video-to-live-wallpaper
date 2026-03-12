package moe.cyunrei.videolivewallpaper.activity

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.cyunrei.videolivewallpaper.R
import moe.cyunrei.videolivewallpaper.service.VideoLiveWallpaperService
import moe.cyunrei.videolivewallpaper.ui.theme.ModernTheme
import java.io.File

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ModernTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SettingsScreen(onBack: () -> Unit) {
        val context = LocalContext.current
        val scrollState = rememberScrollState()
        
        var isSoundEnabled by remember {
            mutableStateOf(File(context.filesDir, "unmute").exists())
        }
        
        val componentName = ComponentName(context, MainActivity::class.java)
        var isIconHidden by remember {
            mutableStateOf(
                context.packageManager.getComponentEnabledSetting(componentName) == 
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState)
                ) {
                    SettingsCategory(title = stringResource(R.string.wallpaper_settings)) {
                        SettingsSwitchItem(
                            title = stringResource(R.string.play_video_with_sound),
                            summary = if (isSoundEnabled) stringResource(R.string.enable) else stringResource(R.string.disable),
                            checked = isSoundEnabled,
                            onCheckedChange = { checked ->
                                isSoundEnabled = checked
                                toggleSound(context, checked)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsCategory(title = stringResource(R.string.applications_settings)) {
                        SettingsSwitchItem(
                            title = stringResource(R.string.hide_icon_from_launcher),
                            summary = if (isIconHidden) stringResource(R.string.hide) else stringResource(R.string.show),
                            checked = isIconHidden,
                            onCheckedChange = { checked ->
                                isIconHidden = checked
                                toggleLauncherIcon(context, checked)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsCategory(title = stringResource(R.string.about)) {
                        SettingsInfoItem(
                            title = stringResource(R.string.version),
                            summary = stringResource(R.string.version_name)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun SettingsCategory(title: String, content: @Composable ColumnScope.() -> Unit) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    content()
                }
            }
        }
    }

    @Composable
    fun SettingsSwitchItem(title: String, summary: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(text = summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }

    @Composable
    fun SettingsInfoItem(title: String, summary: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(text = summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    private fun toggleSound(context: Context, enabled: Boolean) {
        val file = File(context.filesDir, "unmute")
        if (enabled) {
            file.createNewFile()
            VideoLiveWallpaperService.unmuteMusic(context)
        } else {
            file.delete()
            VideoLiveWallpaperService.muteMusic(context)
        }
    }

    private fun toggleLauncherIcon(context: Context, hide: Boolean) {
        val componentName = ComponentName(context, MainActivity::class.java)
        val state = if (hide) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        context.packageManager.setComponentEnabledSetting(
            componentName,
            state,
            PackageManager.DONT_KILL_APP
        )
    }
}