# Insights Java Client

This repo contains Java code for communicating with Red Hat Insights.

This capability is only available for Red Hat subscribers.

The only authentication flows we support in this release are:

1. mTLS using certs managed by Red Hat Subscription Manager (RHSM)
2. Bearer token authentication in OpenShift Container Platform (OCP)

There are three modules within the project:

* api - The core API (Java 8). All uses cases will need to depend on this
* jboss-cert-helper - A standalone Go binary that is used to provide access to RHEL certs
* runtime - A Java 11 module that provides an HTTP client and some top-level reports. Most implementations will depend on this.

## Building

- Requires Java 11
- The [Spotless Maven Plugin](https://github.com/diffplug/spotless/tree/main/plugin-maven) enforces code formatting conventions and will be run as part of the build.

```
$ mvn clean install
```

To skip the spotless task, run :

	$ mvn clean install -Dskip.spotless=true

Do not raise a PR without having run `spotless:apply` immediately prior.

## License Notes

This project contains some code comprising derivative works based upon open-source code originally released by New Relic.

The original work is also licensed under the Apache 2 License.

## How to test against a token-based (e.g. ephemeral) environment

Here's a command-line guide to uploading some payload:

```
export BASIC_AUTH='the-token'
export HOST='the-host'

curl -F "file=@foo.json.gz;type=application/vnd.redhat.runtimes-java-general.analytics+tgz" \
	-H "Authorization: Basic ${BASIC_AUTH}" \
	"https://${HOST}/api/ingress/v1/upload" -v --insecure
```

Or if you prefer HTTPie:

```
http --verbose --multipart $HOST/api/ingress/v1/upload \
'file@foo.json.gz;type=application/vnd.redhat.runtimes-java-general.analytics+tgz' \
type='application/vnd.redhat.runtimes-java-general.analytics+tgz' \
"Authorization":"Basic ${BASIC_AUTH}"
```

## Environment variables and system properties

For standard, in-process clients, a combination of environment vars & system properties are used to configure the client.
The following environment variables are available to be overriden when using `EnvAndSysPropsInsightsConfiguration`.

| Name                                                 | Default value                           | Description                                                          |
|------------------------------------------------------|-----------------------------------------|----------------------------------------------------------------------|
| `RHT_INSIGHTS_JAVA_OPT_OUT`                          | `false`                                 | Opt out of Red Hat Insights reporting when `true`                    |
| `RHT_INSIGHTS_JAVA_IDENTIFICATION_NAME`              | N/A, must be defined                    | Identification name for reporting                                    |
| `RHT_INSIGHTS_JAVA_CERT_FILE_PATH`                   | `/etc/pki/consumer/cert.pem`            | Certificate path file                                                |
| `RHT_INSIGHTS_JAVA_KEY_FILE_PATH`                    | `/etc/pki/consumer/key.pem`             | Key path file                                                        |
| `RHT_INSIGHTS_JAVA_CERT_HELPER_BINARY`               | `/opt/jboss-cert-helper`                | JBoss certificate retrieval helper                                   |
| `RHT_INSIGHTS_JAVA_AUTH_TOKEN`                       | (empty)                                 | Authentication token for token-based auth, if used                   |
| `RHT_INSIGHTS_JAVA_UPLOAD_BASE_URL`                  | `https://cert.console.stage.redhat.com` | Server endpoint URL                                                  |
| `RHT_INSIGHTS_JAVA_UPLOAD_URI`                       | `/api/ingress/v1/upload`                | Request URI at the server endpoint                                   |
| `RHT_INSIGHTS_JAVA_PROXY_HOST`                       | (empty)                                 | Proxy host, if any                                                   |
| `RHT_INSIGHTS_JAVA_PROXY_PORT`                       | (empty)                                 | Proxy port, if any                                                   |
| `RHT_INSIGHTS_JAVA_CONNECT_PERIOD`                   | 1 day (`P1D`)                           | Connect period, see `java.time.Duration::parse` for the syntax       |
| `RHT_INSIGHTS_JAVA_UPDATE_PERIOD`                    | 5 minutes (`PT5M`)                      | Update period, see `java.time.Duration::parse` for the syntax        |
| `RHT_INSIGHTS_JAVA_HTTP_CLIENT_RETRY_INITIAL_DELAY`  | 2000 (milliseconds as `long`)           | HTTP client exponential backoff: initial retry delay in milliseconds |
| `RHT_INSIGHTS_JAVA_HTTP_CLIENT_RETRY_BACKOFF_FACTOR` | 2 (`long`)                              | HTTP client exponential backoff: factor                              |
| `RHT_INSIGHTS_JAVA_HTTP_CLIENT_RETRY_MAX_ATTEMPTS`   | 10 (`int`)                              | HTTP client exponential backoff: maximum number of retry attempts    |
| `RHT_INSIGHTS_JAVA_ARCHIVE_UPLOAD_DIR`               | `/var/tmp/insights-runtimes/uploads`    | Filesystem location to place archives if HTTP upload fails           |

JVM system properties are derived from the environment variable names.
For instance `RHT_INSIGHTS_JAVA_KEY_FILE_PATH` becomes `rht.insights.java.key.file.path`.

Note that environment variables take priority over system properties.

## Testing & coverage report

To run tests simply use maven command:
```
mvn clean test
```

This project is configured with JaCoCo coverage reporting, to get a coverage report run:
```
mvn clean test -Pcoverage
```

Report will be placed on:
```
(module)/target/site/jacoco/index.html
```
