package com.knowledgebase.models

import io.circe.{Decoder, Encoder, Json}

case class Interest(name: String, interestType: String, resources: Seq[Resource])

object Interest {
  lazy implicit val interestDecoder: Decoder[Interest] = c =>
    for {
      name <- c.get[String]("name")
      interestType <- c.get[String]("interestType")
    } yield Interest(name, interestType, Seq.empty[Resource])

  lazy implicit val interestEncoder: Encoder[Interest] = interest => Json.obj(
    ("name", Json fromString interest.name),
    ("interestType", Json fromString interest.interestType),
    ("resources", Json.fromValues(interest.resources map resourceToJson))
  )

  private def resourceToJson(resource: Resource): Json = resource match {
    case InfoResource(info) => Json.obj(
      ("info", Json fromString info)
    )
    case StockResource(currentValue, time) => Json.obj(
      ("currentValue", Json fromString currentValue.toString),
      ("time", Json fromString time.toString)
    )
  }
}
