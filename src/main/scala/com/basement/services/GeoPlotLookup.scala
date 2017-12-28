package com.basement.services

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.pipe
import akka.util.Timeout
import com.basement.bootstrap.ActorRegistry
import com.basement.domain._
import org.slf4j.LoggerFactory
import slick.jdbc.H2Profile.api._
import slick.jdbc.GetResult

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

object GeoPlotLookup{
  def apply(actorSystem: ActorSystem, pointLookup: ActorRef) = actorSystem.actorOf(Props(classOf[GeoPlotLookup], pointLookup), ActorRegistry.plotLookup)
}

class GeoPlotLookup(pointLookup: ActorRef) extends Actor with ActorLogging  {
  import akka.pattern.ask
  import scala.concurrent.duration._

  implicit val ec: ExecutionContext = context.system.dispatcher
  implicit val timeout: Timeout = 1 minute

  val dao = new GeoPlotLookupDAO

  def receive = {
    case FindByToken(token) =>
      pipe(selectByToken(token)) to sender()
  }

  def selectByToken(token: String): Future[Option[GeoPlot]] = {
    val geoPlotOpt: Future[Option[GeoPlot]] = dao.selectByToken(token).map(_.headOption).andThen {
      case Failure(ex) => log.error("Exception fetching plot by cookie [" + token + "] is" + ex.getMessage)
    }

    val geoPoints: Future[Vector[GeoPoint]] = geoPlotOpt.flatMap{p => p.fold(Future.successful(Vector[GeoPoint]()))(geoPlot => fetchGeoPoints(geoPlot.id))}
     geoPoints.flatMap(geoPoints => geoPlotOpt.map(_.map(geoPlot => geoPlot.copy(points = geoPoints))))
  }

  def fetchGeoPoints(plotId: Long): Future[Vector[GeoPoint]] = {
    (pointLookup ? FindByPlotId(plotId)).mapTo[Vector[GeoPoint]]
  }
}

object GeoPlotLookupDAO {
  implicit val getPlotResult = GetResult(r => GeoPlot(r.<<, r.<<, Vector()))
}

class GeoPlotLookupDAO extends DatabaseAccess{
  import GeoPlotLookupDAO._
  def log = LoggerFactory.getLogger(this.getClass)

  def selectByToken(token: String)(implicit ec: ExecutionContext):Future[Vector[GeoPlot]] = {
    val stmt = sql"""
       select pl.id, pl.token
       from plot pl
       where pl.token = ${token}""".as[GeoPlot]
    db.run(stmt)
  }
}

