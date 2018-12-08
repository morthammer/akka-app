package com.basement.bootstrap

import akka.util.Timeout

object ActorRegistry{
  val pointManager = "geoPointManager"
  val pointLookup = "geoPointLookup"
  val weatherClient = "weatherApiClient"
  val weatherManager = "weatherManager"
  val plotLookup = "geoPlotLookup"
  val plotManager = "geoPlotManager"
}


trait PathLookup{
  import scala.concurrent.duration._
  implicit val timeout = Timeout(5 seconds)

  val actorPath: Map[String, String] =
    Map(
      "geoPointLookup" -> s"/user/${ActorRegistry.pointLookup}",
      "geoPointManager" -> s"/user/${ActorRegistry.pointManager}",
      "geoPlotLookup" -> s"/user/${ActorRegistry.plotLookup}",
      "geoPlotManager" -> s"/user/${ActorRegistry.plotManager}",
      "weatherManager" -> s"/user/${ActorRegistry.weatherManager}",
      "weatherApiClient" -> s"/user/${ActorRegistry.weatherClient}"
    )
}
