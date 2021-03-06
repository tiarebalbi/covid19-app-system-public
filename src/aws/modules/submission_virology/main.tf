locals {
  identifier_prefix = "${terraform.workspace}-virology-sub"
}

module "virology_submission_role" {
  source = "../../libraries/iam_upload_lambda"
  name   = local.identifier_prefix
  tags   = var.tags
}

module "test_order_lambda" {
  source                    = "../../libraries/java_lambda"
  lambda_function_name      = local.identifier_prefix
  lambda_repository_bucket  = var.lambda_repository_bucket
  lambda_object_key         = var.lambda_object_key
  lambda_handler_class      = "uk.nhs.nhsx.virology.VirologySubmissionHandler"
  lambda_execution_role_arn = module.virology_submission_role.arn
  lambda_timeout            = 20
  lambda_memory             = 1024
  lambda_environment_variables = {
    test_orders_table         = var.test_orders_table_id
    test_results_table        = var.test_results_table_id
    submission_tokens_table   = var.virology_submission_tokens_table_id
    test_orders_index         = var.test_orders_index
    order_website             = var.test_order_website
    register_website          = var.test_register_website
    SSM_KEY_ID_PARAMETER_NAME = "/app/kms/ContentSigningKeyArn"
    custom_oai                = var.custom_oai
  }
  log_retention_in_days = var.log_retention_in_days
  app_alarms_topic      = var.alarm_topic_arn
  tags                  = var.tags
}

module "test_order_gateway" {
  source               = "../../libraries/submission_api_gateway"
  name                 = "virology-sub"
  lambda_function_arn  = module.test_order_lambda.lambda_function_arn
  lambda_function_name = module.test_order_lambda.lambda_function_name
  burst_limit          = var.burst_limit
  rate_limit           = var.rate_limit
  tags                 = var.tags
}
