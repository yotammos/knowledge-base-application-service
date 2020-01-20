package com.knowledgebase.models

import io.circe.{Encoder, Json}

case class InterestInfo(name: String, interestType: String)

object InterestInfo {
  lazy implicit val interestInfoEncoder: Encoder[InterestInfo] = interestInfo => Json.obj(
    ("name", Json fromString interestInfo.name),
    ("interestType", Json fromString interestInfo.interestType)
  )
}