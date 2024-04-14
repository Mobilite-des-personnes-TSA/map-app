package com.example.myapplication.tisseo

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonClassDiscriminator
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder


object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    private val ISO_LOCAL_DATE_TIME =
        DateTimeFormatterBuilder().parseCaseInsensitive().append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral(' ').append(DateTimeFormatter.ISO_LOCAL_TIME).toFormatter()

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) =
        encoder.encodeString(value.format(ISO_LOCAL_DATE_TIME))


    override fun deserialize(decoder: Decoder) =
        LocalDateTime.parse(decoder.decodeString(), ISO_LOCAL_DATE_TIME)!!

}


data class LineResponse(
    @Serializable(with = LocalDateTimeSerializer::class) val expirationDate: LocalDateTime,
    val lines: List<Line>
)

@Serializable
data class Line(
    val id: String,
    val shortName: String,
    val name: String,
    val network: String,
    val color: String,
    val bgXmlColor: String,
    val fgXmlColor: String,
    val transportMode: TransportMode,
    val terminus: List<Terminus>? = null,
    val messages: List<Message>? = null,
    val geometry: String? = null
) {
    @Serializable
    data class TransportMode(
        val id: String, val article: String, val name: String
    )

    @Serializable
    data class Terminus(
        val id: String, val cityName: String, val name: String
    )

    @Serializable
    data class Message(
        val id: String,
        val type: String,
        val importanceLevel: String,
        val scope: String,
        val title: String,
        val content: String,
        val url: String,
        val lines: List<LineImpact>? = null
    ) {
        @Serializable
        data class LineImpact(
            val id: String,
            val shortName: String,
            val name: String,
            val network: String,
            val color: String,
            val bgXmlColor: String,
            val fgXmlColor: String,
            val transportMode: TransportMode
        )
    }
}


@Serializable
@ExperimentalSerializationApi
data class PlacesResponse(
    @Serializable(with = LocalDateTimeSerializer::class) val expirationDate: LocalDateTime,
    val placesList: PlacesList
) {

    @Serializable
    data class PlacesList(
        val place: List<Place>
    ) {

        @Serializable
        @SerialName("place")
        @JsonClassDiscriminator("className")
        sealed class Place {
            @Serializable
            @SerialName("stop")
            data class Stop(
                val category: String,
                val id: String,
                val key: String,
                val label: String,
                val network: String,
                val rank: Int,
                val x: Double,
                val y: Double
            ) : Place()

            @Serializable
            @SerialName("road")
            data class Road(
                val id: String,
                val label: String,
                val category: String,
                val key: String,
                val x: Double,
                val y: Double,
                val rank: Int
            ) : Place()

            @Serializable
            @SerialName("public_place")
            data class PublicPlace(
                val id: String,
                val label: String,
                val cityName: String? = null,
                val postcode: String? = null,
                val address: String? = null,
                val key: String,
                val category: String,
                val x: Double,
                val y: Double,
                val typeCompressed: PublicPlaceType,
                val rank: Int,
                val type: String? = null,
                val code: String? = null,
                @SerialName("veloStation") val bikeStation: Int? = null
            ) : Place() {
                @Serializable
                enum class PublicPlaceType {
                    @SerialName("a")
                    ADMINISTRATION,

                    @SerialName("b")
                    POST,

                    @SerialName("c")
                    EDUCATION,

                    @SerialName("d")
                    HOSPITAL,

                    @SerialName("e")
                    POLICE,

                    @SerialName("fd")
                    CHURCH,

                    @SerialName("fe")
                    MOSQUE,

                    @SerialName("ff")
                    SYNAGOGUE,

                    @SerialName("g")
                    CEMETERY,

                    @SerialName("h")
                    BUS_STATION,

                    @SerialName("i")
                    TRAIN_STATION,

                    @SerialName("j")
                    AIRPORT,

                    @SerialName("k")
                    BIKE_TOULOUSE,

                    @SerialName("l")
                    PARKING,

                    @SerialName("m")
                    PARK_AND_RIDE,

                    @SerialName("n")
                    TISSEO_AGENCY,

                    @SerialName("o")
                    PARTNER_MERCHANTS,

                    @SerialName("p")
                    FACULTY,

                    @SerialName("qa")
                    FOOTBALL_STADIUM,

                    @SerialName("qb")
                    RUGBY_STADIUM,

                    @SerialName("qc")
                    OTHER_SPORTS_FACILITY,

                    @SerialName("r")
                    LEISURE_CULTURE,

                    @SerialName("s")
                    PUBLIC_GARDEN,

                    @SerialName("t")
                    CITIZ,

                    @SerialName("u")
                    BIKE_PARK,

                    @SerialName("v")
                    CARPOOL_STATION;
                }
            }

            @Serializable
            @SerialName("address")
            data class Address(
                val id: String,
                val label: String,
                val category: String,
                val key: String,
                val x: Double,
                val y: Double,
                val rank: Int
            ) : Place()
        }
    }
}

@Serializable
data class JourneyResponse(
    @Serializable(with = LocalDateTimeSerializer::class) val expirationDate: LocalDateTime,
    val routePlannerResult: RoutePlannerResult
) {
    @Serializable
    data class RoutePlannerResult(
        val journeys: List<JourneyItem>, val query: Query
    ) {
        @Serializable
        data class JourneyItem(
            val journey: Journey
        ) {
            @Serializable
            data class Journey(
                @Serializable(with = LocalDateTimeSerializer::class) val arrivalDateTime: LocalDateTime,
                val arrivalText: Text? = null,
                val chunks: List<Chunk>,
                @SerialName("co2_emissions") val co2Emissions: String,
                @Serializable(with = LocalDateTimeSerializer::class) val departureDateTime: LocalDateTime,
                val duration: String
            ) {

                @Serializable
                data class Text(
                    val lang: String, val text: String
                )

                @Serializable
                data class Chunk(
                    val stop: Stop? = null, val service: Service? = null, val street: Street? = null
                ) {

                    @Serializable
                    data class Street(
                        val length: String,
                        val wkt: String,
                        val roadMode: String,
                        val startAddress: StartAddress,
                        val endAddress: EndAddress,
                        val text: Text
                    ) {
                        @Serializable
                        data class StartAddress(
                            val connectionPlace: ConnectionPlace
                        ) {
                            @Serializable
                            data class ConnectionPlace(
                                val latitude: String, val longitude: String
                            )
                        }

                        @Serializable
                        data class EndAddress(
                            val address: Address
                        ) {
                            @Serializable
                            data class Address(
                                val latitude: String, val longitude: String, val streetName: String
                            )
                        }
                    }

                    @Serializable
                    data class Stop(
                        @SerialName("arrival_id") val arrivalId: String,
                        val connectionPlace: ConnectionPlace,
                        @SerialName("departure_id") val departureId: String,
                        val firstTime: String,
                        val lastTime: String,
                        val latitude: String,
                        val longitude: String,
                        val name: String,
                        val text: Text? = null
                    ) {
                        @Serializable
                        data class ConnectionPlace(
                            val city: String,
                            val id: String,
                            val latitude: String,
                            val longitude: String,
                            val name: String,
                            val x: String,
                            val y: String
                        )
                    }

                    @Serializable
                    data class Service(
                        val destinationStop: DestinationStop,
                        val duration: String,
                        val firstArrivalTime: String,
                        val firstDepartureTime: String,
                        val isContinuousService: String,
                        val lastArrivalTime: String,
                        val lastDepartureTime: String,
                        val maxWaitingTime: String,
                        val name: String,
                        val text: Text? = null,
                        val wkt: String
                    ) {
                        @Serializable
                        data class DestinationStop(
                            val id: String, val line: DestinationLine, val name: String
                        ) {
                            @Serializable
                            data class DestinationLine(
                                val id: String,
                                val shortName: String,
                                val name: String,
                                val network: String,
                                val color: String,
                                val bgXmlColor: String,
                                val fgXmlColor: String,
                                val transportMode: Line.TransportMode,
                                val message: List<Line.Message>,
                                @SerialName("service_number") val serviceNumber: String,
                                val specificPricing: String,
                                val style: String
                            )
                        }
                    }
                }
            }
        }

        @Serializable
        data class Query(
            val maxSolutions: String,
            val places: Places,
            val roadMode: String,
            val timeBounds: TimeBounds
        ) {
            @Serializable
            data class Places(
                val arrivalCity: String,
                val arrivalLatitude: String,
                val arrivalLongitude: String,
                val arrivalStop: String,
                val departureCity: String,
                val departureLatitude: String,
                val departureLongitude: String,
                val departureStop: String
            )

            @Serializable
            data class TimeBounds(
                val maxDepartureHour: String, val minArrivalHour: String
            )
        }
    }
}