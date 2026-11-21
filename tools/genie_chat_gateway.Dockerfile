FROM node:24-alpine
WORKDIR /app
RUN npm install --no-audit --no-fund --ignore-scripts playwright-core@1.57.0
COPY genie_chat_gateway.mjs genie_chat_http.mjs genie_hosted_chat_worker.mjs genie_chat_worker.mjs genie_chat_page.js ./
ENV NODE_OPTIONS=--max-old-space-size=128
CMD ["node", "genie_chat_gateway.mjs"]
