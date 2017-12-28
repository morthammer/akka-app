package com.basement.restapi

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import com.basement.bootstrap.{ActorRegistry, PathLookup}
import com.basement.domain._
import com.basement.services.{GeoPlotLookup, GeoPlotManager}

import scala.concurrent.duration._
//TODO doesn't need path lookup trait mixed in anymore
trait GeoPointRoute extends JsonSupport with PathLookup {
  implicit val system: ActorSystem
  implicit val timeout = Timeout(5 seconds)

  def routes(geoPointLookup: ActorRef, geoPointManager: ActorRef) =
    path("points") {
      get {
        complete((geoPointLookup ? FindAll).mapTo[Vector[GeoPoint]])
        }
    } ~
    path("point"){
      post {
        entity(as[GeoPointInput]) {
          pInput => {
            complete((geoPointManager ? Create[GeoPointInput](pInput)).mapTo[OperationResult])}
        }
      }
    } ~
    pathPrefix("point" / LongNumber){
      id =>
      get{
        complete((geoPointLookup ? FindById(id)).mapTo[Option[GeoPoint]])
      } ~
      put{
        entity(as[GeoPoint]) {
          point => complete((geoPointManager ? Update[GeoPoint](point)).mapTo[OperationResult])
        }
      } ~
      delete{
        complete((geoPointManager ? Delete(id)).mapTo[OperationResult])
      }
    }
}

case class GeoPointRouteImp(system: ActorSystem) extends GeoPointRoute

object GeoPointRoutes{
  def apply(system: ActorSystem) = {
    new GeoPointRouteImp(system)
  }
}

