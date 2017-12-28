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

class GeoPlotManagerUnitSpec extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with MockitoSugar {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val geoPlotLookup = TestProbe()
  val geoPointManager = TestProbe()
  val geoPointLookup = TestProbe()

  val mockDAO = mock[GeoPlotManagerDAO]
  val actorRef = TestActorRef(new GeoPlotManager(geoPlotLookup.ref, geoPointManager.ref, geoPointLookup.ref){override val dao = mockDAO})
  val dateTime = new DateTime()
  val point = GeoPoint(1,2,"Boston","MA",dateTime.getMillis, None)
  val weather = Weather(id = Some(1), currentTemp = Some(1), tempMin = Some(1), tempMax = Some(1), timestamp = 1L, pressure = Some(1), humidity = Some(1), description = Some("desc"), windSpeed = Some(1))
  val plotId = 2L
  val token = "123-1234-12345"
  val geoPlot = GeoPlot(2l, token, Vector(point))

  val pointInput = GeoPointInputsWithPlotId(Vector(GeoPointInput("Cleveland", "Ohio")), plotId)

  implicit val ec = system.dispatcher

  "plot manager" must {
    "return plotId when insert is successful" in {
      when(mockDAO.insert(token)).thenReturn(Future.successful(1))
      actorRef ! CreatePlotId(token)
      geoPlotLookup.expectMsg(FindByToken(token))
      geoPlotLookup.reply(Some(geoPlot))
      expectMsg(Some(geoPlot.id))
     }
    "return failure when dao returns failure" in {
      val ex = new Exception("fail")
      when(mockDAO.insert(token)).thenReturn(Future.failed(ex))
      actorRef ! CreatePlotId(token)
      geoPlotLookup.expectNoMsg
      expectMsg(Status.Failure(ex))
    }
    "return failure when plotLookup returns failure" in {
      val ex = new Exception("fail")
      when(mockDAO.insert(token)).thenReturn(Future.successful(1))
      actorRef ! CreatePlotId(token)
      geoPlotLookup.expectMsg(FindByToken(token))
      geoPlotLookup.reply(Status.Failure(ex))
      expectMsg(Status.Failure(ex))
    }
  }
}
