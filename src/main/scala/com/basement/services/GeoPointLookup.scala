package com.basement.services


import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.pipe
import com.basement.bootstrap.ActorRegistry
import com.basement.domain._
import org.slf4j.LoggerFactory
import slick.jdbc.H2Profile.api._
import slick.jdbc.{GetResult, PositionedResult}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

object GeoPointLookup{
  def apply(actorSystem: ActorSystem) = actorSystem.actorOf(Props[GeoPointLookup], ActorRegistry.pointLookup)
}

class GeoPointLookup extends Actor with ActorLogging  {

  implicit val ec: ExecutionContext = context.system.dispatcher
  val dao = new GeoPointLookupDAO

  def receive = {
    case FindById(id) => pipe(selectById(id)) to sender()
    case FindByPlotId(plotId) => pipe(selectByPlotId(plotId)) to sender()
    case FindAll => pipe(selectAll) to sender()
  }

  def selectById(id: Long) = {
    dao.selectPoints(id).map(_.headOption).andThen {
      case Failure(ex) => log.error("Exception fetching point id [" + id + "] is" + ex.getMessage)
    }
  }

  def selectByPlotId(plotId: Long) = {
    dao.selectPointsByPlotId(plotId).andThen {
      case Failure(ex) => log.error("Exception fetching points with plot id [" + plotId + "] is" + ex.getMessage)
    }
  }

  def selectAll = {
    dao.selectAllPoints.andThen {
      case Failure(ex) => log.error("Exception fetching all points is" + ex.getMessage)
    }
  }
}

object GeoPointLookupDAO {
  implicit val getPointResult:GetResult[GeoPoint] = GetResult[GeoPoint](r => GeoPoint(r.<<, r.<<, r.<<, r.<<, r.<<, getWeather(r)))
  implicit val getWeatherResult:GetResult[Weather] = GetResult(r => Weather(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))

  def getWeather(pr: PositionedResult):Option[Weather] = {
    val weather = Weather(pr.nextLongOption(), pr.nextBigDecimalOption(), pr.nextBigDecimalOption(), pr.nextBigDecimalOption(), pr.nextLong(), pr.nextBigDecimalOption(), pr.nextBigDecimalOption(), pr.nextStringOption, pr.nextBigDecimalOption())
    weather.currentTemp.map(_ => weather)
  }
}

class GeoPointLookupDAO extends DatabaseAccess{
  //TODO refactor so the implicits don't need to be explicity passed
  import GeoPlotLookupDAO._
  def log = LoggerFactory.getLogger(this.getClass)

  def selectPoints(id: Long)(implicit ec: ExecutionContext):Future[Seq[GeoPoint]] = {
    val stmt = sql"""
       select p.id, p.plotId, p.city, p.state, p.timstamp, p.weatherId, w.currentTemp, w.tempMin, w.tempMax, w.timestamp, w.pressure, w.humidity, w.description, w.windSpeed
       from point p
       left join weather w on w.id = p.weatherId
       where p.id = ${id}""".as[GeoPoint](rconv = GeoPointLookupDAO.getPointResult)
    db.run(stmt)
  }

  def selectAllPoints(implicit ec: ExecutionContext):Future[Seq[GeoPoint]] = {
    val stmt = sql"""
         select p.id, p.plotId, p.city, p.state, p.timestamp, p.weatherId, w.currentTemp, w.tempMin, w.tempMax, w.pressure, w.timestamp, w.humidity, w.description, w.windSpeed
         from point p
         left join weather w on w.id = p.weatherId
        """.as[GeoPoint](rconv = GeoPointLookupDAO.getPointResult)
    db.run(stmt)
  }

  def selectPointsByPlotId(plotId: Long)(implicit ec: ExecutionContext): Future[Seq[GeoPoint]] = {
    val stmt =
    sql"""select p.id, p.plotId, p.city, p.state, p.timestamp, p.weatherId, w.currentTemp, w.tempMin, w.tempMax, w.pressure, w.timestamp, w.humidity, w.description, w.windSpeed
         from point p
         left join weather w on w.id = p.weatherId
         where p.plotId = ${plotId}
       """.as[GeoPoint](rconv = GeoPointLookupDAO.getPointResult)
    db.run(stmt)
  }
}

