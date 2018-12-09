package com.basement.restapi

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestProbe
import com.basement.domain.{FindAll, FindById, GeoPoint, GeoPoints}
import org.joda.time.DateTime
import org.scalatest.{Matchers, WordSpec}

class GeoPointRouteUnitSpec extends WordSpec with Matchers with ScalatestRouteTest with GeoPointRoute{

  val pointLookupProbe = TestProbe()
  val pointManagerProbe = TestProbe()

  val pointLookup = pointLookupProbe.ref
  val pointManager = pointManagerProbe.ref
  val dateTime = new DateTime()

  "The point rest api" should {
    val point = GeoPoint(1,2,"Boston","MA",dateTime.getMillis, None)

    "return success when request for all points" in {
      val result = Get("/points") ~> routes(pointLookup, pointManager)
      pointLookupProbe.expectMsg(FindAll)
      pointLookupProbe.reply(GeoPoints(Vector(point)))

      check {
        status shouldEqual StatusCodes.OK
        responseAs[GeoPoints] shouldEqual GeoPoints(Vector(point))
      }(result)
    }
    "return success when making a request for point id 1" in {
      val result = Get("/point/1") ~> routes(pointLookup, pointManager)
      pointLookupProbe.expectMsg(FindById(1))
      pointLookupProbe.reply(Some(point))

      check {
        status shouldEqual StatusCodes.OK
        responseAs[GeoPoint] shouldEqual point
      }(result)
    }
    "return error when lookup actor returns a failure" in {
      val result = Get("/point/1") ~> routes(pointLookup, pointManager)
      pointLookupProbe.expectMsg(FindById(1))
      pointLookupProbe.reply(new Exception("boom!"))

      check {
        status shouldEqual StatusCodes.InternalServerError
      }(result)
    }
  }
}
