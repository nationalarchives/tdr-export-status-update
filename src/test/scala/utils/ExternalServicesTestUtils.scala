package utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{okJson, post, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.doNothing
import org.mockito.MockitoSugar.mock
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.flatspec.AnyFlatSpec

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

class ExternalServicesTestUtils extends AnyFlatSpec with BeforeAndAfterEach with BeforeAndAfterAll {
  val graphQlServerPort = 9001
  val authServerPort = 9002
  val ssmServerPort = 9003

  val wiremockGraphqlServer = new WireMockServer(graphQlServerPort)
  val wiremockAuthServer = new WireMockServer(authServerPort)
  val wiremockSsmServer = new WireMockServer(ssmServerPort)

  override def beforeAll(): Unit = {
    wiremockSsmServer.start()
  }

  override def afterAll(): Unit = {
    wiremockSsmServer.stop()
  }

  override def beforeEach(): Unit = {
    setUpSsmServer()
  }

  override def afterEach(): Unit = {
    wiremockSsmServer.resetAll()
  }

  def setUpSsmServer(): StubMapping = {
    wiremockSsmServer
      .stubFor(
        post(urlEqualTo("/")).willReturn(okJson("""{"Parameter" : {"Name":"string","Value":"string"}}"""))
      )
  }


  def getInputStream(consignmentId: String): ByteArrayInputStream = {
    val input =
      s"""{
         |  "consignmentId": "$consignmentId"
         |}""".stripMargin
    new ByteArrayInputStream(input.getBytes)
  }

  def outputStream(byteArrayCaptor: ArgumentCaptor[Array[Byte]]): ByteArrayOutputStream = {
    val outputStream = mock[ByteArrayOutputStream]
    doNothing().when(outputStream).write(byteArrayCaptor.capture())
    outputStream
  }
}
