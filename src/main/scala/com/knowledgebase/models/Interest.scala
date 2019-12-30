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

  def resourceToJson(resource: Resource): Json = resource match {
    case StockResource(currentValue, time) => Json.obj(
      ("currentValue", Json.fromDouble(currentValue).getOrElse(throw new Exception("can't parse currentValue"))),
      ("time", Json fromString time.toString)
    )
    case PollResource(cycle, state, pollster, fteGrade, sampleSize, officeType, startDate, endDate, stage, entries) => Json.obj(
      ("cycle", Json fromInt cycle),
      ("state", Json fromString state.getOrElse("Federal")),
      ("pollster", Json fromString pollster),
      ("fteGrade", Json fromString fteGrade),
      ("sampleSize", Json fromInt sampleSize),
      ("officeType", Json fromString officeType),
      ("startDate", Json fromString startDate.toString),
      ("endDate", Json fromString endDate.toString),
      ("stage", Json fromString stage),
      ("entries", Json fromValues entries.map(entry => Json.obj(
        ("party", Json fromString entry.party),
        ("candidate", Json fromString entry.candidate),
        ("percentage", Json.fromDouble(entry.percentage).getOrElse(throw new Exception("can't parse percentage")))
      )))
    )
    case InfoResource(info) => Json.obj(
      ("info", Json fromString info)
    )
  }
}
