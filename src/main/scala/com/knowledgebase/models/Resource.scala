package com.knowledgebase.models

import java.time.LocalDateTime

import io.circe.{Encoder, Json}

abstract class Resource

case class StockResource(currentValue: Double, time: LocalDateTime) extends Resource
case class InfoResource(info: String) extends Resource

object Resource {
  lazy implicit val interestEncoder: Encoder[Interest] = interest => Json.obj(
    ("name", Json fromString interest.name),
    ("interestType", Json fromString interest.interestType),
    ("resources", Json fromValues interest.resources.map {
      case StockResource(currentValue, time) => Json.obj(
        ("currentValue", Json.fromDouble(currentValue).getOrElse(throw new Exception("can't parse currentValue"))),
        ("time", Json fromString time.toString)
      )
      case InfoResource(info) => Json.obj(
        ("info", Json fromString info)
      )
    })
  )

}