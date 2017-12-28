package com.basement.bootstrap

object ActorRegistry{
  val pointManager = "geoPointManager"
  val pointLookup = "geoPointLookup"
  val weatherClient = "weatherApiClient"
  val weatherManager = "weatherManager"
  val plotLookup = "geoPlotLookup"
  val plotManager = "geoPlotManager"
}


trait PathLookup{
  val actorPath: Map[String, String] =
    Map(
      "geoPointLookup" -> "/user/pointLookup",
      "geoPointManager" -> "/user/pointManager",
      "geoPlotLookup" -> "/user/plotLookup",
      "geoPlotManager" -> "/user/plotManager",
      "weatherManager" -> "/user/weatherManager",
      "weatherApiClient" -> "/user/weatherApiClient"
    )
}
