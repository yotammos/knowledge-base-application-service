package com.knowledgebase

import com.knowledgebase.context.ComponentProvider
import com.knowledgebase.models.Interest
import com.knowledgebase.models.Interest._
import com.knowledgebase.thrift.UserId
import com.twitter.app.Flag
import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.http.filter.Cors
import com.twitter.server.TwitterServer
import com.twitter.util.{Await, Future}
import io.finch.Endpoint
import io.finch._
import io.finch.circe._
import io.finch.syntax._

object KnowledgeBaseApplicationService extends TwitterServer {

  val context = new ComponentProvider
  val port: Flag[Int] = flag("port", 8081, "TCP port for HTTP server")

  val policy: Cors.Policy = Cors.Policy(
    allowsOrigin = _ => Some("*"),
    allowsMethods = _ => Some(Seq("GET", "POST")),
    allowsHeaders = _ => Some(Seq("Accept", "Content-Type"))
  )

  final val getInterests: Endpoint[Seq[Interest]] = get("interests" :: param[String]("userId")) {
    userId: String => context.knowledgeBaseThriftClient.getInterests(UserId(userId.toLong)) map Ok
  }

  final val addInterests: Endpoint[Unit] = post("interests" :: path[Long] :: jsonBody[Interest]) {
    (userId: Long, interest: Interest) =>
      context.knowledgeBaseThriftClient.addInterests(UserId(userId), Seq(interest)) map Ok
  }

  final val hello: Endpoint[String] = get("hello") {
    () => Future.value("Hello World!!") map Ok
  }

  val api = (getInterests :+: addInterests).handle {
    case e: Exception =>
      println(e.getMessage)
      InternalServerError(e)
  }

  val serviceWithCors: Service[Request, Response] = new Cors.HttpFilter(policy).andThen(api.toServiceAs[Application.Json])

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
