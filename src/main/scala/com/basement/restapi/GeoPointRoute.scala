package com.basement.restapi

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import com.basement.bootstrap.{ActorRegistry, PathLookup}
import com.basement.domain._
import com.basement.services.{GeoPlotLookup, GeoPlotManager}

import scala.concurrent.duration._
trait GeoPointRoute extends JsonSupport with PathLookup {
  implicit val system: ActorSystem

  def routes(geoPointLookup: ActorRef, geoPointManager: ActorRef) =
    path("points") {
      get {
        complete((geoPointLookup ? FindAll).mapTo[GeoPoints])
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

