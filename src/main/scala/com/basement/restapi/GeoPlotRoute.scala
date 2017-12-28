package com.basement.restapi

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import com.basement.bootstrap.PathLookup
import com.basement.domain._
import com.basement.services.TokenSupport
import akka.http.scaladsl.model.StatusCodes._

import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait GeoPlotRoutes extends JsonSupport with PathLookup with TokenSupport {

  implicit val system: ActorSystem
  implicit val timeout = Timeout(5 seconds)

  //TODO geoPlotLOokup isn't used
  def routes(plotHandler: ActorRef) =
    path("plot") {
      (get & optionalCookie("weathertracker")) { (opCookie) =>
        complete(((plotHandler ? IdentifyGeoPlot(opCookie)).mapTo[GeoPlot]))
      } ~
      post {
        entity(as[GeoPointInputs]) {
          plotIn => {
            //TODO add in circuit breaker to onCOmplete!!!!!
            onComplete((plotHandler ? CreateGeoPlot(plotIn)).mapTo[GeoPlot]){
              case Success(plot) => setCookie(HttpCookie("weathertracker", plot.token)) {complete(plot)}
              case Failure(ex) => complete(HttpResponse(InternalServerError, entity = ex.getMessage))
            }
          }
        }
      }
    }

}

case class GeoPlotRoutesImpl(system: ActorSystem) extends GeoPlotRoutes

object GeoPlotRoutes {
  def apply(system: ActorSystem) = {
    new GeoPlotRoutesImpl(system)
  }
}
