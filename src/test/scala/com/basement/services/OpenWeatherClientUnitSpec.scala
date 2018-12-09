package com.basement.services

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.basement.domain._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import scala.concurrent.duration._

import scala.concurrent.Future

class OpenWeatherClientUnitSpec extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with MockitoSugar {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
  val weather = Weather(id = Some(1), currentTemp = Some(1), tempMin = Some(1), tempMax = Some(1), timestamp = 1L, pressure = Some(1), humidity = Some(1), description = Some("desc"), windSpeed = Some(1))
  val point = GeoPoint(1,2,"Boston","MA",1l, Some(weather))

  val weatherServiceMock = mock[(String) => dispatch.Future[String]]
  val weatherManager = TestProbe()

  val actorRef = TestActorRef(new OpenWeatherClient(system, weatherManager.ref){
    override val weatherService = weatherServiceMock
  })

  val weatherResult = WeatherAPIResult(Coordinates(-0.13,51.51),List(WeatherAPI(300,"Drizzle","light intensity drizzle","09d")),"stations",Main(280.32,1012,81,279.15,281.15),10000,Some(Wind(4.1,80)),Some(Clouds(90)),1485789600,System(1,5091,0.0103,"GB",1485762037,1485794875),2643743,"London",200)

  val jsonResponse = """{"coord":{"lon":-0.13,"lat":51.51},"weather":[{"id":300,"main":"Drizzle","description":"light intensity drizzle","icon":"09d"}],"base":"stations","main":{"temp":280.32,"pressure":1012,"humidity":81,"temp_min":279.15,"temp_max":281.15},"visibility":10000,"wind":{"speed":4.1,"deg":80},"clouds":{"all":90},"dt":1485789600,"sys":{"type":1,"id":5091,"message":0.0103,"country":"GB","sunrise":1485762037,"sunset":1485794875},"id":2643743,"name":"London","cod":200}"""

  implicit val ec = system.dispatcher

  "open weather client" must {
    "return WeatherApiResult from open weather" in {
      when(weatherServiceMock("Boston")).thenReturn(Future.successful(jsonResponse))
      actorRef ! FetchWeatherForGeoPoint(Vector(point))
      expectMsgPF(1 second, ""){
        case GeoPointsWithWeather(Vector(p)) => assert(p.weather.get.copy(timestamp = 1l) == OpenWeatherClient.toWeather(weatherResult).copy(timestamp = 1l))
      }
    }
  }
}
