#!/bin/bash
set -e

echo "Starting Minikube..."
minikube start

echo "Enabling addons..."
minikube addons enable ingress
minikube addons enable metrics-server

echo "Creating Kubernetes secrets..."

kubectl create secret generic finance-secret \
  --from-literal=FINANCE_DB_USERNAME=smartwallet \
  --from-literal=FINANCE_DB_PASSWORD=smartwallet \
  --from-literal=RABBITMQ_USERNAME=smartwallet \
  --from-literal=RABBITMQ_PASSWORD=smartwallet \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic auth-secret \
  --from-literal=AUTH_DB_USERNAME=smartwallet \
  --from-literal=AUTH_DB_PASSWORD=smartwallet \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic budget-secret \
  --from-literal=BUDGET_DB_USERNAME=smartwallet \
  --from-literal=BUDGET_DB_PASSWORD=smartwallet \
  --from-literal=RABBITMQ_USERNAME=smartwallet \
  --from-literal=RABBITMQ_PASSWORD=smartwallet \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic analytics-secret \
  --from-literal=ANALYTICS_DB_USERNAME=smartwallet \
  --from-literal=ANALYTICS_DB_PASSWORD=smartwallet \
  --from-literal=RABBITMQ_USERNAME=smartwallet \
  --from-literal=RABBITMQ_PASSWORD=smartwallet \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic notification-secret \
  --from-literal=NOTIFICATION_DB_USERNAME=smartwallet \
  --from-literal=NOTIFICATION_DB_PASSWORD=smartwallet \
  --from-literal=RABBITMQ_USERNAME=smartwallet \
  --from-literal=RABBITMQ_PASSWORD=smartwallet \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic jwt-public-key \
  --from-file=jwt_public_key=secrets/jwt/public.pem \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic jwt-private-key \
  --from-file=jwt_private_key=secrets/jwt/private.pem \
  --dry-run=client -o yaml | kubectl apply -f -

echo "Applying Kubernetes manifests..."
kubectl apply -R -f k8s/

echo "Waiting for deployments..."
kubectl rollout status deployment/auth-service
kubectl rollout status deployment/finance-service
kubectl rollout status deployment/budget-service
kubectl rollout status deployment/analytics-service
kubectl rollout status deployment/notification-service
kubectl rollout status deployment/api-gateway
kubectl rollout status deployment/smartwallet-web
kubectl rollout status deployment/rabbitmq

echo "Current pods:"
kubectl get pods

echo
echo "SmartWallet local deployment completed."
echo "Run this in another terminal to expose ingress:"
echo
echo "minikube service ingress-nginx-controller -n ingress-nginx --url"
