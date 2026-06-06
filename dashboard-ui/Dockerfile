# 🟢 FIX: Upgraded to Node 22 to satisfy Next.js version requirements
FROM node:22-slim

WORKDIR /app

# Copy package manifests
COPY package*.json ./

# Install fresh Linux dependencies inside the container with network timeout safety
RUN npm install --network-timeout=100000 --prefer-offline --no-audit

# Copy the rest of the application source files
COPY . .

# Compile the production application build layer
RUN npm run build

EXPOSE 3000

CMD ["npm", "run", "start"]