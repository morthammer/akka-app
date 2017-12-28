package com.basement.services

import java.util.UUID

trait TokenSupport {

  def createToken = UUID.randomUUID().toString

}
