package com.basement.services

import akka.actor.{ActorSystem, Status}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.basement.domain._
import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Future

class GeoPointManagerUnitSpec extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with MockitoSugar {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val weatherManager = TestProbe()
  val geoPointLookup = TestProbe()

  val mockDAO = mock[GeoPointManagerDAO]
  val actorRef = TestActorRef(new GeoPointManager(weatherManager.ref, geoPointLookup.ref){override val dao = mockDAO})
  val dateTime = new DateTime()
  val point = GeoPoint(1,2,"Boston","MA",dateTime.getMillis, None)
  val weather = Weather(id = Some(1), currentTemp = Some(1), tempMin = Some(1), tempMax = Some(1), timestamp = 1L, pressure = Some(1), humidity = Some(1), description = Some("desc"), windSpeed = Some(1))
  val plotId = 111L

  val pointInput = GeoPointInputsWithPlotId(Vector(GeoPointInput("Cleveland", "Ohio")), plotId)

  implicit val ec = system.dispatcher

  "point manager" must {
    "return geopoint vector when insert is successful" in {
      when(mockDAO.insert(GeoPointInput(any[String], any[String], any[Option[BigDecimal]]), plotId)).thenReturn(Future.successful(1))
      actorRef ! Create(pointInput)
      geoPointLookup.expectMsg(FindByPlotId(plotId))
      geoPointLookup.reply(GeoPoints(Vector(point)))
      expectMsg(GeoPoints(Vector(point)))
    }
    "return failure when insert is failure" in {
      val ex = new Exception("fail")
      when(mockDAO.insert(GeoPointInput(any[String], any[String], any[Option[BigDecimal]]), plotId)).thenReturn(Future.failed(ex))
      actorRef ! Create(pointInput)
      expectMsg(Status.Failure(ex))
    }
    "update point and weather" in {
      val pointWithWeather = point.copy(weather = Some(weather))
      val gpWithWeather = GeoPointsWithWeather(Vector(pointWithWeather))
      when(mockDAO.update(pointWithWeather)).thenReturn(Future.successful(1))
      actorRef ! Update(gpWithWeather)
      weatherManager.expectMsg(Upsert(weather))
      expectMsg(Vector(OperationResult(true)))
    }
    "return failure when update point and weather fails" in {
      val ex = new Exception("fail")
      val pointWithWeather = point.copy(weather = Some(weather))
      val gpWithWeather = GeoPointsWithWeather(Vector(pointWithWeather))
      when(mockDAO.update(pointWithWeather)).thenReturn(Future.failed(ex))
      weatherManager.expectNoMsg
      actorRef ! Update(gpWithWeather)
      expectMsg(Status.Failure(ex))
    }
    "return true operation result when delete is successful" in {
      when(mockDAO.delete(1l)).thenReturn(Future.successful(1))
      actorRef ! Delete(1l)
      expectMsg(OperationResult(true))
    }
    "return false operation result when delete is failure" in {
      val ex = new Exception("fail")
      when(mockDAO.delete(1l)).thenReturn(Future.failed(ex))
      actorRef ! Delete(1l)
      expectMsg(Status.Failure(ex))
    }
  }
}
