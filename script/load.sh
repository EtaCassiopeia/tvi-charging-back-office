#!/bin/bash

echo " >> Submit a new tariff"

curl -H "Content-Type: application/json" \
-d '{"fee":"1.0 EUR/kWh","parkingFee":"0.01 EUR/hour","serviceFee":0.02}' \
-u username:password http://localhost:8181/tariff

# submit charge sessions
for i in {1..5}
do
  echo -e "\n >> submit charge session"
  start_date_time=$(date -v +1S '+%Y-%m-%dT%TZ')
  end_date_time=$(date -v +2S '+%Y-%m-%dT%TZ')

  echo " >> sleep 4 seconds before sending the request"
  sleep 4

  curl -H "Content-Type: application/json" \
  -d '{"driverId":"driver-1","sessionStartTime":"'"$start_date_time"'","sessionEndTime":"'"$end_date_time"'","consumedEnergy":"100.0 kWh"}' \
  http://localhost:8181/session
done

echo -e "\n\n >> get charge sessions"

curl -H "Content-Type: application/json" http://localhost:8181/session/driver-1