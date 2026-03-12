package moe.cyunrei.videolivewallpaper.activity

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import moe.cyunrei.videolivewallpaper.R
import moe.cyunrei.videolivewallpaper.service.VideoLiveWallpaperService
import moe.cyunrei.videolivewallpaper.ui.theme.ModernTheme
import moe.cyunrei.videolivewallpaper.utils.DocumentUtils.getPath

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ModernTheme {
                MainScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        val context = LocalContext.current
        var showPathDialog by remember { mutableStateOf(false) }
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                showPathDialog = true
            }
        }

        val videoPickerLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            uri?.let {
                saveVideoPathAndSetWallpaper(context, it)
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ModernButton(
                        text = stringResource(R.string.choose_video_file),
                        icon = Icons.Default.VideoLibrary,
                        onClick = {
                            videoPickerLauncher.launch(arrayOf("video/*"))
                        }
                    )
                    
                    if (showPathDialog) {
                        PathInputDialog(
                            onDismiss = { showPathDialog = false },
                            onConfirm = { path ->
                                saveRawPathAndSetWallpaper(context, path)
                                showPathDialog = false
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    ModernButton(
                        text = stringResource(R.string.add_video_file_path),
                        icon = Icons.Default.Add,
                        onClick = {
                            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.READ_MEDIA_VIDEO
                            } else {
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            }
                            
                            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                showPathDialog = true
                            } else {
                                permissionLauncher.launch(permission)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    ModernButton(
                        text = stringResource(R.string.settings),
                        icon = Icons.Default.Settings,
                        onClick = {
                            context.startActivity(Intent(context, SettingsActivity::class.java))
                        }
                    )
                }
            }

        }
    }

    @Composable
    fun ModernButton(
        text: String,
        icon: ImageVector,
        onClick: () -> Unit
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }
    }

    @Composable
    fun PathInputDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.add_path)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Path") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { onConfirm(text) }) {
                    Text(stringResource(R.string.apply))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    private fun saveVideoPathAndSetWallpaper(context: Context, uri: Uri) {
        // Persist Uri permission if it's a content Uri
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
        
        context.openFileOutput("video_live_wallpaper_file_path", Context.MODE_PRIVATE).use {
            it.write(uri.toString().toByteArray())
        }
        VideoLiveWallpaperService.setToWallPaper(context)
    }

    private fun saveRawPathAndSetWallpaper(context: Context, path: String) {
        context.openFileOutput("video_live_wallpaper_file_path", Context.MODE_PRIVATE).use {
            it.write(path.toByteArray())
        }
        VideoLiveWallpaperService.setToWallPaper(context)
    }
}