# OpenSearch Docker Deployment

This directory contains a Docker Compose configuration for running a local OpenSearch cluster with OpenSearch Dashboards.

## Security Warning

**This configuration DISABLES OpenSearch security plugins and is intended ONLY for local development.**

DO NOT use this configuration in production. Without security enabled, anyone can access, modify, or delete your data.

## System Requirements

- **RAM**: 2GB minimum (4GB+ recommended)
  - Each OpenSearch node is configured with 1GB heap (`-Xms1g -Xmx1g`)
  - Two nodes + Dashboards requires at least 2GB available
- **Docker**: Version 20.10 or later
- **Docker Compose**: Version 2.0 or later

## Quick Start

1. Start the services:
   ```bash
   docker-compose -f docker-compose-opensearch.yml up -d
   ```

2. Wait for services to be healthy (may take 1-2 minutes):
   ```bash
   docker-compose -f docker-compose-opensearch.yml ps
   ```

3. Access the services:
   - **OpenSearch API**: http://localhost:9200
   - **OpenSearch Dashboards**: http://localhost:5601

## Service URLs

| Service | URL | Description |
|---------|-----|-------------|
| OpenSearch Node 1 | http://localhost:9200 | REST API for search operations |
| OpenSearch Performance Analyzer | http://localhost:9600 | Performance monitoring endpoint |
| OpenSearch Dashboards | http://localhost:5601 | Web UI for data visualization |

## Verify Cluster Health

Check cluster status:
```bash
curl http://localhost:9200/_cluster/health?pretty
```

View cluster nodes:
```bash
curl http://localhost:9200/_cat/nodes?v
```

## Stopping Services

Stop all services:
```bash
docker-compose -f docker-compose-opensearch.yml down
```

Stop and remove volumes (deletes all data):
```bash
docker-compose -f docker-compose-opensearch.yml down -v
```

## Troubleshooting

### Services fail to start

If you see "memory lock" errors, your system may not have enough available RAM. Try reducing the heap size in `docker-compose-opensearch.yml`:
```yaml
OPENSEARCH_JAVA_OPTS=-Xms512m -Xmx512m
```

### Cannot access services

- Ensure Docker is running: `docker ps`
- Check container logs: `docker-compose logs opensearch-node1`
- Verify ports aren't already in use on your host

## Configuration

- **Cluster size**: 2 nodes for high availability
- **Persistence**: Docker volumes store data across container restarts
- **Restart policy**: `unless-stopped` - services restart automatically unless explicitly stopped
- **Network**: Services communicate via bridge network `search-net`
