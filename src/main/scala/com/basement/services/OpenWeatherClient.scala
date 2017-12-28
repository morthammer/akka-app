package com.basement.services

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.basement.bootstrap.ActorRegistry
import com.basement.domain._
import com.typesafe.config.ConfigFactory
import dispatch._
import org.joda.time.DateTime
import org.json4s.jackson.JsonMethods._
import org.json4s._
import akka.pattern.pipe


object OpenWeatherClient {
  def apply(actorSystem: ActorSystem, weatherManager: ActorRef) = actorSystem.actorOf(Props(classOf[OpenWeatherClient], weatherManager), ActorRegistry.weatherClient)
  val openWeatherConfig =  ConfigFactory.load().getConfig("openWeather")
  val serviceUrl = openWeatherConfig.getString("serviceUrl")
  val apiKey = openWeatherConfig.getString("apiKey")

  def toWeather(w:WeatherAPIResult): Weather = {
    Weather(id = Some(w.sys.id), currentTemp = Some(w.main.temp), tempMin = Some(w.main.tempMin), tempMax = Some(w.main.tempMax), timestamp = new DateTime().getMillis, pressure = Some(w.main.pressure),
      humidity = Some(w.main.humidity), description = w.weather.headOption.map(_.description), windSpeed = Some(w.wind.speed))
  }
}

class OpenWeatherClient(weatherManager: ActorRef ) extends Actor with ActorLogging {
  import OpenWeatherClient._
  implicit val formats = DefaultFormats
  implicit val ec = context.system.dispatcher

  val weatherService = (city: String) => Http.default(url(serviceUrl) <<? Map("q" -> city, "APPID" -> apiKey) OK as.String)

  def receive = {
    case FetchWeatherForGeoPoint(geoPoints) => pipe(executeWeatherServiceForGeoPoints(geoPoints)) to sender()
  }

  def executeWeatherService(gp:GeoPoint): Future[Weather] = {
    weatherService(gp.city).map{
      weatherJson =>
        toWeather(parse(weatherJson).camelizeKeys.extract[WeatherAPIResult])
    }
  }

  def executeWeatherServiceForGeoPoints(points: Vector[GeoPoint]): Future[GeoPointsWithWeather] = {
    Future.traverse(points)(gp => executeWeatherService(gp).map(w => gp.copy(weather = Some(w)))).map(GeoPointsWithWeather.apply)
  }
}

case class WeatherAPIResults(weatherApiResult: WeatherAPIResult)
case class WeatherAPIResult(coord: Coordinates, weather: List[WeatherAPI], base: String, main: Main, visibility: BigDecimal, wind: Wind, clouds: Clouds, dt: BigDecimal, sys: System, id: BigDecimal, name: String, cod: Long)
case class Coordinates(lon: BigDecimal, lat: BigDecimal)
case class WeatherAPI(id:Long, main: String, description: String, icon: String)
case class Main(temp: BigDecimal, pressure: BigDecimal, humidity: BigDecimal, tempMin: BigDecimal, tempMax: BigDecimal)
case class Wind(speed: BigDecimal, deg: BigDecimal)
case class Clouds(all: Long)
case class System(`type`: Int, id: Long, message: BigDecimal, country: String, sunrise: BigDecimal, sunset: BigDecimal)
