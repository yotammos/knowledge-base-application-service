package com.knowledgebase.clients

import java.sql.Timestamp
import java.time.{LocalDate, LocalDateTime}

import com.knowledgebase.models.{InfoResource, Interest, PollEntry, PollResource, StockResource}
import com.knowledgebase.thrift.KnowledgeBaseService.{AddInterests, GetInterests}
import com.knowledgebase.thrift.{AddInterestsRequest, GetInterestsRequest, GetInterestsResponse, InterestType, KnowledgeBaseService, Resource, SimpleResponse, UserId, InfoResource => ThriftInfoResource, Interest => ThriftInterest, PollEntry => ThriftPollEntry, PollResource => ThriftPollResource, StockResource => ThriftStockResource}
import com.twitter.finagle.Thrift
import com.twitter.util.Future

trait KnowledgeBaseThriftClientComponent {

  def knowledgeBaseThriftClient: KnowledgeBaseThriftClient

  class KnowledgeBaseThriftClient(host: String, port: Int) {
    private val client = Thrift.client.servicePerEndpoint[KnowledgeBaseService.ServicePerEndpoint](
      "localhost:8080",
      "another_thrift_client"
    )

    def getInterests(userId: UserId): Future[Seq[Interest]] =
      client.getInterests(GetInterests Args GetInterestsRequest(userId))
        .map {
          case GetInterestsResponse(true, None, Some(interests)) =>
            println(s"received interests from domain service, interests = $interests")
            interests.map(interest => Interest(
              name = interest.name,
              interestType = interest.interestType.originalName,
              resources = interest.resources.map {
                case Resource.StockResource(ThriftStockResource(currentValue, timestamp)) => StockResource(currentValue, LocalDateTime.parse(timestamp))
                case Resource.PollResource(ThriftPollResource(cycle, pollster, fteGrade, sampleSize, officeType, startDate, endDate, stage, entries, state)) =>
                  PollResource(
                    cycle,
                    state,
                    pollster,
                    fteGrade,
                    sampleSize,
                    officeType,
                    LocalDate parse startDate,
                    LocalDate parse endDate,
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
        userId, interests.map(interest =>
          ThriftInterest(
            name = interest.name,
            interestType = interest.interestType match {
              case "STOCK" => InterestType.Stock
              case "POLL" => InterestType.Poll
              case "INFO" | _ => InterestType.Info
            },
            resources = interest.resources map {
              case StockResource(currentValue, time) =>
                Resource StockResource ThriftStockResource(currentValue, Timestamp.valueOf(time).getTime.toString)
              case PollResource(cycle, state, pollster, fteGrade, sampleSize, officeType, startDate, endDate, stage, entries) =>
                Resource PollResource ThriftPollResource(
                  cycle,
                  pollster,
                  fteGrade,
                  sampleSize,
                  officeType,
                  startDate.toString,
                  endDate.toString,
                  stage,
                  entries.map(entry => ThriftPollEntry(entry.party, entry.candidate, entry.percentage)),
                  state
                )
              case InfoResource(info) =>
                Resource InfoResource ThriftInfoResource(info)
            }
          )
        )
      ))
        .map {
          case SimpleResponse(false, Some(errorMessage)) => throw new Exception("failed adding interests, error = " + errorMessage)
          case _ => println("success adding interests")
        }
  }
}
