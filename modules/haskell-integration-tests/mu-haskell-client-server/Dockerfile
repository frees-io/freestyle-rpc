FROM fpco/stack-build:lts-16.22 as build
COPY --from=cb372/mu-haskell-warm-dot-stack:lts-16.22 /root/.stack /root/.stack
RUN mkdir /opt/build
RUN mkdir /opt/build/bin
COPY . /opt/build
RUN cd /opt/build && stack build --system-ghc --copy-bins --local-bin-path /opt/build/bin
FROM ubuntu:20.04
RUN mkdir -p /opt/mu-haskell-client-server
WORKDIR /opt/mu-haskell-client-server
RUN apt-get update && apt-get install -y \
  ca-certificates \
  libgmp-dev
COPY --from=build /opt/build/bin .
RUN chmod a+x /opt/mu-haskell-client-server/*
EXPOSE 9123
