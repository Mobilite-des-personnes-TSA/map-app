package com.example.myapplication.tisseo

import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadNode
import org.osmdroid.util.GeoPoint

object TisseoOsrmUtils {

    private fun String.wktToGeoPoints(): List<GeoPoint> {
        return substringAfter("(").substringBeforeLast(")")
            .removePrefix("(").removeSuffix(")").split(",")
            .map { it.trim().split(" ") }
            .map { GeoPoint(it[1].toDouble(), it[0].toDouble()) }
    }

    fun journeyToRoad(journey: Journey): Road {
        val road = Road()
        journey.chunks.forEach { chunk ->
            chunk.service?.also { service ->
                val geoPoints = service.wkt.wktToGeoPoints()
                val roadNode = RoadNode().apply {
                    mInstructions = service.text.text
                    mDuration = service.duration.seconds.toDouble()
                    mLocation = geoPoints.first()
                    mManeuverType = 1
                }
                geoPoints.forEach(road.mRouteHigh::add)
                road.mNodes.add(roadNode)
            }
            chunk.street?.also { street ->
                val geoPoints = street.wkt.wktToGeoPoints()
                val roadNode = RoadNode().apply {
                    mInstructions = street.text.text
                    mDuration = street.duration.seconds.toDouble()
                    mLength = street.length.toDouble() / 1000
                    mLocation = GeoPoint(
                        street.startAddress.connectionPlace.latitude.toDouble(),
                        street.startAddress.connectionPlace.longitude.toDouble()
                    )
                    mManeuverType = 1
                }
                geoPoints.forEach(road.mRouteHigh::add)
                road.mNodes.add(roadNode)
            }
        }
        return road
    }

    fun addressToGeoPoint(place: String): GeoPoint? {
        return TisseoApiClient.places(place, "", "fr")?.let { result ->
            result.placesList.place.getOrNull(0)?.let {
                GeoPoint(it.x, it.y)
            }
        }
    }
}