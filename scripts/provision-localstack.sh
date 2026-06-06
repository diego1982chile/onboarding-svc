#!/usr/bin/env sh
set -eu

LOCALSTACK_CONTAINER="${LOCALSTACK_CONTAINER:-onboarding-localstack}"
AWS_REGION="${AWS_REGION:-us-east-1}"
TOPIC_NAME="${TOPIC_NAME:-identity-events}"
QUEUE_NAME="${QUEUE_NAME:-onboarding-identity-events}"
DLQ_NAME="${DLQ_NAME:-onboarding-identity-events-dlq}"
MAX_RECEIVE_COUNT="${MAX_RECEIVE_COUNT:-5}"

awslocal() {
  docker exec \
    -e AWS_DEFAULT_REGION="${AWS_REGION}" \
    "${LOCALSTACK_CONTAINER}" \
    awslocal "$@"
}

echo "Provisioning LocalStack SNS/SQS resources in ${LOCALSTACK_CONTAINER}..."

TOPIC_ARN="$(awslocal sns create-topic \
  --name "${TOPIC_NAME}" \
  --query 'TopicArn' \
  --output text)"

DLQ_URL="$(awslocal sqs create-queue \
  --queue-name "${DLQ_NAME}" \
  --query 'QueueUrl' \
  --output text)"

DLQ_ARN="$(awslocal sqs get-queue-attributes \
  --queue-url "${DLQ_URL}" \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' \
  --output text)"

QUEUE_URL="$(awslocal sqs create-queue \
  --queue-name "${QUEUE_NAME}" \
  --query 'QueueUrl' \
  --output text)"

QUEUE_ARN="$(awslocal sqs get-queue-attributes \
  --queue-url "${QUEUE_URL}" \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' \
  --output text)"

REDRIVE_POLICY="$(cat <<EOF
{
  "deadLetterTargetArn": "${DLQ_ARN}",
  "maxReceiveCount": "${MAX_RECEIVE_COUNT}"
}
EOF
)"

awslocal sqs set-queue-attributes \
  --queue-url "${QUEUE_URL}" \
  --attributes "{\"RedrivePolicy\":\"$(printf '%s' "${REDRIVE_POLICY}" | tr -d '\n' | sed 's/"/\\"/g')\"}"

awslocal sns subscribe \
  --topic-arn "${TOPIC_ARN}" \
  --protocol sqs \
  --notification-endpoint "${QUEUE_ARN}" \
  >/dev/null

QUEUE_POLICY="$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": "*",
      "Action": "sqs:SendMessage",
      "Resource": "${QUEUE_ARN}",
      "Condition": {
        "ArnEquals": {
          "aws:SourceArn": "${TOPIC_ARN}"
        }
      }
    }
  ]
}
EOF
)"

awslocal sqs set-queue-attributes \
  --queue-url "${QUEUE_URL}" \
  --attributes "{\"Policy\":\"$(printf '%s' "${QUEUE_POLICY}" | tr -d '\n' | sed 's/"/\\"/g')\"}"

cat <<EOF
Provisioned:
  SNS topic: ${TOPIC_ARN}
  SQS queue: ${QUEUE_URL}
  SQS queue ARN: ${QUEUE_ARN}
  DLQ: ${DLQ_URL}
  DLQ ARN: ${DLQ_ARN}
EOF
