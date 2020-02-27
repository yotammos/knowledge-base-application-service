package com.knowledgebase

import com.knowledgebase.context.ComponentProvider
import com.knowledgebase.models.{Interest, InterestInfo, Message, Resource}
import com.knowledgebase.models.Interest._
import com.knowledgebase.models.InterestInfo._
import com.knowledgebase.thrift.UserId
import com.twitter.app.Flag
import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.http.filter.Cors
import com.twitter.server.TwitterServer
import com.twitter.util.{Await, Future}
import io.finch.{Endpoint, jsonBody, _}
import io.finch.circe._
import io.finch.syntax._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.auto._
import com.knowledgebase.services.ExampleService

object KnowledgeBaseApplicationService extends TwitterServer {

  private val context = new ComponentProvider
  private val port: Flag[Int] = flag("port", 8081, "TCP port for HTTP server")

  private val policy: Cors.Policy = Cors.Policy(
    allowsOrigin = _ => Some("*"),
    allowsMethods = _ => Some(Seq("GET", "POST")),
    allowsHeaders = _ => Some(Seq("Accept", "Content-Type"))
  )

  private final val getInterests: Endpoint[Seq[Interest]] = get("interests" :: param[String]("userId")) {
    userId: String => context.knowledgeBaseThriftClient.getInterests(UserId(userId.toLong)) map Ok
  }

  private final val getInterestInfo: Endpoint[Seq[InterestInfo]] = get("info" :: param[String]("userId")) {
    userId: String => context.knowledgeBaseThriftClient.getInterestInfo(UserId(userId.toLong)) map Ok
  }

  private final val addInterests: Endpoint[Unit] = post("interests" :: path[Long] :: param[String]("name") :: param[String]("interestType")) {
    (userId: Long, name: String, interestType: String) =>
      context.knowledgeBaseThriftClient.addInterests(UserId(userId), Seq(Interest(name, interestType, Seq.empty[Resource]))) map Ok
  }

  private final val removeInterests: Endpoint[Unit] = post("remove" :: path[Long] :: param[String]("name")) {
    (userId: Long, name: String) =>
      context.knowledgeBaseThriftClient.removeInterests(UserId(userId), Seq(name)) map Ok
  }

  val exampleService = new ExampleService

  def hello: Endpoint[Message] = get("hello") {
    exampleService.getMessage map Ok
  }

  def accept: Endpoint[Message] = post("accept" :: jsonBody[Message]) { incomingMessage: Message =>
    exampleService.acceptMessage(incomingMessage) map Ok
  }

  def acceptMultiple: Endpoint[Seq[Message]] = post("acceptMultiple" :: jsonBody[Seq[Message]]) { incomingMessages: Seq[Message] =>
    Future.collect(incomingMessages map exampleService.acceptMessage) map Ok
  }

  private val api = (getInterests :+: getInterestInfo :+: addInterests :+: removeInterests).handle {
    case e: Exception =>
      println(e.getMessage)
      InternalServerError(e)
  }

  private val serviceWithCors: Service[Request, Response] = new Cors.HttpFilter(policy).andThen(api.toServiceAs[Application.Json])

  def main(): Unit = {
    println(s"Serving the application on port ${port()}")

    val server =
      Http.server
        .withStatsReceiver(statsReceiver)
        .serve(s":${port()}", serviceWithCors)
    closeOnExit(server)

    Await ready adminHttpServer
  }
}
