package com.example.ujvirtualnavigatorv3

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateBearing
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState

class MainActivity : ComponentActivity(), PermissionsListener {

    private lateinit var permissionsManager: PermissionsManager

    data class MapStyle(
        val styleUri: String,
        val color: Color,
        val title: String,
        val iconRes: Int
    )

    private val mapStyles = listOf(
        MapStyle("mapbox://styles/slick16/cmf9xecrz003201sdgschbnwy", Color(0xFF454052), "Dark", R.drawable.ic_style),
        MapStyle("mapbox://styles/slick16/cmf9xhlb7002m01s39w451ckj", Color(0xFFD3D3D3), "Light", R.drawable.ic_style),
        MapStyle("mapbox://styles/slick16/cmf9xz8kh002t01sddxebh40f", Color(0xFF00FF00), "Satellite", R.drawable.ic_style)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen immersive
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        // Permissions
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            setupMap()
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    private fun setupMap() {
        setContent {
            var menuOpen by remember { mutableStateOf(false) }
            var currentStyle by remember { mutableStateOf(mapStyles[0].styleUri) }

            val mapViewportState = rememberMapViewportState {
                setCameraOptions {
                    zoom(16.0)
                    center(Point.fromLngLat(28.0805, -26.1462))
                    pitch(0.0)
                    bearing(0.0)
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                MapboxMap(
                    modifier = Modifier.fillMaxSize(),
                    mapViewportState = mapViewportState
                ) {
                    MapEffect(currentStyle) { mapView ->
                        // Enable location component with 2D puck
                        mapView.location.updateSettings {
                            locationPuck = LocationPuck2D()
                            enabled = true
                            puckBearing = PuckBearing.COURSE
                            puckBearingEnabled = true
                            pulsingEnabled = true
                        }

                        // Follow user location immediately
                        mapViewportState.transitionToFollowPuckState(
                            FollowPuckViewportStateOptions.Builder()
                                .bearing(FollowPuckViewportStateBearing.SyncWithLocationPuck)
                                .padding(EdgeInsets(200.0, 0.0, 0.0, 0.0))
                                .build()
                        )

                        // Load selected style
                        mapView.getMapboxMap().loadStyleUri(currentStyle)
                    }
                }

                // --- Recenter Button ---
                Box(
                    modifier = Modifier
                        .padding(bottom = 32.dp, end = 16.dp)
                        .size(56.dp)
                        .align(Alignment.BottomEnd)
                        .background(color = Color.DarkGray, shape = CircleShape)
                        .clickable {
                            mapViewportState.transitionToFollowPuckState(
                                FollowPuckViewportStateOptions.Builder()
                                    .bearing(FollowPuckViewportStateBearing.SyncWithLocationPuck)
                                    .padding(EdgeInsets(200.0, 0.0, 0.0, 0.0))
                                    .build()
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_locator),
                        contentDescription = "Recenter",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // --- Change Style Button ---
                Box(
                    modifier = Modifier
                        .padding(bottom = 100.dp, end = 16.dp)
                        .size(56.dp)
                        .align(Alignment.BottomEnd)
                        .background(color = Color.DarkGray, shape = CircleShape)
                        .clickable { menuOpen = !menuOpen },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_map_style),
                        contentDescription = "Change Style",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // --- Style Option Menu ---
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .padding(bottom = 168.dp, end = 16.dp) // stacked above main button
                        .align(Alignment.BottomEnd)
                ) {
                    if (menuOpen) {
                        mapStyles.forEach { style ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(color = style.color, shape = CircleShape)
                                        .clickable { currentStyle = style.styleUri },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = style.iconRes),
                                        contentDescription = style.title,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    text = style.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {}
    override fun onPermissionResult(granted: Boolean) { if (granted) setupMap() }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
