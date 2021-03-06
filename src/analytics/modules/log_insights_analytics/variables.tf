variable "database_name" {
  description = "The name of the database this module should create"
}

variable "workgroup_name" {
  description = "The name of the workspace associated to the database"
}

variable "service" {
  description = "The name of a component providing a certain service. This could be another App-System service or an AWS service."
}

variable "logs_bucket_id" {
  description = "The name of the bucket to which S3 access logs are saved."
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}

variable "circuit_breaker_stats_bucket_id" {
  description = "Bucket storing json files of circuit breaker analytics stat"
}

variable "key_federation_download_stats_bucket_id" {
  description = "Bucket storing json files of key federation download stat"
}

variable "key_federation_upload_stats_bucket_id" {
  description = "Bucket storing json files of key federation upload stat"
}

variable "diagnosis_key_submission_stats_bucket_id" {
  description = "The diagnosis key submission S3 bucket"
}

variable "virology_test_stats_bucket_id" {
  description = "The S3 bucket containing statistics for virology test stats"
}
