package com.knowledgebase.models

import java.time.{LocalDate, LocalDateTime}

import io.circe.{Encoder, Json}

abstract class Resource

case class StockResource(currentValue: Double, time: LocalDateTime) extends Resource
case class InfoResource(info: String) extends Resource
case class PollResource(
                         cycle: Int,
                         state: Option[String] = None,
                         pollster: String,
                         fteGrade: String,
                         sampleSize: Int,
                         officeType: String,
                         startDate: LocalDate,
                         endDate: LocalDate,
                         stage: String,
                         entries: Seq[PollEntry]
                       ) extends Resource

case class PollEntry(party: String, candidate: String, percentage: Double)

object Resource {
  lazy implicit val interestEncoder: Encoder[Interest] = interest => Json.obj(
    ("name", Json fromString interest.name),
    ("interestType", Json fromString interest.interestType),
    ("resources", Json fromValues interest.resources.map {
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
    })
  )

}