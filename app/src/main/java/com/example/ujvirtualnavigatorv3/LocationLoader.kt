package com.example.ujvirtualnavigatorv3.utils

import android.content.Context
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point

data class LocationFeature(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

fun loadLocationsFromGeoJson(context: Context, fileName: String = "locations.geojson"): List<LocationFeature> {
    val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
    val featureCollection = FeatureCollection.fromJson(jsonString)
    return featureCollection.features()?.mapNotNull { feature ->
        val name = feature.getStringProperty("name")
        val point = feature.geometry() as? Point
        if (point != null && name != null) {
            LocationFeature(name, point.latitude(), point.longitude())
        } else null
    } ?: emptyList()
}
