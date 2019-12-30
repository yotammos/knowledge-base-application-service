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
    ("resources", Json fromValues interest.resources.map(Interest.resourceToJson))
  )
}