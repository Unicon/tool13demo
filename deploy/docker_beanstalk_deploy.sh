#!/bin/bash
#
# This script:
#   - builds a Docker image
#   - pushes it to an AWS ECR repo
#   - deploys it to a Beanstalk environment
#
# Required environment variables:
#   - TRAVIS_BUILD_NUMBER         - provided by Travis
#   - TRAVIS_COMMIT               - provided by Travis

set -euox pipefail

export REPO_NAME=lti-service
export AWS_ECR_IMAGE_TAG=travis-build-$TRAVIS_BUILD_NUMBER


if [ -z "$1" ]; then
	echo "Usage: $0 <dev|stg|prod>"
	exit 1
fi


case "$1" in
	dev)
		echo "* Building Docker image and deploying to DEV"
		export AWS_ACCOUNT_NUMBER=725843923591
        export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID_STG:-$AWS_ACCESS_KEY_ID}
		export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY_STG:-$AWS_SECRET_ACCESS_KEY}
		export BEANSTALK_ENV=lti-service-dev-docker
		export BEANSTALK_APPLICATION=lti-service
		export S3_BUCKET_NAME=lumen-beanstalk-lti-service-dockerrun-dev
	;;
	stg)
		echo "* Building Docker image and deploying to STAGING"
		export AWS_ACCOUNT_NUMBER=725843923591
        export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID_STG:-$AWS_ACCESS_KEY_ID}
		export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY_STG:-$AWS_SECRET_ACCESS_KEY}
		export BEANSTALK_ENV=lti-service-stage-docker
		export BEANSTALK_APPLICATION=lti-service
		export S3_BUCKET_NAME=lumen-beanstalk-lti-service-dockerrun-stage
	;;
	dev-exemplar)
		echo "* Building Docker image and deploying to DEV-EXEMPLAR"
		export AWS_ACCOUNT_NUMBER=631711495858
        export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID_PREPROD_EXEMPLAR:-$AWS_ACCESS_KEY_ID}
		export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY_PREPROD_EXEMPLAR:-$AWS_SECRET_ACCESS_KEY}
		export BEANSTALK_ENV=lti-service-dev-exemplar-docker
		export BEANSTALK_APPLICATION=lti-service
		export S3_BUCKET_NAME=lumen-beanstalk-lti-service-dockerrun-dev-exemplar
	;;
	staging-exemplar)
		echo "* Building Docker image and deploying to STAGING-EXEMPLAR"
		export AWS_ACCOUNT_NUMBER=631711495858
        export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID_PREPROD_EXEMPLAR:-$AWS_ACCESS_KEY_ID}
		export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY_PREPROD_EXEMPLAR:-$AWS_SECRET_ACCESS_KEY}
		export BEANSTALK_ENV=lti-service-staging-exemplar-docker
		export BEANSTALK_APPLICATION=lti-service
        export S3_BUCKET_NAME=lumen-beanstalk-lti-service-dockerrun-staging-exemplar
	;;
	prod)
		echo "* Building Docker image and deploying to PROD."
		export AWS_ACCOUNT_NUMBER=523740042085
        export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID_PROD:-$AWS_ACCESS_KEY_ID}
		export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY_PROD:-$AWS_SECRET_ACCESS_KEY}
		export BEANSTALK_ENV=lti-service-prod-docker
		export BEANSTALK_APPLICATION=lti-service
		export S3_BUCKET_NAME=lumen-beanstalk-lti-service-dockerrun-prod
	;;
	*)
		echo "Unknown environment. Skipping Docker build and deploy."
		exit 2
	;;
esac

export PATH=$PATH:$HOME/.local/bin


# echo -e "\n============================================================"
# echo "  Building Docker image"
# echo -e "============================================================\n"

# # build app container
# docker --version
# docker build -f Dockerfile  -t lti-service -t $REPO_NAME:$AWS_ECR_IMAGE_TAG .

echo -e "\n============================================================"
echo "  Get AWS ECR login credentials"
echo -e "============================================================\n"

# Following commands include credentials, don't echo command
set +x
# This runs "docker login" using AWS ECR credentials.
# Output looks like: docker login -u AWS -p p4s$w0rd https://1234.dkr.ecr.us-west-2.amazonaws.com
#eval $(aws ecr get-login --region us-west-2 --no-include-email)

# Docker cli deprecated the previous method of authenticating. Use above for Travis and below for local testing.
docker login -u AWS -p $(aws ecr get-login-password --region us-west-2) https://$AWS_ACCOUNT_NUMBER.dkr.ecr.us-west-2.amazonaws.com
set -x

echo -e "\n============================================================"
echo "  Tagging Docker image and pushing to AWS ECR repo: $REPO_NAME"
echo -e "============================================================\n"

# Tag and push app image
docker tag \
	$REPO_NAME:$AWS_ECR_IMAGE_TAG \
	$AWS_ACCOUNT_NUMBER.dkr.ecr.us-west-2.amazonaws.com/$REPO_NAME:$AWS_ECR_IMAGE_TAG
docker push \
	$AWS_ACCOUNT_NUMBER.dkr.ecr.us-west-2.amazonaws.com/$REPO_NAME:$AWS_ECR_IMAGE_TAG

#
# We now need to:
#   1. Create a Dockerrun.aws.json file with the precise image to be deployed
#      to a Beanstalk environemnt.
#   2. Upload it to an S3 bucket
#   3. Create a Beanstalk application version.
#   4. Update a Beanstalk environment with that app version.
#

DOCKERRUN_FILENAME="Dockerrun.aws.json"
ZIP_FILE_NAME="lti-service-beanstalk-${AWS_ECR_IMAGE_TAG}.zip"

echo -e "\n============================================================"
echo "  Creating Dockerrun.aws.json file"
echo -e "============================================================\n"

cp deploy/Dockerrun.aws.json.template "$DOCKERRUN_FILENAME"
sed -i "s/AWS_ACCOUNT_ID/${AWS_ACCOUNT_NUMBER}/" "$DOCKERRUN_FILENAME"
sed -i "s/ECR_IMAGE_TAG/${AWS_ECR_IMAGE_TAG}/" "$DOCKERRUN_FILENAME"
cat "$DOCKERRUN_FILENAME"

echo -e "\n============================================================"
echo "  Creating beankstalk zip archive"
echo -e "============================================================\n"
zip "$ZIP_FILE_NAME" -r $DOCKERRUN_FILENAME .platform/ 


echo -e "\n============================================================"
echo "  Uploading to S3: $DOCKERRUN_FILENAME -> s3://${S3_BUCKET_NAME}"
echo -e "============================================================\n"

aws s3 cp "$ZIP_FILE_NAME" "s3://${S3_BUCKET_NAME}/"

echo -e "\n============================================================"
echo "  Creating application version"
echo -e "============================================================\n"

aws elasticbeanstalk create-application-version --region us-west-2 --application-name "$BEANSTALK_APPLICATION" --version-label "$AWS_ECR_IMAGE_TAG" --description "Git commit $TRAVIS_COMMIT" --source-bundle S3Bucket="$S3_BUCKET_NAME",S3Key="$ZIP_FILE_NAME"


echo -e "\n============================================================"
echo "  Beginning deployment to AWS Beanstalk environment: $BEANSTALK_ENV"
echo -e "============================================================\n"

aws elasticbeanstalk update-environment --region us-west-2 --environment-name "$BEANSTALK_ENV" --version-label "$AWS_ECR_IMAGE_TAG"
