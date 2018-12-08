package com.basement.services

import akka.actor.{ActorSystem, Status}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.basement.domain._
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.{ExecutionContext, Future}

class GeoPlotLookupUnitSpec extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with MockitoSugar {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val mockDAO = mock[GeoPlotLookupDAO]
  val geoLookupProbe = TestProbe()
  val weather = Weather(id = Some(1), currentTemp = Some(1), tempMin = Some(1), tempMax = Some(1), timestamp = 1L, pressure = Some(1), humidity = Some(1), description = Some("desc"), windSpeed = Some(1))

  val actorRef = TestActorRef(new GeoPlotLookup(geoLookupProbe.ref){override val dao = mockDAO})
  val dateTime = new DateTime()
  val token = "123-1234-12345"
  val point = GeoPoint(1,2,"Boston","MA",dateTime.getMillis, Some(weather))
  val geoPlot = GeoPlot(1l, token, Vector(point))
  val pointResponse = Vector(point)
  implicit val ec:ExecutionContext = system.dispatcher

  "plot lookup" must {
    "successfully return plot when token exists" in {
      when(mockDAO.selectByToken(token)).thenReturn(Future.successful(Vector(geoPlot)))
      actorRef ! FindByToken(token)
      geoLookupProbe.expectMsg(FindByPlotId(1l))
      geoLookupProbe.reply(GeoPoints(Vector(point)))
      expectMsg(Some(geoPlot))
    }
    "return none when token doesn't exist" in {
      when(mockDAO.selectByToken(token)).thenReturn(Future.successful(Vector()))
      actorRef ! FindByToken(token)
      expectMsg(None)
    }
    "return failure when dao selectByToken fails" in {
      val ex = new Exception("Failure!")
      when(mockDAO.selectByToken(token)).thenReturn(Future.failed(ex))
      actorRef ! FindByToken(token)
      expectMsg(Status.Failure(ex))
    }
  }
}
