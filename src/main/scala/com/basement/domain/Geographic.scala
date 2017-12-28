package com.basement.domain

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.{HttpCookiePair}
import org.joda.time.DateTime
import spray.json._

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val weatherFormat = jsonFormat9(Weather)
  implicit val geoPointFormat = jsonFormat6(GeoPoint)
  implicit val geoPlotFormat = jsonFormat3(GeoPlot)
  implicit val operationResultFormat = jsonFormat1(OperationResult)
  implicit val geoPlotInputFormat = jsonFormat3(GeoPointInput)
  implicit val geoPlotInputsFormat = jsonFormat1(GeoPointInputs)
}

/**
  * Trait that represents a message passed to an actor
  */
trait Message

case class OperationResult(isSuccess:Boolean) extends Message
case class FindById(id: Long) extends Message
case class FindPlotByCookie(cookieVal: String) extends Message
case object FindAll extends Message
case class FindByPlotId(tripId: Long) extends Message
case class Create[T](obj:T) extends Message
case class Update[T](obj:T) extends Message
case class Upsert[T](obj:T) extends Message
case class Delete(id:Long) extends Message
case class FetchWeatherForGeoPoint(points: Vector[GeoPoint])
case class UpdatedWeather(weather: Vector[Weather])
case class FetchWeather(city: String, state: String) extends Message
case class IdentifyGeoPlot(cookie: Option[HttpCookiePair]) extends Message
case class GeoPointInput(city: String, state: String, timestamp: Option[BigDecimal] = Some(DateTime.now().getMillis)) extends Message
case class GeoPointInputs(geoInputPoints: Vector[GeoPointInput]) extends Message
case class GeoPointInputsWithPlotId(geoPointInputs: Vector[GeoPointInput], plotId: Long) extends Message
case class FindByToken(cookie: String) extends Message
case class GeoPointsWithWeather(geoPoints: Vector[GeoPoint]) extends Message
case class UpdatedGeoPlot(updated: GeoPlot) extends Message
case class CreateGeoPlot(geoPlotInputs: GeoPointInputs) extends Message
case class CreatePlotId(token: String) extends Message

/**
  * Trait that represents data
  */
trait Payload

case class GeoPoint(id: Long, plotId: Long, city: String, state: String, timestamp: Long, weather: Option[Weather] = None) extends Payload
case class Weather(id: Option[Long], currentTemp: Option[BigDecimal], tempMin: Option[BigDecimal], tempMax: Option[BigDecimal], timestamp:Long, pressure:Option[BigDecimal], humidity: Option[BigDecimal], description: Option[String], windSpeed: Option[BigDecimal]) extends Payload
case class GeoPlot(id: Long, token: String, points: Vector[GeoPoint]) extends Payload