# syntax=docker/dockerfile:1.7
FROM node:22-alpine AS build
WORKDIR /workspace
COPY package.json ./
RUN npm install --no-audit --no-fund
COPY tsconfig.json tsconfig.app.json angular.json ./
COPY src ./src
RUN npx ng build --configuration production

FROM nginx:1.27-alpine
RUN rm -rf /usr/share/nginx/html/* /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/burgee-dashboard/browser /usr/share/nginx/html
COPY nginx.template.conf /etc/nginx/templates/burgee.conf.template
COPY docker-entrypoint.sh /docker-entrypoint.d/40-burgee-config.sh
RUN chmod +x /docker-entrypoint.d/40-burgee-config.sh
EXPOSE 80
