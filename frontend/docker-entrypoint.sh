#!/bin/sh
set -eu

: "${BURGEE_API_BASE_URL:=http://backend:8080}"

# Render the nginx config from the template (envsubst preserves nginx $vars).
envsubst '${BURGEE_API_BASE_URL}' \
  < /etc/nginx/templates/burgee.conf.template \
  > /etc/nginx/conf.d/default.conf
