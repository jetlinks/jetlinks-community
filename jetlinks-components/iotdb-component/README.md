# JetLinks Apache IoTDB component

This module integrates JetLinks with the Apache IoTDB 2.0.11 Table model. It provides row-mode and column-mode thing data strategies, the generic `TimeSeriesManager` API, native session pooling, schema creation/evolution, and a standalone IoTDB Compose service for development.

The component targets the IoTDB Table dialect. It does not use the Tree model. IoTDB is configured with one database per component; each JetLinks metric maps to one table. Metric names and non-time column names are reversibly encoded before DDL and SQL, so case-sensitive or punctuation-heavy JetLinks identifiers remain distinct under IoTDB's case-insensitive table identifiers. `time` is the native IoTDB timestamp column.

## Configuration

Enable the component and provide credentials through environment variables:

```yaml
iotdb:
  enabled: true
  database: jetlinks
  node-urls:
    - 127.0.0.1:6667
  username: ${IOTDB_USERNAME}
  password: ${IOTDB_PASSWORD}
  max-size: 16
  query-timeout-in-ms: 10000
  fetch-size: 1000
  connection-timeout-in-ms: 5000
  write-batch-size: 1000
  time-series:
    enabled: true
  things-data:
    enabled: true
```

`iotdb.enabled` controls the client and connection pool. The `time-series` and `things-data` switches independently register the generic time-series manager and the two thing-data repository strategies. The default standalone application configuration keeps IoTDB disabled.

## Local server

Start the pinned single-node server from this module:

```shell
docker compose up -d
docker compose logs -f iotdb
```

The Compose service exposes the native RPC port `6667` and stores data in a named volume. Set `IOTDB_ENDPOINT=127.0.0.1:6667` for an application running on the host.

## Build and tests

Run the module tests with its reactor dependencies from the repository root:

```shell
mvn -pl jetlinks-components/iotdb-component -am test
```

The unit tests cover SQL identifier/literal handling and query filters/pagination. Native session and end-to-end checks run when `IOTDB_TEST_ENDPOINT`, `IOTDB_TEST_USERNAME`, and `IOTDB_TEST_PASSWORD` are set:

```shell
IOTDB_TEST_ENDPOINT=127.0.0.1:6667 \
IOTDB_TEST_USERNAME=<username> \
IOTDB_TEST_PASSWORD=<password> \
mvn -pl jetlinks-components/iotdb-component -am -Djacoco.skip=true \
  -Dtest=IoTDBNativeIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

The native tests create a uniquely named database per test and drop that database after checking the stored data.
