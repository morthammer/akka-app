package com.basement.services

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.basement.domain._
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Future

class WeatherManagerUnitSpec extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with MockitoSugar {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val mockDAO = mock[WeatherManagerDAO]
  val actorRef = TestActorRef(new WeatherManager{override val dao = mockDAO})
  val dateTime = new DateTime()
  val weather = Weather(id = Some(1), currentTemp = Some(1), tempMin = Some(1), tempMax = Some(1), timestamp = 1L, pressure = Some(1), humidity = Some(1), description = Some("desc"), windSpeed = Some(1))

  implicit val ec = system.dispatcher

  "weather manager" must {
    "return true operation result when insert is successful" in {
      when(mockDAO.insert(weather)).thenReturn(Future.successful(1))
      actorRef ! Create(weather)
      expectMsg(OperationResult(true))
    }
    "return false operation result when insert is failure" in {
      when(mockDAO.insert(weather)).thenReturn(Future.failed(new Exception("fail")))
      actorRef ! Create[Weather](weather)
      expectMsg(OperationResult(false))
    }
    "return true operation result when update is successful" in {
      when(mockDAO.update(weather)).thenReturn(Future.successful(1))
      actorRef ! Update[Weather](weather)
      expectMsg(OperationResult(true))
    }
    "return false operation result when update is failure" in {
      when(mockDAO.update(weather)).thenReturn(Future.failed(new Exception("fail")))
      actorRef ! Update[Weather](weather)
      expectMsg(OperationResult(false))
    }
    "return true operation result when delete is successful" in {
      when(mockDAO.delete(1)).thenReturn(Future.successful(1))
      actorRef ! Delete(1l)
      expectMsg(OperationResult(true))
    }
    "return false operation result when delete is failure" in {
      when(mockDAO.delete(1l)).thenReturn(Future.failed(new Exception("fail")))
      actorRef ! Delete(1l)
      expectMsg(OperationResult(false))
    }
  }
}
