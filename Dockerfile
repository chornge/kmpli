# Image for running kmpli in a container. Linux only: containers are Linux, so there is no
# macOS or Windows equivalent of this, and the binaries for those platforms are not used here.
#
# Built from the published release binaries rather than from source. Compiling Kotlin/Native
# inside the image would mean carrying a JDK and the konan toolchain for no benefit, and the
# release binaries are the artifacts people actually run.
FROM debian:stable-slim

# The documented Linux runtime prerequisites, and nothing else. ca-certificates is the trust
# store kmpli verifies against, and unzip is what it shells out to when unpacking a project.
# curl is deliberately absent: the binary links libcurl statically, and only the installer
# script needs the curl command.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates unzip \
    && rm -rf /var/lib/apt/lists/*

# TARGETARCH is amd64 or arm64; the release assets call those x64 and arm64. Both are copied
# because a COPY cannot be conditional, then the wrong one is discarded.
ARG TARGETARCH
COPY kmpli-linux-x64 kmpli-linux-arm64 /tmp/
RUN set -eu; \
    case "$TARGETARCH" in \
      amd64) mv /tmp/kmpli-linux-x64 /usr/local/bin/kmpli ;; \
      arm64) mv /tmp/kmpli-linux-arm64 /usr/local/bin/kmpli ;; \
      *) echo "unsupported TARGETARCH: ${TARGETARCH}" >&2; exit 1 ;; \
    esac; \
    chmod +x /usr/local/bin/kmpli; \
    rm -f /tmp/kmpli-linux-x64 /tmp/kmpli-linux-arm64; \
    kmpli --version

# Generation writes into the working directory, so this is where a volume should be mounted.
WORKDIR /out

# Flags, not the wizard. A container started without -it has no terminal to answer prompts, so
# the wizard would have nothing to read; kmpli exits non-zero and says so rather than reporting
# success having generated nothing. The wizard is still reachable with `docker run -it` and no
# arguments. Deliberately no CMD: a default of --help would make the wizard unreachable, since
# there would be no way to ask for "no arguments".
ENTRYPOINT ["kmpli"]
