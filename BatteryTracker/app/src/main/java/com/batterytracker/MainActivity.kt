package com.batterytracker

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        setContent {
            BatteryTrackerTheme {
                MainScaffold(this@MainActivity, fusedLocationClient)
            }
        }
    }
}

@Composable
fun BatteryTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF6C63FF),
            secondary = Color(0xFF03DAC6),
            background = Color(0xFF0D1117),
            surface = Color(0xFF161B22),
            onPrimary = Color.White,
            onSecondary = Color.Black,
            onBackground = Color(0xFFC9D1D9),
            onSurface = Color(0xFFC9D1D9),
        ),
        content = content
    )
}

sealed class Tab(val label: String, val icon: ImageVector) {
    data object Battery : Tab("Battery", Icons.Default.BatteryFull)
    data object Device : Tab("Device", Icons.Default.PhoneAndroid)
    data object Location : Tab("Location", Icons.Default.MyLocation)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(context: Context, fusedLocationClient: FusedLocationProviderClient) {
    var selectedTab by remember { mutableStateOf<Tab>(Tab.Battery) }
    val tabs = listOf(Tab.Battery, Tab.Device, Tab.Location)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF161B22),
                contentColor = Color(0xFFC9D1D9)
            ) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontFamily = FontFamily.Monospace) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF6C63FF),
                            selectedTextColor = Color(0xFF6C63FF),
                            unselectedIconColor = Color(0xFF8B949E),
                            unselectedTextColor = Color(0xFF8B949E),
                            indicatorColor = Color(0xFF1C2333)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0D1117), Color(0xFF161B22), Color(0xFF0D1117))
                    )
                )
        ) {
            when (selectedTab) {
                Tab.Battery -> BatteryTab(context)
                Tab.Device -> DeviceTab()
                Tab.Location -> LocationTab(context, fusedLocationClient)
            }
        }
    }
}

@Composable
fun BatteryTab(context: Context) {
    var batteryLevel by remember { mutableFloatStateOf(0f) }
    var isCharging by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) batteryLevel = level.toFloat() / scale.toFloat()
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                        || status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BatteryArc(batteryLevel, isCharging)
        Spacer(modifier = Modifier.height(24.dp))

        if (isCharging) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, "Charging", tint = Color(0xFF03DAC6), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("CHARGING", color = Color(0xFF03DAC6), fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
            }
        } else {
            Text("NOT CHARGING", color = Color(0xFF8B949E), fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2333))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.BatteryFull, "Battery", tint = Color(0xFF6C63FF), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("BATTERY LEVEL", color = Color(0xFF8B949E), fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                    Text("${(batteryLevel * 100).toInt()}%", color = Color.White, fontSize = 40.sp,
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun DeviceTab() {
    val info = listOf(
        "Model" to Build.MODEL,
        "Manufacturer" to Build.MANUFACTURER,
        "Brand" to Build.BRAND,
        "Android" to Build.VERSION.RELEASE,
        "SDK" to "${Build.VERSION.SDK_INT}",
        "Device" to Build.DEVICE,
        "Product" to Build.PRODUCT,
        "Hardware" to Build.HARDWARE,
        "Board" to Build.BOARD,
        "Fingerprint" to Build.FINGERPRINT.take(48) + "...",
    )

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text("DEVICE INFO", color = Color(0xFF6C63FF), fontSize = 14.sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(4.dp))

        info.forEach { (label, value) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2333))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(label.uppercase(), color = Color(0xFF8B949E), fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(value, color = Color.White, fontSize = 16.sp,
                        fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun LocationTab(context: Context, fusedLocationClient: FusedLocationProviderClient) {
    var latitude by remember { mutableDoubleStateOf(0.0) }
    var longitude by remember { mutableDoubleStateOf(0.0) }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.MyLocation,
            contentDescription = null,
            tint = Color(0xFF6C63FF),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("LOCATION", color = Color(0xFF6C63FF), fontSize = 14.sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2333))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = Color(0xFF6C63FF), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LATITUDE", color = Color(0xFF8B949E), fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (latitude != 0.0 || longitude != 0.0) String.format("%.6f", latitude) else "---",
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = Color(0xFF6C63FF), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LONGITUDE", color = Color(0xFF8B949E), fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (latitude != 0.0 || longitude != 0.0) String.format("%.6f", longitude) else "---",
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    loading = true
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            latitude = location.latitude
                            longitude = location.longitude
                        }
                        loading = false
                    }.addOnFailureListener { loading = false }
                }
            },
            enabled = !loading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("GET LOCATION", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BatteryArc(level: Float, isCharging: Boolean) {
    val animatedLevel by animateFloatAsState(
        targetValue = level,
        animationSpec = tween(durationMillis = 1000)
    )

    val arcColor = when {
        animatedLevel > 0.6f -> Color(0xFF4CAF50)
        animatedLevel > 0.2f -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
        Canvas(modifier = Modifier.size(220.dp)) {
            val strokeWidth = 16.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            drawArc(Color(0xFF1C2333), 135f, 270f, false, topLeft, arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
            drawArc(
                if (isCharging) Color(0xFF03DAC6) else arcColor,
                135f, 270f * animatedLevel, false, topLeft, arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(animatedLevel * 100).toInt()}", fontSize = 56.sp,
                fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
            Text("%", fontSize = 18.sp, color = Color(0xFF8B949E), fontFamily = FontFamily.Monospace)
        }
    }
}
