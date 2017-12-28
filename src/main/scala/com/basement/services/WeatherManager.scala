package com.basement.services

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.pipe
import com.basement.bootstrap.ActorRegistry
import com.basement.domain._
import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext

class WeatherManager extends Actor with ActorLogging {
  val dao = new WeatherManagerDAO
  implicit val ec: ExecutionContext = context.system.dispatcher

  def receive = {
    case Create(weather: Weather) => pipe(createWeather(weather)) to sender()
    case Update(weather: Weather) => pipe(updateWeather(weather)) to sender()
    case Upsert(weather: Weather) => pipe(upsertWeather(weather)) to sender()
      //TODO eliminated by erasure
    case Upsert(weather: Option[Weather]) => weather.map(w => upsertWeather(w))
    case Delete(id: Long) => pipe(deleteWeather(id)) to sender()
  }

  def upsertWeather(weather: Weather) = {
    dao.upsert(weather).map(_ => OperationResult(true)) recover {
      case e: Exception =>
        log.error("UPSERT FAILED: [" + e.getMessage + "]" + ".  FOR WEATHER [" + weather + "]")
        OperationResult(false)
    }
  }

  def createWeather(weather: Weather) = {
    dao.insert(weather).map(_ => OperationResult(true)) recover {
      case e: Exception =>
        log.error("INSERT FAILED: [" + e.getMessage + "]" + ".  FOR WEATHER [" + weather + "]")
        OperationResult(false)
    }
  }

  def updateWeather(weather: Weather) = {
    dao.update(weather).map(_ => OperationResult(true)) recover {
      case e: Exception =>
        log.error("UPDATE FAILED: [" + e.getMessage + "]" + ".  FOR WEATHER [" + weather + "]")
        OperationResult(false)
    }
  }

  def deleteWeather(id: Long) = {
    dao.delete(id).map(_ => OperationResult(true)) recover {
      case e: Exception =>
        log.error("DELETE FAILED: [" + e.getMessage + "]" + ".  FOR WEATHER ID [" + id + "]")
        OperationResult(false)
    }
  }
}

object WeatherManager{
  def apply(actorSystem: ActorSystem) = actorSystem.actorOf(Props[WeatherManager], ActorRegistry.weatherManager)
}

object WeatherManagerDAO{

  val upsertStmt = (weather: Weather) =>
    sqlu"""merge into WEATHER (id, currentTemp, tempMin, tempMax, pressure, timestamp, humidity, description, windSpeed)
          values(${weather.id},${weather.currentTemp}, ${weather.tempMin}, ${weather.tempMax}, ${weather.pressure}, ${weather.timestamp}, ${weather.humidity}, ${weather.description}, ${weather.windSpeed})"""

  val insertStmt = (weather: Weather) =>
    sqlu"""insert into WEATHER (id, currentTemp, tempMin, tempMax, pressure, timestamp, humidity, description, windSpeed)
          values(${weather.id},${weather.currentTemp}, ${weather.tempMin}, ${weather.tempMax}, ${weather.pressure}, ${weather.timestamp}, ${weather.humidity}, ${weather.description}, ${weather.windSpeed})"""

  val updateStmt = (weather: Weather) =>
    sqlu"""update weather set currentTemp = ${weather.currentTemp}, tempMin = ${weather.tempMin}, tempMax = ${weather.tempMax},
          pressure = ${weather.pressure}, timestamp = ${weather.timestamp}, humidity = ${weather.humidity}, description = ${weather.description}, windSpeed = ${weather.windSpeed} WHERE ID = ${weather.id} """

  val deleteStmt = (id: Long) => sqlu"""delete from weather where id = $id """
}

class WeatherManagerDAO extends DatabaseAccess {
import WeatherManagerDAO._

  def upsert(weather: Weather)(implicit ec: ExecutionContext) = db.run(upsertStmt(weather))

  def insert(weather: Weather)(implicit ec: ExecutionContext) = db.run(insertStmt(weather))

  def update(weather: Weather)(implicit ec: ExecutionContext) = db.run(updateStmt(weather))

  def delete(id: Long)(implicit ec: ExecutionContext) = db.run(deleteStmt(id))
}
