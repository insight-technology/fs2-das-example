package fs2.aws.examples

import cats.{Show, derived}
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder

final case class Activity(
    `type`: String,
    version: String,
    databaseActivityEvents: String,
    key: String
)

case class DatabaseActivityEvents(
    `type`: String,
    clusterId: String,
    instanceId: String,
    databaseActivityEventList : List[DatabaseActivityEventList]
)

object DatabaseActivityEvents{
  implicit val eventShow : Show[DatabaseActivityEvents] = derived.semiauto.show
}

case class DatabaseActivityEventList(
    `class`: Option[String],
    clientApplication: Option[String],
    command: Option[String],
    commandText: Option[String],
    dbid: Option[String],
    databaseName: Option[String],
    dbProtocol: Option[String],
    dbUserName: Option[String],
    endTime: Option[String],
    errorMessage: Option[String],
    exitCode: Option[Int],
    logTime: Option[String],
    netProtocol: Option[String],
    objectName: Option[String],
    objectType: Option[String],
    paramList: List[String] = Nil,
    pid: Option[Int],
    remoteHost: Option[String],
    remotePort: Option[String],
    rowCount: Option[Int],
    serverHost: Option[String],
    serverType: Option[String],
    serverVersion: Option[String],
    serviceName: Option[String],
    sessionId: Option[Long],
    startTime: Option[String],
    statementId: Option[Int],
    substatementId: Option[Int],
    transactionId: Option[String],
    engineNativeAuditFields: Option[EngineNativeAuditFields],
)

object DatabaseActivityEventList {
  implicit val eventShow: Show[DatabaseActivityEventList] = derived.semiauto.show
  implicit val defaultConfiguration: Configuration = Configuration.default.withDefaults
  implicit val eventListDecoder: Decoder[DatabaseActivityEventList] = deriveConfiguredDecoder
}


case class EngineNativeAuditFields(
    unifiedAuditPolicies: Option[String],
    fgaPolicyName: Option[String],
    dvObjectStatus: Option[String],
    systemPrivilegeUsed: Option[String],
    olsLabelComponentType: Option[String],
    xsSessionId: Option[Long],
    additionalInfo: Option[String],
    instanceId: Option[Int],
    dvComment: Option[String],
    rmanSessionStamp: Option[Long],
    newName: Option[String],
    dvActionName: Option[String],
    olsProgramUnitName: Option[String],
    olsStringLabel: Option[String],
    rmanSessionRecId: Option[Long],
    objectPrivileges: Option[String],
    olsOldValue: Option[String],
    xsTargetPrincipalName: Option[String],
    xsNsAttribute: Option[String],
    xsNsName: Option[String],
    dblinkInfo: Option[String],
    authenticationType: Option[String],
    objectEdition: Option[String],
    olsPrivilegesGranted: Option[String],
    excludedUser: Option[String],
    dvActionObjectName: Option[String],
    olsLabelComponentName: Option[String],
    excludedSchema: Option[String],
    dpTextParameters1: Option[String],
    xsUserName: Option[String],
    xsEnabledRole: Option[String],
    xsNsAttributeNewVal: Option[String],
    directPathNumColumnsLoaded: Option[Long],
    auditOption: Option[String],
    dvExtendedActionCode: Option[Int],
    xsPackageName: Option[String],
    olsNewValue: Option[String],
    dvReturnCode: Option[Long],
    xsCallbackEventType: Option[String],
    userHost: Option[String],
    globalUserid: Option[String],
    clientIdentifier: Option[String],
    rmanOperation: Option[String],
    terminal: Option[String],
    osUsername: Option[String],
    olsMaxReadLabel: Option[String],
    xsProxyUserName: Option[String],
    xsDataSecPolicyName: Option[String],
    dvFactorContext: Option[String],
    olsMaxWriteLabel: Option[String],
    olsParentGroupName: Option[String],
    excludedObject: Option[String],
    dvRuleSetName: Option[String],
    externalUserid: Option[String],
    executionId: Option[String],
    role: Option[String],
    proxySessionId: Option[Int],
    dpBooleanParameters1: Option[String],
    olsPolicyName: Option[String],
    olsGrantee: Option[String],
    olsMinWriteLabel: Option[String],
    applicationContexts: Option[String],
    xsSchemaName: Option[String],
    dvGrantee: Option[String],
    xsCookie: Option[String],
    dbProxyUsername: Option[String],
    dvActionCode: Option[Int],
    olsPrivilegesUsed: Option[String],
    rmanDeviceType: Option[String],
    xsNsAttributeOldVal: Option[String],
    targetUser: Option[String],
    xsEntityType: Option[String],
    entryId: Option[Int],
    xsProcedureName: Option[String],
    xsInactivityTimeout: Option[Int],
    rmanObjectType: Option[String],
    systemPrivilege: Option[String],
    newSchema: Option[String],
    scn: Option[Int],
)

object EngineNativeAuditFields {
  implicit val engineShow: Show[EngineNativeAuditFields] = derived.semiauto.show
  implicit val fieldCaseConfig: Configuration = Configuration.default.withScreamingSnakeCaseMemberNames
  implicit val engineDecoder: Decoder[EngineNativeAuditFields] = deriveConfiguredDecoder
}


