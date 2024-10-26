#!/bin/bash

if ! docker network ls | grep -q 'big_stock_network'; then
  echo "Network 'big_stock_network' does not exist. Creating it..."
  docker network create big_stock_network
else
  echo "Network 'big_stock_network' already exists."
fi
docker-compose -f ./infra-bigstock-compose.yaml down
docker-compose -f ./infra-bigstock-compose.yaml up -d