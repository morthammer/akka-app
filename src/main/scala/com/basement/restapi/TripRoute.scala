package com.basement.restapi

import akka.http.scaladsl.server.Directives._
import com.basement.domain.Trip

trait TripRoute {

  val routes = {
    pathPrefix("trip"){
      post{
        entity(as[Trip]){
          trip => complete("YIPPEEEEE")
        }
      }
      get{
        complete("Yipeeee!!!!")
      }
    }

  }
}
