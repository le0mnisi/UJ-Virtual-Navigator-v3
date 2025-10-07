package com.example.ujvirtualnavigatorv3

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateBearing
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.location.toCommonLocation
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.LocationPuck3D
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import java.io.BufferedReader
import java.io.InputStreamReader
import com.mapbox.maps.ImageHolder

class MainActivity : ComponentActivity(), PermissionsListener {

    private lateinit var permissionsManager: PermissionsManager

    private var mapboxNavigation: MapboxNavigation? = null
    private val navigationLocationProvider = NavigationLocationProvider()
    private var routeLineApi: MapboxRouteLineApi? = null
    private var routeLineView: MapboxRouteLineView? = null

    data class MapStyle(
        val styleUri: String,
        val color: Color,
        val title: String,
        val iconRes: Int
    )

    data class Place(
        val name: String,
        val description: String?,
        val coordinates: Point
    )

    private val mapStyles = listOf(
        MapStyle(
            "mapbox://styles/slick16/cmf9xecrz003201sdgschbnwy",
            Color(0xFF454052),
            "Dark",
            R.drawable.ic_style
        ),
        MapStyle(
            "mapbox://styles/slick16/cmf9xhlb7002m01s39w451ckj",
            Color(0xFFD3D3D3),
            "Light",
            R.drawable.ic_style
        ),
        MapStyle(
            "mapbox://styles/slick16/cmf9xz8kh002t01sddxebh40f",
            Color(0xFF00FF00),
            "Satellite",
            R.drawable.ic_style
        )
    )

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Immersive fullscreen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        // Request permissions or setup map
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            setupNavigation()
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun setupNavigation() {
        val navOptions = NavigationOptions.Builder(this).build()
        mapboxNavigation = MapboxNavigationProvider.create(navOptions)
        mapboxNavigation?.startTripSession()

        // Route line
        routeLineApi = MapboxRouteLineApi(MapboxRouteLineApiOptions.Builder().build())
        routeLineView = MapboxRouteLineView(
            MapboxRouteLineViewOptions.Builder(this)
                .routeLineColorResources(
                    RouteLineColorResources.Builder()
                        .routeDefaultColor(Color(0xFF007AFF).hashCode())
                        .routeLineTraveledColor(Color.Blue.hashCode())
                        .build()
                )
                .build()
        )

        setupMap()
    }

    @OptIn(MapboxExperimental::class)
    private fun setupMap() {
        setContent {
            var menuOpen by remember { mutableStateOf(false) }
            var currentStyle by remember { mutableStateOf(mapStyles[0].styleUri) }
            var searchQuery by remember { mutableStateOf("") }
            var selectedPlace by remember { mutableStateOf<Place?>(null) }

            val context = LocalContext.current
            val mapViewportState = rememberMapViewportState {
                setCameraOptions {
                    zoom(14.0)
                    center(Point.fromLngLat(28.0805, -26.1462))
                }
            }

            val places = remember { loadPlacesFromGeoJson(context, "locations.geojson") }
            val filteredPlaces = if (searchQuery.isNotBlank()) {
                places.filter { it.name.contains(searchQuery, ignoreCase = true) }
            } else emptyList()

            Box(modifier = Modifier.fillMaxSize()) {

                // --- Map ---
                MapboxMap(
                    modifier = Modifier.fillMaxSize(),
                    mapViewportState = mapViewportState
                ) {
                    MapEffect(currentStyle) { mapView ->
                        mapView.getMapboxMap().loadStyleUri(currentStyle) { style ->

                            // ✅ Style is fully loaded, now enable location
                            val locationPlugin = mapView.location
                            locationPlugin.updateSettings {
                                enabled = true
                                locationPuck = LocationPuck3D(
                                    modelUri = "asset://student_avatar.glb", // put in /assets
                                    modelScale = listOf(50.0f, 50.0f, 50.0f),
                                    modelRotation = listOf(0f, 0f, 0f),
                                    modelTranslation = listOf(0f, 0f, 0.5f),
                                    modelOpacity = 1.0f
                                )
                                puckBearing = PuckBearing.HEADING
                                puckBearingEnabled = true
                            }


                            // Update NavigationLocationProvider
                        locationPlugin.addOnIndicatorPositionChangedListener { point ->
                            val location = android.location.Location("mapbox")
                            location.latitude = point.latitude()
                            location.longitude = point.longitude()
                            navigationLocationProvider.changePosition(location.toCommonLocation())
                        }

                        // Follow puck
                        mapViewportState.transitionToFollowPuckState(
                            FollowPuckViewportStateOptions.Builder()
                                .bearing(FollowPuckViewportStateBearing.SyncWithLocationPuck)
                                .build()
                        )

                        // Load map style
                        mapView.getMapboxMap().loadStyleUri(currentStyle)

                        // Route line observer
                        mapboxNavigation?.registerRoutesObserver(RoutesObserver { routes ->
                            val api = routeLineApi ?: return@RoutesObserver
                            val view = routeLineView ?: return@RoutesObserver
                            api.setNavigationRoutes(routes.navigationRoutes) { drawData ->
                                mapView.getMapboxMap().getStyle()?.let { style ->
                                    view.renderRouteDrawData(style, drawData)
                                }
                            }
                        })
                    }}

                    // Selected place marker
                    selectedPlace?.let { place ->
                        val marker = rememberIconImage(
                            key = "selected-marker",
                            painter = painterResource(id = R.drawable.ic_marker)
                        )
                        PointAnnotation(point = place.coordinates) { iconImage = marker }
                    }
                }

                // --- Search Bar ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 40.dp)
                        .align(Alignment.TopCenter)
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search places", color = Color.Gray) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(Color.White.copy(alpha = 0.7f), shape = RoundedCornerShape(50)),
                        textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = Color.Black,
                            focusedPlaceholderColor = Color.Gray,
                            unfocusedPlaceholderColor = Color.Gray,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    if (filteredPlaces.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                        ) {
                            LazyColumn {
                                items(filteredPlaces) { place ->
                                    Text(
                                        text = place.name,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedPlace = place
                                                searchQuery = ""
                                            }
                                            .padding(12.dp),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // --- Pop-up Card ---
                selectedPlace?.let { place ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable { selectedPlace = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .wrapContentHeight(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(place.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        requestRouteTo(place.coordinates)
                                        mapViewportState.flyTo(
                                            CameraOptions.Builder()
                                                .center(place.coordinates)
                                                .zoom(16.0)
                                                .build()
                                        )
                                        selectedPlace = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                                ) {
                                    Text("Navigate")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Tap outside to close", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                // --- Recenter Button ---
                Box(
                    modifier = Modifier
                        .padding(bottom = 32.dp, end = 16.dp)
                        .size(56.dp)
                        .align(Alignment.BottomEnd)
                        .background(color = Color.White.copy(alpha = 0.8f), shape = CircleShape)
                        .clickable {
                            mapViewportState.transitionToFollowPuckState(
                                FollowPuckViewportStateOptions.Builder()
                                    .bearing(FollowPuckViewportStateBearing.SyncWithLocationPuck)
                                    .build()
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_locator),
                        contentDescription = "Recenter",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // --- Style Switcher ---
                Box(
                    modifier = Modifier
                        .padding(bottom = 100.dp, end = 16.dp)
                        .size(56.dp)
                        .align(Alignment.BottomEnd)
                        .background(color = Color.White.copy(alpha = 0.8f), shape = CircleShape)
                        .clickable { menuOpen = !menuOpen },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_map_style),
                        contentDescription = "Change Style",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (menuOpen) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .padding(bottom = 170.dp, end = 16.dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        mapStyles.forEach { style ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(style.color, CircleShape)
                                    .clickable {
                                        currentStyle = style.styleUri
                                        menuOpen = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = style.iconRes),
                                    contentDescription = style.title,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadPlacesFromGeoJson(context: android.content.Context, fileName: String): List<Place> {
        val places = mutableListOf<Place>()
        try {
            val inputStream = context.assets.open(fileName)
            val json = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
            val featureCollection = FeatureCollection.fromJson(json)

            featureCollection.features()?.forEach { feature ->
                val name = feature.getStringProperty("name")
                val description = feature.getStringProperty("description")
                val point = feature.geometry() as? Point
                if (point != null) {
                    places.add(Place(name, description, point))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return places
    }

    private fun requestRouteTo(destination: Point) {
        val originLocation = navigationLocationProvider.lastLocation ?: return
        val originPoint = Point.fromLngLat(originLocation.longitude, originLocation.latitude)

        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(listOf(originPoint, destination))
            .build()

        mapboxNavigation?.requestRoutes(
            routeOptions,
            object : NavigationRouterCallback {
                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: String
                ) {
                    if (routes.isNotEmpty()) {
                        mapboxNavigation?.setNavigationRoutes(routes)
                    }
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {
                    // Handle failure
                }

                override fun onCanceled(
                    routeOptions: RouteOptions,
                    routerOrigin: String
                ) {
                    // Handle cancellation
                }
            }
        )
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {}
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onPermissionResult(granted: Boolean) { if (granted) setupNavigation() }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
