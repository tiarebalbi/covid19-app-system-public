package uk.nhs.nhsx.analyticssubmission

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClientBuilder
import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Environment.EnvironmentKey
import uk.nhs.nhsx.core.EnvironmentKeys
import uk.nhs.nhsx.core.HttpResponses.badRequest
import uk.nhs.nhsx.core.HttpResponses.ok
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.UniqueId
import uk.nhs.nhsx.core.auth.ApiName.Health
import uk.nhs.nhsx.core.auth.ApiName.Mobile
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.core.aws.s3.S3Storage
import uk.nhs.nhsx.core.aws.s3.UniqueObjectKeyNameProvider
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.MobileAnalyticsSubmission
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.core.handler.RoutingHandler
import uk.nhs.nhsx.core.readJsonOrNull
import uk.nhs.nhsx.core.handler.ApiGatewayHandler
import uk.nhs.nhsx.core.routing.Routing.Method.POST
import uk.nhs.nhsx.core.routing.Routing.path
import uk.nhs.nhsx.core.routing.Routing.routes
import uk.nhs.nhsx.core.routing.authorisedBy
import uk.nhs.nhsx.core.routing.withoutSignedResponses

class AnalyticsSubmissionHandler @JvmOverloads constructor(
    environment: Environment = Environment.fromSystem(),
    clock: Clock = CLOCK,
    events: Events = PrintingJsonEvents(clock),
    healthAuthenticator: Authenticator = awsAuthentication(Health, events),
    mobileAuthenticator: Authenticator = awsAuthentication(Mobile, events),
    kinesisFirehose: AmazonKinesisFirehose = AmazonKinesisFirehoseClientBuilder.defaultClient(),
    objectKeyNameProvider: ObjectKeyNameProvider = UniqueObjectKeyNameProvider(clock, UniqueId.ID),
    analyticsConfig: AnalyticsConfig = analyticsConfig(environment)
) : RoutingHandler() {
    override fun handler(): ApiGatewayHandler = handler

    private val service = AnalyticsSubmissionService(
        analyticsConfig,
        objectKeyNameProvider,
        kinesisFirehose,
        events,
        clock
    )

    private val handler = withoutSignedResponses(
        events,
        environment,
        routes(
            authorisedBy(
                mobileAuthenticator,
                path(POST, "/submission/mobile-analytics", ApiGatewayHandler { r, _ ->
                    events(MobileAnalyticsSubmission())
                    Json.readJsonOrNull<ClientAnalyticsSubmissionPayload>(r.body) { events(UnprocessableJson(it)) }
                        ?.let {
                            service.accept(it)
                            ok()
                        } ?: badRequest()
                })
            ),
            authorisedBy(
                healthAuthenticator,
                path(POST, "/submission/mobile-analytics/health", ApiGatewayHandler { _, _ -> ok() })
            )
        )
    )

    companion object {
        private val FIREHOSE_INGEST_ENABLED = EnvironmentKey.bool("firehose_ingest_enabled")
        private val FIREHOSE_STREAM = EnvironmentKey.string("firehose_stream_name")

        private fun analyticsConfig(environment: Environment) = AnalyticsConfig(
            environment.access.required(FIREHOSE_STREAM),
            environment.access.required(FIREHOSE_INGEST_ENABLED)
        )
    }
}
