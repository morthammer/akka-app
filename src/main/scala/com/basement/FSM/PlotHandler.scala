package com.basement.FSM

import akka.actor.{Actor, ActorRef, ActorSystem, FSM, Props}
import com.basement.FSM.PlotWorker.FSMModel
import com.basement.FSM.PlotWorker.FSMModel._
import com.basement.domain._
import com.basement.services.TokenSupport
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime

import scala.util.Failure

object PlotHandler {

  def apply(system: ActorSystem, dependencies: FSMModel.Dependencies) = {
    system.actorOf(Props(classOf[PlotHandler], system, dependencies))
  }
}

class PlotHandler(system: ActorSystem, dependencies: Dependencies) extends Actor{
  def receive = {
    case msg@IdentifyGeoPlot(_) => PlotWorker(system, dependencies).forward(msg)
    case msg@CreateGeoPlot(_) => PlotWorker(system, dependencies).forward(msg)
  }
}

object PlotWorker {
  object FSMModel {
    sealed trait State
    case object Idle extends State
    case object FetchingGeoPlot extends State
    case object FetchingWeather extends State
    case object CreatingGeoPlot extends State
    case object CreatingGeoPoints extends State
    sealed trait Data
    abstract class DataWithRespondActor extends Data{
      val respondToActor: ActorRef
    }
    case object Uninitialized extends Data
    case class RespondData(respondToActor: ActorRef) extends DataWithRespondActor
    case class GeoPlotInputsData(geoPlotInputs: GeoPointInputs, token: String, respondToActor: ActorRef) extends DataWithRespondActor
    case class GeoPlotInputsDataWithPlotId(geoPlotInputs: GeoPointInputs, token: String, respondToActor: ActorRef, plotId: Long) extends DataWithRespondActor
    case class GeoPlotData(geoPlot: GeoPlot, respondToActor: ActorRef) extends DataWithRespondActor
    case class Dependencies(openWeatherClient: ActorRef, geoPointLookup: ActorRef, geoPointManager: ActorRef, geoPlotManager: ActorRef, geoPlotLookup: ActorRef)
  }

  val openWeatherConfig =  ConfigFactory.load().getConfig("openWeather")
  val expirationDuration = openWeatherConfig.getDuration("expirationDuration")

  def apply(system: ActorSystem, dependencies: FSMModel.Dependencies) = {
    system.actorOf(Props(classOf[PlotWorker], dependencies))
  }
}

class PlotWorker(dependencies: Dependencies) extends FSM[PlotWorker.FSMModel.State, PlotWorker.FSMModel.Data] with TokenSupport {
  import PlotWorker._

  startWith(Idle, Uninitialized)

  when(Idle){
    case Event(IdentifyGeoPlot(Some(cookie)), Uninitialized) =>
      val sendingActorRef = sender()
      dependencies.geoPlotLookup ! FindByToken(cookie.value)
      goto(FetchingGeoPlot) using RespondData(sendingActorRef)
    case Event(CreateGeoPlot(geoPlotInputs: GeoPointInputs), Uninitialized) =>
      val sendingActorRef = sender()
      val token = createToken
      dependencies.geoPlotManager ! CreatePlotId(token)
      goto(CreatingGeoPlot) using GeoPlotInputsData(geoPlotInputs, token, sendingActorRef)
  }

  when(FetchingGeoPlot){
    case Event(Some(geoPlot:GeoPlot), data: RespondData) =>
      val expiredPoints = geoPlot.points.filter(_.timestamp < new DateTime().minusSeconds(expirationDuration.getSeconds.toInt).getMillis)
      if (expiredPoints.nonEmpty) {
        dependencies.openWeatherClient ! FetchWeatherForGeoPoint(expiredPoints)
        goto(FetchingWeather) using GeoPlotData(geoPlot, data.respondToActor)
      } else {
        //Nothing expired respond immediately
        data.respondToActor ! geoPlot
        stop
      }
  }

  when(CreatingGeoPlot){
    case Event(Some(geoPlotId: Long), GeoPlotInputsData(geoPlotInputs,token,respondToActor)) =>
      dependencies.geoPointManager ! Create(GeoPointInputsWithPlotId(geoPlotInputs.geoInputPoints, geoPlotId))
      goto(CreatingGeoPoints) using GeoPlotInputsDataWithPlotId(geoPlotInputs, token, respondToActor, geoPlotId)
  }

  when(CreatingGeoPoints){
    case Event(geoPoints: Vector[GeoPoint], data: GeoPlotInputsDataWithPlotId) =>
      dependencies.openWeatherClient ! FetchWeatherForGeoPoint(geoPoints)
    goto(FetchingWeather) using GeoPlotData(GeoPlot(data.plotId, data.token, geoPoints), data.respondToActor)
  }

  when(FetchingWeather){
    case Event(updatedGeoPoints:GeoPointsWithWeather, data: GeoPlotData) =>
      dependencies.geoPointManager ! Update(updatedGeoPoints)
      val geoPlotWithUpdatedPoints = data.geoPlot.copy(points = data.geoPlot.points.filterNot(p => updatedGeoPoints.geoPoints.map(_.id).contains(p.id)) ++ updatedGeoPoints.geoPoints)
      data.respondToActor ! geoPlotWithUpdatedPoints
      stop
  }

  whenUnhandled{
    case Event(e: Throwable, data:DataWithRespondActor) =>
      data.respondToActor ! Failure(e)
      stop
    case Event(e, s) =>
      log.error("received unhandled request {} in state {}/{}", e, stateName, s)
      stop
  }

  initialize()
}
