package fs2.aws.examples

import cats.effect._
import cats.effect.kernel.Deferred
import cats.effect.std.Console
import cats.implicits._
import cats.instances.unit
import com.amazonaws.encryptionsdk.{AwsCrypto, CommitmentPolicy}
import fs2.aws.examples.syntax._
import fs2.aws.kinesis.{Kinesis, KinesisCheckpointSettings, KinesisConsumerSettings}
import fs2.compression.Compression
import fs2.{RaiseThrowable, Stream}
import io.circe.fs2._
import io.circe.generic.auto._
import io.laserdisc.pure.cloudwatch.tagless.{Interpreter => CloudwatchInterpreter}
import io.laserdisc.pure.dynamodb.tagless.{Interpreter => DynamoDbInterpreter}
import io.laserdisc.pure.kinesis.tagless.{Interpreter => KinesisInterpreter}
import org.apache.commons.io.output.ByteArrayOutputStream
import org.bouncycastle.jce.provider.BouncyCastleProvider
import pure.kms.tagless.{KmsAsyncClientOp, Interpreter => KmsInterpreter}
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClientBuilder
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder
import software.amazon.awssdk.services.kinesis.KinesisAsyncClientBuilder
import software.amazon.awssdk.services.kms.model.DecryptRequest
import software.amazon.awssdk.utils.IoUtils
import sun.misc.Signal

import java.nio.charset.StandardCharsets


object KinesisExample extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = args.headOption match {
    case None => IO.println("args empty ") *> IO.pure(ExitCode.Error)
    case Some(value) =>
      val appConfig = KinesisAppConfig.appConfig(value)
      (for {
        kinesis <- kAlgebraResource[IO](
          appConfig.kinesisSdkBuilder,
          appConfig.dynamoSdkBuilder,
          appConfig.cloudwatchSdkBuilder
        )
        kms <- KmsInterpreter[IO].KmsAsyncClientResource(appConfig.kmsSkdBuilder)
      } yield (kinesis, KmsInterpreter[IO].create(kms))).use { case (kinesis, kms) =>
        for {
          cancel <- Deferred[IO, Either[Throwable, Unit]]
          _ <- (IO.async_[Unit] { cb =>
            Signal.handle(
              new Signal("INT"), //INT and TERM signals are nearly identical, we have to handle both
              (_: Signal) => cb(Right(unit))
            )
            Signal.handle(
              new Signal("TERM"),
              (_: Signal) => cb(Right(()))
            )
          } *> cancel.complete(Right(()))).background.use(_ =>
            program[IO](kinesis, appConfig.consumerConfig, kms, appConfig.dbResourceId)
              .interruptWhen(cancel).compile.drain
          )
        } yield ExitCode.Success
      }
  }

  private def kAlgebraResource[F[_] : Async](
      kac: KinesisAsyncClientBuilder,
      dac: DynamoDbAsyncClientBuilder,
      cac: CloudWatchAsyncClientBuilder) =
    for {
      d <- DynamoDbInterpreter[F].DynamoDbAsyncClientResource(dac)
      c <- CloudwatchInterpreter[F].CloudWatchAsyncClientResource(cac)
      k <- KinesisInterpreter[F].KinesisAsyncClientResource(kac)
      kAlgebra = Kinesis.create[F](k, d, c)
    } yield kAlgebra

  def program[F[_]: Async: RaiseThrowable: Console](
      kinesis: Kinesis[F],
      consumerSettings: KinesisConsumerSettings,
      kms: KmsAsyncClientOp[F],
      dbResourceId: String
  ): Stream[F, Unit] = {
    val crypto: AwsCrypto = AwsCrypto.builder().withCommitmentPolicy(CommitmentPolicy.RequireEncryptAllowDecrypt).build()

    def decryptEvents(keyBytes: Array[Byte], eventsBytes: Array[Byte]) = {
      val req = decryptRequest(dbResourceId, keyBytes)
      for {
        res <- kms.decrypt(req)
        decrypted <- decrypt(crypto, eventsBytes, res.plaintext.asByteArray())
        decompressed <- decompress(decrypted)
      } yield new String(decompressed, StandardCharsets.UTF_8)
    }

    def base64Decode(str: String) =
      Stream(str).through(fs2.text.base64.decode).compile.toList.map(_.toArray)

    (for {
      record <- kinesis.readFromKinesisStream(consumerSettings)
      _ <- Stream(record)
        .map(cr => StandardCharsets.UTF_8.decode(cr.record.data()).toString)
        .through(stringStreamParser)
        .through(decoder[F, Activity])
        .evalMap { act =>
          for {
            key <- base64Decode(act.key)
            events <- base64Decode(act.databaseActivityEvents)
          } yield (key, events)
        }
        .evalMap(record => decryptEvents(record._1, record._2))
        .through(stringStreamParser)
        .through(decoder[F, DatabaseActivityEvents])
        .flatMap(event => Stream.emits(event.databaseActivityEventList))
        .evalTap(event => Console[F].println(event))
    } yield record).through(kinesis.checkpointRecords(KinesisCheckpointSettings.defaultInstance)).void
  }

  import java.io.ByteArrayInputStream
  import scala.jdk.CollectionConverters._

  private def decryptRequest(dbResourceId: String, bytes: Array[Byte]): DecryptRequest =
    DecryptRequest.builder().ciphertextBlob(SdkBytes.fromByteArray(bytes))
      .encryptionContext(Map("aws:rds:db-id" -> dbResourceId).asJava)
      .build()

  private def decompress[F[_] : Sync](src: Array[Byte]) = {
    Stream.emits(src).through(Compression[F].gunzip()).flatMap { _.content }
      .compile.toList.map(_.toArray)
  }

  private def decrypt[F[_] : Sync](
      awsCrypto: AwsCrypto,
      decoded: Array[Byte],
      decodedDataKey: Array[Byte]
  ) = { // Create a JCE master key provider using the random key and an AES-GCM encryption algorithm

    import com.amazonaws.encryptionsdk.jce.JceMasterKey

    import javax.crypto.spec.SecretKeySpec
    val masterKey = JceMasterKey.getInstance(new SecretKeySpec(decodedDataKey, "AES"), BouncyCastleProvider.PROVIDER_NAME, "DataKey", "AES/GCM/NoPadding")

    def cryptInputStream(
        decoded: Array[Byte],
    ) = Resource.fromAutoCloseable {
      Sync[F].blocking(awsCrypto.createDecryptingStream(masterKey, new ByteArrayInputStream(decoded)))
    }

    def byteArrayOutputStream() = Resource.fromAutoCloseable {Sync[F].blocking{new ByteArrayOutputStream()}}

    def copy(in: Array[Byte]) = (for {
      inStream <- cryptInputStream(in)
      outStream <- byteArrayOutputStream()
    } yield (inStream, outStream)).use { case (inStream, outStream) =>
      Sync[F].blocking(IoUtils.copy(inStream, outStream)) *> Sync[F].delay(outStream.toByteArray)
    }

    copy(decoded)
  }
}
