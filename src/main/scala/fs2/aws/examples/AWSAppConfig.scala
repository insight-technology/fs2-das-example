package fs2.aws.examples

import cats.implicits._
import fs2.aws.kinesis.{KinesisConsumerSettings, Polling}
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.{CloudWatchAsyncClient, CloudWatchAsyncClientBuilder}
import software.amazon.awssdk.services.dynamodb.{DynamoDbAsyncClient, DynamoDbAsyncClientBuilder}
import software.amazon.awssdk.services.kinesis.{KinesisAsyncClient, KinesisAsyncClientBuilder}
import software.amazon.awssdk.services.kms.{KmsAsyncClient, KmsAsyncClientBuilder}
import software.amazon.kinesis.common.InitialPositionInStream

import java.util.Date

case class KinesisAppConfig(
    awsRegion: Region,
    appName: String,
    dbResourceId: String,
)
object KinesisAppConfig {
  def appConfig(dbResourceId: String): KinesisAppConfig = KinesisAppConfig(
    awsRegion = Region.AP_NORTHEAST_1,
    appName = "example-of-das-sample",
    dbResourceId = dbResourceId,
  )
}

object syntax {

  implicit class ConfigExtensions(kinesisAppConfig: KinesisAppConfig) {
    private def overwriteStuff[B <: AwsClientBuilder[B, C], C](
        awsClientBuilder: AwsClientBuilder[B, C]
    ) =
      awsClientBuilder
        .region(kinesisAppConfig.awsRegion)

    def kinesisSdkBuilder: KinesisAsyncClientBuilder = overwriteStuff(KinesisAsyncClient.builder())
    def dynamoSdkBuilder: DynamoDbAsyncClientBuilder = overwriteStuff(DynamoDbAsyncClient.builder())
    def cloudwatchSdkBuilder: CloudWatchAsyncClientBuilder =
      overwriteStuff(CloudWatchAsyncClient.builder())
    def kmsSkdBuilder: KmsAsyncClientBuilder = overwriteStuff(KmsAsyncClient.builder())

//    def kmsMasterKeyProvider: KmsMasterKeyProvider = overwriteStuff(KmsMasterKeyProvider.builder())


    def consumerConfig: KinesisConsumerSettings = KinesisConsumerSettings(
      s"aws-rds-das-${kinesisAppConfig.dbResourceId}",
      kinesisAppConfig.appName,
      initialPositionInStream = InitialPositionInStream.TRIM_HORIZON.asLeft[Date],
      retrievalMode = Polling
    )

  }

}
