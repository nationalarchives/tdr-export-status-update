package utils

import com.github.tomakehurst.wiremock.WireMockServer
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.doNothing
import org.mockito.MockitoSugar.mock
import org.scalatest.flatspec.AnyFlatSpec

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

class ExternalServicesTestUtils extends AnyFlatSpec {

  val wiremockGraphqlServer = new WireMockServer(9001)
  val wiremockAuthServer = new WireMockServer(9002)

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
