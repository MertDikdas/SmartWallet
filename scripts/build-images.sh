#!/bin/bash
set -e

build_and_load() {
  service_dir=$1
  deployment_file=$2

  image=$(grep "image:" "$deployment_file" | awk '{print $2}')

  echo "Building $image from $service_dir"

  docker build -t "$image" "$service_dir"
  minikube image load "$image"
}

build_and_load ./auth-service k8s/auth/deployment.yaml
build_and_load ./finance-service k8s/finance/deployment.yaml
build_and_load ./budget-service k8s/budget/deployment.yaml
build_and_load ./analytics-service k8s/analytics/deployment.yaml
build_and_load ./notification-service k8s/notification/deployment.yaml
build_and_load ./api-gateway k8s/api-gateway/deployment.yaml
build_and_load ./smartwallet-web k8s/frontend/deployment.yaml