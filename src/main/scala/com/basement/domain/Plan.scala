package com.basement.domain

import org.joda.time.DateTime

case class Point(place: String, time: DateTime, weather: Option[Weather] = None)
case class Weather(temp: BigDecimal, percip:BigDecimal, cloudCover: BigDecimal, dewPoint: BigDecimal, humiditiy: BigDecimal, windSpeed: BigDecimal)
case class Trip(trip: List[Point])
