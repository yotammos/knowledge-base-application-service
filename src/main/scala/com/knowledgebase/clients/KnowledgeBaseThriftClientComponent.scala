package com.knowledgebase.clients

import java.sql.Timestamp

import com.knowledgebase.models.{InfoResource, Interest, StockResource}
import com.knowledgebase.thrift.KnowledgeBaseService.{AddInterests, GetInterests}
import com.knowledgebase.thrift.{AddInterestsRequest, GetInterestsRequest, GetInterestsResponse, InterestType, KnowledgeBaseService, Resource, SimpleResponse, UserId, InfoResource => ThriftInfoResource, Interest => ThriftInterest, StockResource => ThriftStockResource}
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
            println("success getting interests")
            interests.map(interest => Interest(
              name = interest.name,
              interestType = interest.interestType.originalName,
              resources = interest.resources.map {
                case Resource.StockResource(ThriftStockResource(currentValue, timestamp)) => StockResource(currentValue, Timestamp.valueOf(timestamp).toLocalDateTime)
                case Resource.InfoResource(ThriftInfoResource(info)) => InfoResource(info)
              }
            ))
          case GetInterestsResponse(false, Some(errorMessage), None) =>
            throw new Exception("Failed getting interests for user, error = " + errorMessage)
          case _ =>
            throw new Exception("Failed getting interests for user")
        }

    def addInterests(userId: UserId, interests: Seq[Interest]): Future[Unit] =
      client.addInterests(AddInterests Args AddInterestsRequest(
        userId, interests.map(interest =>
          ThriftInterest(
            name = interest.name,
            interestType = interest.interestType match {
              case "STOCK" => InterestType.Stock
              case "INFO" | _ => InterestType.Info
            },
            resources = interest.resources map {
              case StockResource(currentValue, time) => Resource StockResource ThriftStockResource(currentValue, Timestamp.valueOf(time).getTime.toString)
              case InfoResource(info) => Resource InfoResource ThriftInfoResource(info)
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
