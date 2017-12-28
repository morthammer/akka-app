package com.basement.services

import slick.jdbc.H2Profile.api._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

trait DatabaseAccess {
//TODO seperate db variable from instantiating with new trait so testing classes don't create database

  val db = Database.forConfig("h2mem1")

  def initDatabase = {
    logDBResult(db.run(
      sqlu"""CREATE TABLE POINT
        (id INTEGER NOT NULL PRIMARY KEY auto_increment,
        plotId INTEGER NOT NULL,
        city VARCHAR(255) NOT NULL,
        state VARCHAR(255) NOT NULL,
        timestamp BIGINT NOT NULL,
        weatherId INTEGER);""")
    )

    logDBResult(db.run(
      sqlu"""CREATE TABLE WEATHER
        (id INTEGER NOT NULL PRIMARY KEY,
        currentTemp DECIMAL,
        tempMin Decimal,
        tempMax Decimal,
        pressure DECIMAL,
        timestamp BIGINT,
        humidity DECIMAL,
        description VARCHAR(255),
        windSpeed DECIMAL);""")
    )

    logDBResult(db.run(
      sqlu"""CREATE TABLE PLOT
        (id INTEGER NOT NULL PRIMARY KEY auto_increment,
         token VARCHAR(255) NOT NULL);""")
    )
  }

  //initDatabase

  def logDBResult[T](fut:Future[T]) = {
    println("!!!!!!!!!!!! [" + Await.result(fut, 5 seconds) +"]")
  }

}
