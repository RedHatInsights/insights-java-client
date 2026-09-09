# Insights Certificate reader

The purpose of this project is to provide a certificate (& key) reader to allow
a non-root user (in our case user `jboss`) access to two root-owned PEM files
stored in previously-known locations in `/etc/pki/consumer/`.

### Deliverables

A single binary that:

* is owned by root
* is world-readable and world-executable
* has the setuid bit set

### Design

The solution:

* only has to work on RHEL
* has no capability to read any other location other than the specified
* will not proceed if run by any user other than `jboss`

> **Note — hardcoded username:** The binary enforces that the calling process's username is
> `jboss` (see `validUsername` constant in `src/main.go`). Deployments that run the JVM under
> a different service account (e.g. `wildfly`, `eap`, or a custom name) will receive exit code 2
> (`ERR_INCORRECT_USER`) and the Java client will fall back to direct cert file reads.
>
> If you need to change the allowed username, rebuild the binary after editing the constant —
> there is currently no runtime configuration for this value.

NOTE: The binary could also be run by root, if root had manipulated their RUID & EUID
to appear to be jboss. This is a pathological case, and to be realized the attacker would
already have general root access, and could read the certs anyway.

### Interface and return codes

```
OK(0, "OK"),
ERR_CURRENT_USER(1, "Failed getting current user"),
ERR_INCORRECT_USER(2, "Called by wrong user"),
ERR_WRONG_ARGS(3, "Called with wrong arguments"),
ERR_NOT_SETUID(4, "Helper not setuid"),
ERR_CERT_OR_KEY(5, "Must specify cert or key"),
ERR_FILE_READ(6, "Could not read file");
```

## Build and test

```
	$ go build
	$ sudo chown root:staff jboss-cert-helper
	$ sudo chmod +s jboss-cert-helper
```

```
	$ go test
```
