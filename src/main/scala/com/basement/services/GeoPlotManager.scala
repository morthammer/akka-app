package com.basement.services

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.pipe
import akka.util.Timeout
import com.basement.bootstrap.ActorRegistry
import com.basement.domain.{FindByToken, _}
import slick.jdbc.H2Profile.api._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Failure

class GeoPlotManager(geoPlotLookup: ActorRef, geoPointManager: ActorRef, geoPointLookup: ActorRef) extends Actor with ActorLogging with TokenSupport {
  val dao = new GeoPlotManagerDAO
  implicit val ec: ExecutionContext = context.system.dispatcher
  implicit val timeout: Timeout = 1 minute


  def receive = {
    case CreatePlotId(token) => pipe(createPlot(token)) to sender()
  }

  def createPlot(token: String): Future[Option[Long]] = {
    import akka.pattern.ask
     val insertRes = for {
         _ <- dao.insert(token)
        geoPlotOpt <-(geoPlotLookup ? FindByToken(token)).mapTo[Option[GeoPlot]]
    } yield geoPlotOpt.map(_.id)

    insertRes andThen {
      case Failure(e) =>
        log.error("INSERT FAILED: [" + e.getMessage + "]" + ".  FOR PLOT TOKEN [" + token + "]")
    }
  }
}

object GeoPlotManager{
  def apply(actorSystem: ActorSystem, geoPlotLookup: ActorRef, geoPointManager: ActorRef, geoPointLookup: ActorRef) =
    actorSystem.actorOf(Props(classOf[GeoPlotManager], geoPlotLookup, geoPointManager, geoPointLookup), ActorRegistry.plotManager)
}

object GeoPlotManagerDAO{
  val insertStmt = (token: String) => sqlu"""insert into plot (token) values(${token})"""
}

class GeoPlotManagerDAO extends DatabaseAccess {
import GeoPlotManagerDAO._
  def insert(token: String)(implicit ec: ExecutionContext) = db.run(insertStmt(token))
}
