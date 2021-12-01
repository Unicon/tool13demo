#!/bin/bash
#
# Logs into the ECR public docker registry before builds
# To get around image pull quotas in travis

# Path to awscli
export PATH=$PATH:$HOME/.local/bin

export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID_STG:-$AWS_ACCESS_KEY_ID}
export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY_STG:-$AWS_SECRET_ACCESS_KEY}

# log in to lumen public ECR registry
aws ecr-public get-login-password --region us-east-1 | docker login --username AWS --password-stdin public.ecr.aws/o9c0t3w1