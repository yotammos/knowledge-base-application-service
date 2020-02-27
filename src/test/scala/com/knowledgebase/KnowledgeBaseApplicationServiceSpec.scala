package com.knowledgebase

import java.nio.charset.StandardCharsets

import com.knowledgebase.models.Message
import com.twitter.io.Buf
import org.scalatest.{FlatSpec, Matchers}
import io.finch.{Application, Input}

class KnowledgeBaseApplicationServiceSpec extends  FlatSpec with Matchers {

  import KnowledgeBaseApplicationService._

  behavior of "the hello endpoint"

  it should "get 'Hello, world!'" in {
     hello(Input.get("/hello")).awaitValueUnsafe() shouldBe Some(Message("Hello, world!"))
  }

  behavior of "the accept endpoint"

  it should "post our message" in {
    val input = Input.post("/accept")
      .withBody[Application.Json](Buf.Utf8("{\"message\":\"here's some post\"}"), Some(StandardCharsets.UTF_8))
    val res = accept(input)
    res.awaitValueUnsafe() shouldBe Some(Message("here's some post"))
  }

  behavior of "the acceptMultiple endpoint"

  it should "post multiple messages" in {
    val input = Input.post("/acceptMultiple")
      .withBody[Application.Json](Buf.Utf8("[{\"message\":\"here's some post\"}]"), Some(StandardCharsets.UTF_8))
    val res = acceptMultiple(input)
    res.awaitValueUnsafe() shouldBe Some(Seq(Message("here's some post")))
  }
}
