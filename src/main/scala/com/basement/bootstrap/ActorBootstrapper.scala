package com.basement.bootstrap

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RouteConcatenation._
import com.basement.FSM.PlotHandler
import com.basement.FSM.PlotWorker.FSMModel.Dependencies
import com.basement.restapi.{GeoPlotRoutes, GeoPointRoutes}
import com.basement.services._

class ActorBootstrapper {
  def boot: Unit = {
    implicit val system = ActorSystem.create("basement")
    implicit val mat = akka.stream.ActorMaterializer()
    implicit val executionContext = system.dispatcher

    //startActors(system)
    val weatherManager = WeatherManager(system)
    val openWeatherClient = OpenWeatherClient(system, weatherManager)
    val geoPointLookup = GeoPointLookup(system)
    val geoPointManager = GeoPointManager(system, weatherManager, geoPointLookup)
    val geoPlotLookup = GeoPlotLookup(system, geoPointLookup)
    val geoPlotManager = GeoPlotManager(system, geoPlotLookup, geoPointManager, geoPointLookup)
    val plotHandler = PlotHandler(system, Dependencies(openWeatherClient, geoPointLookup, geoPointManager, geoPlotManager, geoPlotLookup))

    val myRoutes =
      GeoPointRoutes(system).routes(geoPointLookup, geoPointManager) ~
      GeoPlotRoutes(system).routes(plotHandler)
    Http().bindAndHandle(handler = myRoutes, interface = "localhost", port =  8080)
  }
}

object ActorBootstrapper{
  def main(args: Array[String]){new ActorBootstrapper().boot}
}
