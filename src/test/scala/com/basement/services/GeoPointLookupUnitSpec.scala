package com.basement.services

import java.sql.Timestamp

import akka.actor.{ActorSystem, Status}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.basement.domain._
import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.{ExecutionContext, Future}

class GeoPointLookupUnitSpec extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with MockitoSugar {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val mockDAO = mock[GeoPointLookupDAO]
  val actorRef = TestActorRef(new GeoPointLookup{override val dao = mockDAO})
  val dateTime = new DateTime()
  val point = GeoPoint(1,2,"Boston","MA",dateTime.getMillis, None)
  val pointResponse = Vector(point)
  implicit val ec:ExecutionContext = system.dispatcher

  "point lookup" must {
    "successfully return point when id exists" in {
      when(mockDAO.selectPoints(2)).thenReturn(Future.successful(pointResponse))
      actorRef ! FindById(2)
      expectMsg(Some(point))
    }
    "return none when id doesn't exist" in {
      when(mockDAO.selectPoints(2)).thenReturn(Future.successful(Nil))
      actorRef ! FindById(2)
      expectMsg(None)
    }
    "return failure when dao selectPoints fails" in {
      val ex = new Exception("Failure!")
      when(mockDAO.selectPoints(2)).thenReturn(Future.failed(ex))
      actorRef ! FindById(2)
      expectMsg(Status.Failure(ex))
    }
    "successfully return points by plot id when id exists" in {
      when(mockDAO.selectPointsByPlotId(2)).thenReturn(Future.successful(pointResponse))
      actorRef ! FindByPlotId(2)
      expectMsg(pointResponse)
    }
    "return none when plot id doesn't exist" in {
      when(mockDAO.selectPointsByPlotId(2)).thenReturn(Future.successful(Nil))
      actorRef ! FindByPlotId(2)
      expectMsg(Nil)
    }
    "return failure when dao selectPointsByPlotId fails" in {
      val ex = new Exception("Failure!")
      when(mockDAO.selectPointsByPlotId(2)).thenReturn(Future.failed(ex))
      actorRef ! FindByPlotId(2)
      expectMsg(Status.Failure(ex))
    }
    "successfully return all points" in {
      when(mockDAO.selectAllPoints).thenReturn(Future.successful(pointResponse))
      actorRef ! FindAll
      expectMsg(pointResponse)
    }
    "return none when no plots" in {
      when(mockDAO.selectAllPoints).thenReturn(Future.successful(Nil))
      actorRef ! FindAll
      expectMsg(Nil)
    }
    "return failure when dao selectAllPoints fails" in {
      val ex = new Exception("Failure!")
      when(mockDAO.selectAllPoints).thenReturn(Future.failed(ex))
      actorRef ! FindAll
      expectMsg(Status.Failure(ex))
    }
  }
}
