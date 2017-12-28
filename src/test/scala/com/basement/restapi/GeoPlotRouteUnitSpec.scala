package com.basement.restapi

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Cookie, HttpCookie, HttpCookiePair, `Set-Cookie`}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestProbe
import com.basement.domain._
import org.joda.time.DateTime
import org.scalatest.{Matchers, WordSpec}

class GeoPlotRouteUnitSpec extends WordSpec with Matchers with ScalatestRouteTest with GeoPlotRoutes{

  val plotHandlerProbe = TestProbe()

  val plotHandler = plotHandlerProbe.ref
  val dateTime = new DateTime()

  "The plot rest api" should {
    val plot = GeoPlot(1,"1234-123421312-421332", Vector())
    val wtCookie = HttpCookiePair("weathertracker", "1234-123421312-421332")

    "return success when request for plot with cookie" in {
      val result = Get("/plot") ~> Cookie("weathertracker", "1234-123421312-421332") ~> routes(plotHandler)
      plotHandlerProbe.expectMsg(IdentifyGeoPlot(Some(wtCookie)))
      plotHandlerProbe.reply(plot)

      check {
        status shouldEqual StatusCodes.OK
        responseAs[GeoPlot] shouldEqual plot
      }(result)
    }
    "return success when request for plot with no cookie" in {
      val result = Get("/plot") ~> routes(plotHandler)
      plotHandlerProbe.expectMsg(IdentifyGeoPlot(None))
      plotHandlerProbe.reply(plot)

      check {
        status shouldEqual StatusCodes.OK
        responseAs[GeoPlot] shouldEqual plot
      }(result)
    }
    "return success when creating new plot" in {
      val geoPlotBos = GeoPointInput("Boston", "Massachusetts", Some(BigDecimal("1516798245505")))
      val geoPlotSeattle = GeoPointInput("Seattle", "Washington", Some(BigDecimal("1516798245504")))
      val postData = Map("geoInputPoints" -> List(geoPlotBos, geoPlotSeattle))
      val geoPlotResult = GeoPlot(1,"1234-123421312-421332", Vector())
      val result = Post("/plot", postData) ~> routes(plotHandler)
      plotHandlerProbe.expectMsg(CreateGeoPlot(GeoPointInputs(Vector(geoPlotBos, geoPlotSeattle))))
      plotHandlerProbe.reply(geoPlotResult)

      check {
        status shouldEqual StatusCodes.OK
        responseAs[GeoPlot] shouldEqual geoPlotResult
        header[`Set-Cookie`] shouldEqual Some(`Set-Cookie`(HttpCookie("weathertracker", value = "1234-123421312-421332")))
      }(result)
    }
  }
}
