#!/bin/bash
curl -XPOST localhost:8081/mark-fraudulent/5 --data "@example-request.json"
