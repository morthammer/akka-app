package com.basement.restapi

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}

class TripRouteSpec extends WordSpec with Matchers with ScalatestRouteTest with TripRoute{

  "The trip routes" should {
    "return success when trip data is posted" in {
      Get("/trip") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        //responseAs[String] shouldEqual "HTTP method not allowed, supported methods: GET"
      }
    }
    "return success when trip data is posted" in {
      Get("/trip") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        //responseAs[String] shouldEqual "HTTP method not allowed, supported methods: GET"
      }
    }
  }
}
