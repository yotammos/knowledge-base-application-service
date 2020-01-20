package com.knowledgebase.clients

import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}

import com.knowledgebase.models.{InfoResource, Interest, InterestInfo, PollEntry, PollResource, StockResource}
import com.knowledgebase.thrift.KnowledgeBaseService.{AddInterests, GetInterestInfo, GetInterests, RemoveInterests}
import com.knowledgebase.thrift.{AddInterestsRequest, GetInterestInfoResponse, GetInterestsResponse, InterestType, KnowledgeBaseService, RemoveInterestsRequest, Resource, SimpleRequest, SimpleResponse, UserId, InfoResource => ThriftInfoResource, Interest => ThriftInterest, PollEntry => ThriftPollEntry, PollResource => ThriftPollResource, StockResource => ThriftStockResource}
import com.twitter.finagle.Thrift
import com.twitter.util.Future

trait KnowledgeBaseThriftClientComponent {

  def knowledgeBaseThriftClient: KnowledgeBaseThriftClient

  class KnowledgeBaseThriftClient(host: String, port: Int) {
    private val client = Thrift.client.servicePerEndpoint[KnowledgeBaseService.ServicePerEndpoint](
      "localhost:8080",
      "another_thrift_client"
    )

    def getInterestInfo(userId: UserId): Future[Seq[InterestInfo]] =
      client.getInterestInfo(GetInterestInfo Args SimpleRequest(userId))
      .map {
        case GetInterestInfoResponse(true, None, Some(info)) =>
          println(s"received interest info from domain service, info = $info")
          info.map(interestInfo => InterestInfo(interestInfo.name, interestInfo.interestType.originalName))
        case GetInterestInfoResponse(false, Some(errorMessage), None) =>
          throw new Exception("Failed getting interest info for user, error = " + errorMessage)
        case _ =>
          throw new Exception("Failed getting interest info for user")
      } handle {
        case t: Throwable =>
          println("failed getting interest info, error = " + t.getMessage)
          throw t
      }

    def getInterests(userId: UserId): Future[Seq[Interest]] =
      client.getInterests(GetInterests Args SimpleRequest(userId))
        .map {
          case GetInterestsResponse(true, None, Some(interests)) =>
            println(s"received interests from domain service, interests = $interests")
            interests.map(interest => Interest(
              name = interest.name,
              interestType = interest.interestType.originalName,
              resources = interest.resources.map {
                case Resource.StockResource(ThriftStockResource(currentValue, timestamp)) => StockResource(currentValue, LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp.toLong), ZoneId.systemDefault()))
                case Resource.PollResource(ThriftPollResource(cycle, pollster, fteGrade, sampleSize, officeType, startDate, endDate, stage, entries, state)) =>
                  PollResource(
                    cycle,
                    state,
                    pollster,
                    fteGrade,
                    sampleSize,
                    officeType,
                    LocalDate.parse(startDate, DateTimeFormatter.ofPattern("M/d/yy")),
                    LocalDate.parse(endDate, DateTimeFormatter.ofPattern("M/d/yy")),
                    stage,
                    entries.map(entry => PollEntry(entry.party, entry.candidate, entry.percentage))
                  )
                case Resource.InfoResource(ThriftInfoResource(info)) => InfoResource(info)
              }
            ))
          case GetInterestsResponse(false, Some(errorMessage), None) =>
            throw new Exception("Failed getting interests for user, error = " + errorMessage)
          case _ =>
            throw new Exception("Failed getting interests for user")
        } handle {
        case t: Throwable =>
          println("failed getting interests, error = " + t.getMessage)
          throw t
      }

    def addInterests(userId: UserId, interests: Seq[Interest]): Future[Unit] =
      client.addInterests(AddInterests Args AddInterestsRequest(
        userId, interests.map { interest =>
          println("adding interest: " + interest)
          ThriftInterest(
            name = interest.name,
            interestType = interest.interestType match {
              case "STOCK" => InterestType.Stock
              case "POLL" => InterestType.Poll
              case "INFO" | _ => InterestType.Info
            },
            resources = Seq.empty[Resource]
          )
        }
      ))
        .map {
          case SimpleResponse(false, Some(errorMessage)) => throw new Exception("failed adding interests, error = " + errorMessage)
          case _ => println("success adding interests")
        }

    def removeInterests(userId: UserId, interestNames: Seq[String]): Future[Unit] =
      client.removeInterests(RemoveInterests Args RemoveInterestsRequest(userId, interestNames))
        .map {
          case SimpleResponse(false, Some(errorMessage)) => throw new Exception("failed removing interests, error = " + errorMessage)
          case _ => println("success removing interests")
        }
  }
}
