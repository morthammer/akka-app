package com.basement.services

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.basement.domain._
import com.basement.bootstrap.ActorRegistry
import slick.jdbc.H2Profile.api._
import akka.pattern.pipe
import akka.util.Timeout
import org.joda.time.DateTime

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class GeoPointManager(weatherManager: ActorRef, geoPointLookup: ActorRef) extends Actor with ActorLogging {
  val dao = new GeoPointManagerDAO
  implicit val ec: ExecutionContext = context.system.dispatcher
  implicit val timeout: Timeout = 1 minute

  def receive = {
    case Create(pointInputsWithPlotId: GeoPointInputsWithPlotId) => pipe(createPointsWithPlotId(pointInputsWithPlotId)) to sender()
    case Update(point: GeoPoint) => pipe(updatePoint(point)) to sender()
    case Update(geoPointsWithWeather: GeoPointsWithWeather) => pipe(updatePoints(geoPointsWithWeather.geoPoints)) to sender()
    case Delete(id: Long) => pipe(deletePoint(id)) to sender()
  }

  def createPointsWithPlotId(geoPointInputsWithPlotId: GeoPointInputsWithPlotId): Future[GeoPoints] = {
    import akka.pattern.ask

    val geoPointInputs = geoPointInputsWithPlotId.geoPointInputs
    val plotId = geoPointInputsWithPlotId.plotId
    for {
      _ <- Future.traverse(geoPointInputs) (createPoint(plotId))
      geoPoints <- (geoPointLookup ? FindByPlotId(geoPointInputsWithPlotId.plotId)).mapTo[GeoPoints]
    } yield geoPoints
  }

  def createPoint(plotId: Long)(geoPointInput: GeoPointInput) = {
    dao.insert(geoPointInput, plotId).map( _ => OperationResult(true)) andThen {
      case Failure(e) =>
        log.error("INSERT FAILED: [" + e.getMessage + "]" + ".  FOR POINT INPUT [" + geoPointInput + "]")
    }
  }

  def updatePoints(points: Vector[GeoPoint]): Future[Vector[OperationResult]] = {
    Future.traverse(points)(gp => updatePoint(gp))
  }

  def updatePoint(point: GeoPoint): Future[OperationResult] = {
    dao.update(point).map(_ => OperationResult(true)) andThen {
      case Success(OperationResult(true)) =>
        point.weather.foreach(weatherManager ! Upsert(_))
      case Failure(e) =>
        log.error("UPDATE FAILED: [" + e.getMessage + "]" + ".  FOR POINT [" + point + "]")
    }
  }

  def deletePoint(id: Long): Future[OperationResult] = {
    dao.delete(id).map(_ => OperationResult(true)) andThen {
      case Failure(e) =>
        log.error("DELETE FAILED: [" + e.getMessage + "]" + ".  FOR POINT ID [" + id + "]")
    }
  }
}

object GeoPointManager{
  def apply(actorSystem: ActorSystem, weatherManager: ActorRef, geoPointLookup: ActorRef) = actorSystem.actorOf(Props(classOf[GeoPointManager], weatherManager, geoPointLookup), ActorRegistry.pointManager)
}

object GeoPointManagerDAO{
  val insertStmt = (pointInput: GeoPointInput, plotId: Long) => sqlu"""insert into point (plotId, city, state, timestamp) values(${plotId}, ${pointInput.city}, ${pointInput.state}, ${new DateTime().getMillis})"""

  val updateStmt = (point: GeoPoint) => sqlu"""update point set plotId = ${point.plotId}, city = ${point.city}, state = ${point.state}, timestamp = ${point.timestamp}, weatherid = ${point.weather.flatMap(_.id)} WHERE ID = ${point.id} """

  val deleteStmt = (id: Long) => sqlu"""delete from point where id = $id """
}

class GeoPointManagerDAO extends DatabaseAccess {
import GeoPointManagerDAO._

  def insert(pointInput: GeoPointInput, plotId: Long)(implicit ec: ExecutionContext) = db.run(insertStmt(pointInput, plotId))

  def update(point: GeoPoint)(implicit ec: ExecutionContext) = db.run(updateStmt(point))

  def delete(id: Long)(implicit ec: ExecutionContext) = db.run(deleteStmt(id))
}
