package com.basement.restapi

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import com.basement.bootstrap.PathLookup
import com.basement.domain._
import com.basement.services.TokenSupport
import akka.http.scaladsl.model.StatusCodes._
import com.basement.FSM.PlotWorker.RequestFormatException
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}

trait GeoPlotRoutes extends JsonSupport with PathLookup with TokenSupport {

  implicit val system: ActorSystem
  val log = LoggerFactory.getLogger(this.getClass)

  def routes(plotHandler: ActorRef) =
    path("plot") {
      (get & cookie("weathertracker")) { cookie =>
        onComplete(plotHandler ? IdentifyGeoPlot(cookie)){
          case Success(plot: GeoPlot) => complete(plot)
          case Success(Failure(RequestFormatException(msg))) => complete(HttpResponse(BadRequest, entity = msg))
          case _ => complete(HttpResponse(InternalServerError))
        }
      } ~
      post {
        entity(as[GeoPointInputs]) {
          plotIn => {
            onComplete((plotHandler ? CreateGeoPlot(plotIn))){
              case Success(plot: GeoPlot) => setCookie(HttpCookie("weathertracker", plot.token)) (complete(plot))
              case Success(Failure(RequestFormatException(msg))) => complete(HttpResponse(BadRequest, entity = msg))
              case _ => complete(HttpResponse(InternalServerError))
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
