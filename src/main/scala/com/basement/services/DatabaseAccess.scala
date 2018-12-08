package com.basement.services

import org.slf4j.LoggerFactory
import slick.jdbc.H2Profile.api._

trait DatabaseAccess {
    val db = H2DataStore.db
}

object H2DataStore{
  val log = LoggerFactory.getLogger(this.getClass)


  val db = Database.forConfig("h2mem1")

  def initDatabase = {
    db.run(
      sqlu"""CREATE TABLE POINT
        (id INTEGER NOT NULL PRIMARY KEY auto_increment,
        plotId INTEGER NOT NULL,
        city VARCHAR(255) NOT NULL,
        state VARCHAR(255) NOT NULL,
        timestamp BIGINT NOT NULL,
        weatherId INTEGER);""")

    db.run(
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

    db.run(
      sqlu"""CREATE TABLE PLOT
        (id INTEGER NOT NULL PRIMARY KEY auto_increment,
         token VARCHAR(255) NOT NULL);""")
  }

  log.info("Initialized H2 database")
  initDatabase
}
