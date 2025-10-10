package com.example.ujvirtualnavigatorv3

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
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
import coil.compose.rememberAsyncImagePainter
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.location.toCommonLocation
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateBearing
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.maps.plugin.LocationPuck3D
import com.google.android.gms.maps.StreetViewPanoramaView
import com.google.android.gms.maps.model.LatLng
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

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

    data class AvatarOption(
        val name: String,
        val modelUri: String,
        val iconUri: String
    )

    data class Place(
        val name: String,
        val description: String? = null,
        val coordinates: Point,
        val streetViewPoints: List<LatLng> = emptyList(),
        val keywords: List<String> = emptyList() // <-- added for keyword search
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

    private val avatars = listOf(
        AvatarOption(
            "Avatar 1",
            "asset://student_avatar1.glb",
            "file:///android_asset/student_avatar1.jpg"
        ),
        AvatarOption(
            "Avatar 2",
            "asset://student_avatar2.glb",
            "file:///android_asset/student_avatar2.jpg"
        ),
        AvatarOption(
            "Avatar 3",
            "asset://student_avatar3.glb",
            "file:///android_asset/student_avatar3.jpg"
        ),
        AvatarOption(
            "Avatar 4",
            "asset://student_avatar4.glb",
            "file:///android_asset/student_avatar4.jpg"
        )
    )

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

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

        routeLineApi = MapboxRouteLineApi(MapboxRouteLineApiOptions.Builder().build())
        routeLineView = MapboxRouteLineView(
            MapboxRouteLineViewOptions.Builder(this)
                .routeLineColorResources(
                    RouteLineColorResources.Builder()
                        .routeDefaultColor(Color(0xFF007AFF).hashCode())
                        .routeLineTraveledColor(Color.Blue.hashCode())
                        .build()
                ).build()
        )

        setupMap()
    }

    @OptIn(MapboxExperimental::class)
    private fun setupMap() {
        setContent {
            var menuOpen by remember { mutableStateOf(false) }
            var avatarMenuOpen by remember { mutableStateOf(false) }
            var currentStyle by remember { mutableStateOf(mapStyles[0].styleUri) }
            var currentAvatar by remember { mutableStateOf(avatars[0]) }
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
                val query = searchQuery.lowercase()
                places.filter { place ->
                    place.name.contains(query, ignoreCase = true) ||
                            place.keywords.any { it.contains(query) }
                }
            } else emptyList()

            Box(modifier = Modifier.fillMaxSize()) {

                // --- Map ---
                MapboxMap(
                    modifier = Modifier.fillMaxSize(),
                    mapViewportState = mapViewportState
                ) {
                    MapEffect(currentStyle + currentAvatar.modelUri) { mapView ->
                        mapView.getMapboxMap().loadStyleUri(currentStyle) { _ ->

                            val locationPlugin = mapView.location
                            locationPlugin.updateSettings {
                                enabled = true
                                locationPuck = LocationPuck3D(
                                    modelUri = currentAvatar.modelUri,
                                    modelScale = listOf(50.0f, 50.0f, 50.0f),
                                    modelRotation = listOf(0f, 0f, 0f),
                                    modelTranslation = listOf(0f, 0f, 0.5f),
                                    modelOpacity = 1.0f
                                )
                                puckBearing = PuckBearing.HEADING
                                puckBearingEnabled = true
                            }

                            locationPlugin.addOnIndicatorPositionChangedListener { point ->
                                val location = android.location.Location("mapbox")
                                location.latitude = point.latitude()
                                location.longitude = point.longitude()
                                navigationLocationProvider.changePosition(location.toCommonLocation())
                            }

                            mapViewportState.transitionToFollowPuckState(
                                FollowPuckViewportStateOptions.Builder()
                                    .bearing(FollowPuckViewportStateBearing.SyncWithLocationPuck)
                                    .build()
                            )

                            mapboxNavigation?.registerRoutesObserver(RoutesObserver { routes ->
                                val api = routeLineApi ?: return@RoutesObserver
                                val view = routeLineView ?: return@RoutesObserver
                                api.setNavigationRoutes(routes.navigationRoutes) { drawData ->
                                    mapView.getMapboxMap().getStyle()?.let { style ->
                                        view.renderRouteDrawData(style, drawData)
                                    }
                                }
                            })
                        }
                    }

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

                // --- Pop-up Card for selected place ---
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
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(place.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                place.description?.let {
                                    Text(it, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                val svPoints = if (place.streetViewPoints.isNotEmpty()) place.streetViewPoints else listOf(
                                    LatLng(place.coordinates.latitude(), place.coordinates.longitude())
                                )
                                var currentIndex by remember { mutableStateOf(0) }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    StreetViewPanoramaComposable(
                                        position = svPoints[currentIndex],
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(250.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )

                                    if (svPoints.size > 1) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            Button(
                                                onClick = {
                                                    currentIndex = if (currentIndex > 0) currentIndex - 1 else svPoints.lastIndex
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                                            ) { Text("Previous") }

                                            Button(
                                                onClick = {
                                                    currentIndex = if (currentIndex < svPoints.lastIndex) currentIndex + 1 else 0
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                                            ) { Text("Next") }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        requestRouteTo(place.coordinates)
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(bottom = 170.dp, end = 16.dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        mapStyles.forEach { style ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    currentStyle = style.styleUri
                                    menuOpen = false
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(style.color, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = style.iconRes),
                                        contentDescription = style.title,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = style.title,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }

                // --- Avatar Switcher ---
                Box(
                    modifier = Modifier
                        .padding(bottom = 100.dp, start = 16.dp)
                        .size(56.dp)
                        .align(Alignment.BottomStart)
                        .background(Color.White.copy(alpha = 0.8f), CircleShape)
                        .clickable { avatarMenuOpen = !avatarMenuOpen },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_avatar),
                        contentDescription = "Change Avatar",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (avatarMenuOpen) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(bottom = 170.dp, start = 16.dp)
                            .align(Alignment.BottomStart)
                    ) {
                        avatars.forEach { avatar ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    currentAvatar = avatar
                                    avatarMenuOpen = false
                                }
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(avatar.iconUri),
                                    contentDescription = avatar.name,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (currentAvatar == avatar) 3.dp else 1.dp,
                                            color = if (currentAvatar == avatar) Color(0xFF007AFF) else Color.LightGray,
                                            shape = CircleShape
                                        )
                                )
                                Text(
                                    text = avatar.name,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(top = 4.dp)
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
            val root = JSONObject(json)
            val features = root.optJSONArray("features")
            if (features != null) {
                for (i in 0 until features.length()) {
                    val feature = features.getJSONObject(i)
                    val properties = feature.optJSONObject("properties")
                    val geometry = feature.optJSONObject("geometry")
                    val coordsArray = geometry?.optJSONArray("coordinates")
                    if (coordsArray != null && coordsArray.length() >= 2) {
                        val lng = coordsArray.getDouble(0)
                        val lat = coordsArray.getDouble(1)
                        val name = properties?.optString("name") ?: "Unknown"
                        val description = properties?.optString("description", null)
                        val keywords = properties?.optJSONArray("keywords")?.let { arr ->
                            List(arr.length()) { arr.optString(it).lowercase() }
                        } ?: emptyList()
                        val point = Point.fromLngLat(lng, lat)

                        val streetViewsList = mutableListOf<LatLng>()
                        val svArr = properties?.optJSONArray("streetviews")
                        if (svArr != null) {
                            for (j in 0 until svArr.length()) {
                                val svItem = svArr.optJSONObject(j)
                                if (svItem != null) {
                                    val svLat = svItem.optDouble("lat", Double.NaN)
                                    val svLng = svItem.optDouble("lng", Double.NaN)
                                    if (!svLat.isNaN() && !svLng.isNaN()) {
                                        streetViewsList.add(LatLng(svLat, svLng))
                                    }
                                }
                            }
                        }

                        places.add(Place(name, description, point, streetViewsList, keywords))
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
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
            object : com.mapbox.navigation.base.route.NavigationRouterCallback {
                override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: String) {
                    if (routes.isNotEmpty()) mapboxNavigation?.setNavigationRoutes(routes)
                }
                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {}
                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {}
            }
        )
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {}
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onPermissionResult(granted: Boolean) {
        if (granted) setupNavigation()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}

@Composable
fun StreetViewPanoramaComposable(position: LatLng, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            StreetViewPanoramaView(context).apply {
                onCreate(Bundle())
                getStreetViewPanoramaAsync { panorama -> panorama.setPosition(position) }
                onResume()
            }
        },
        update = { view -> view.getStreetViewPanoramaAsync { it.setPosition(position) } },
        onRelease = { view -> try { view.onPause() } catch (_: Exception) {}; try { view.onDestroy() } catch (_: Exception) {} }
    )
}
