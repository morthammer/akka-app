package com.basement.FSM

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.HttpCookiePair
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.basement.FSM.PlotWorker.RequestFormatException
import com.basement.domain._
import com.basement.services.GeoPointManagerDAO
import org.joda.time.DateTime
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.util.Failure

class PlotWorkerUnitSpec extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with MockitoSugar {
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val openWeatherClient = TestProbe()
  val geoPointLookup = TestProbe()
  val geoPointManager = TestProbe()
  val geoPlotManager = TestProbe()
  val geoPlotLookup = TestProbe()

  val dependencies = PlotWorker.FSMModel.Dependencies(openWeatherClient.ref, geoPointLookup.ref, geoPointManager.ref, geoPlotManager.ref, geoPlotLookup.ref)
  val token = "1234-123421312-421332"

  val mockDAO = mock[GeoPointManagerDAO]

  val dateTime = new DateTime()
  val point = GeoPoint(1,2,"Boston","MA",dateTime.getMillis, None)
  val weather = Weather(id = Some(1), currentTemp = Some(1), tempMin = Some(1), tempMax = Some(1), timestamp = 1L, pressure = Some(1), humidity = Some(1), description = Some("desc"), windSpeed = Some(1))
  val plotId = 111L
  val wtCookie = HttpCookiePair("weathertracker", token)
  val geoPlot = GeoPlot(1l, token, Vector(point))
  val ex = new Exception("error!")

  val geoPointInput = Vector(GeoPointInput("Cleveland", "Ohio"))

  val pointInputWithPlotId = GeoPointInputsWithPlotId(geoPointInput, plotId)
  val updatedWeather = Vector(point.copy(timestamp = new DateTime().getMillis, weather = Some(weather)))

  implicit val ec = system.dispatcher
 "the identify geo plot message to plot handler" should {
    "identify a plot by cookie token and reply with geoPlot without refreshing weather" in {
      val actorRef = TestActorRef(new PlotWorker(dependencies))

      actorRef ! IdentifyGeoPlot(wtCookie)

      geoPlotLookup.expectMsg(FindByToken(wtCookie.value))
      geoPlotLookup.reply(Some(geoPlot))

      expectMsg(geoPlot)
    }
   "return failure when geoPlotLookup fails" in {
     val actorRef = TestActorRef(new PlotWorker(dependencies))
     actorRef ! IdentifyGeoPlot(wtCookie)

     geoPlotLookup.expectMsg(FindByToken(wtCookie.value))
     geoPlotLookup.reply(ex)

     expectMsg(Failure(ex))
   }
    "identify a plot by cookie token and refresh the weather only for expired points" in {
      GeoPoint(1,2,"Boston","MA",new DateTime().minusDays(1).getMillis, None)
      val ptExpired =  GeoPoint(11,2,"Denver","CO",new DateTime().minusDays(1).getMillis, None)
      val ptExpired2 = GeoPoint(12,2,"San Francisco","CA",new DateTime().minusDays(1).getMillis, None)

      val updatedWeather = Vector(ptExpired.copy(timestamp = new DateTime().getMillis, weather = Some(weather)),
        ptExpired2.copy(timestamp = new DateTime().getMillis, weather = Some(weather)))

      val points = Vector(ptExpired, ptExpired2, point)
      val actorRef = TestActorRef(new PlotWorker(dependencies))
      actorRef ! IdentifyGeoPlot(wtCookie)

      geoPlotLookup.expectMsg(FindByToken(wtCookie.value))
      geoPlotLookup.reply(Some(geoPlot.copy(points = points)))

      openWeatherClient.expectMsg(FetchWeatherForGeoPoint(Vector(ptExpired, ptExpired2)))
      openWeatherClient.reply(GeoPointsWithWeather(updatedWeather))

      geoPointManager.expectMsg(Update(GeoPointsWithWeather(updatedWeather)))

      expectMsg(geoPlot.copy(points = point +: updatedWeather))
    }
   "return failure when openWeatherClient fails" in {
     GeoPoint(1,2,"Boston","MA",new DateTime().minusDays(1).getMillis, None)
     val ptExpired =  GeoPoint(11,2,"Denver","CO",new DateTime().minusDays(1).getMillis, None)
     val ptExpired2 = GeoPoint(12,2,"San Francisco","CA",new DateTime().minusDays(1).getMillis, None)

     val updatedWeather = Vector(ptExpired.copy(timestamp = new DateTime().getMillis, weather = Some(weather)),
       ptExpired2.copy(timestamp = new DateTime().getMillis, weather = Some(weather)))

     val points = Vector(ptExpired, ptExpired2, point)
     val actorRef = TestActorRef(new PlotWorker(dependencies))
     actorRef ! IdentifyGeoPlot(wtCookie)

     geoPlotLookup.expectMsg(FindByToken(wtCookie.value))
     geoPlotLookup.reply(Some(geoPlot.copy(points = points)))

     openWeatherClient.expectMsg(FetchWeatherForGeoPoint(Vector(ptExpired, ptExpired2)))
     openWeatherClient.reply(ex)

     geoPointManager.expectNoMsg()

     expectMsg(Failure(RequestFormatException("Weather could not be found")))
   }
  }
  "the create geo plot message to plot handler" should{
    "create a new geo plot and respond" in {
      val actorRef = TestActorRef(new PlotWorker(dependencies){
        override def createToken = token
      })

      actorRef ! CreateGeoPlot(GeoPointInputs(geoPointInput))

      geoPlotManager.expectMsg(CreatePlotId(token))
      geoPlotManager.reply(Some(2l))

      geoPointManager.expectMsg(Create(GeoPointInputsWithPlotId(geoPointInput, 2l)))
      geoPointManager.reply(GeoPoints(Vector(point)))

      openWeatherClient.expectMsg(FetchWeatherForGeoPoint(Vector(point)))
      openWeatherClient.reply(GeoPointsWithWeather(updatedWeather))

      geoPointManager.expectMsg(Update(GeoPointsWithWeather(updatedWeather)))

      expectMsg(geoPlot.copy(id = 2, points = updatedWeather))
    }
  }

}
