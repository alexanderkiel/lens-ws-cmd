# Lens Command Webservice

[![Build Status](https://travis-ci.org/alexanderkiel/lens-ws-cmd.svg?branch=master)](https://travis-ci.org/alexanderkiel/lens-ws-cmd)

The Lens Command Webservice provides the HTTP endpoint for commands in the CQRS architecture of Lens. It does the following:

* user authentication
* command coercion
* user authorization
* puts the command in a queue

## Commands

A command is a tuple of command name and params or a triple of name, id and params. The command name is a keyword. The id is an UUID. Params are a map.

```clojure
[:create-user {:name "John Doe"}]
```

```clojure
[:create-user #uuid "612db307-6a67-4945-8823-238950130bde" {:name "John Doe"}]
```

Commands without an id, get an id attached. The advantage of sending a command with id is that the command becomes idempotent.

Commands can reference attachments which can be files or arbitrary binary blobs. Referencing attachments is done by symbols in the param map.

```clojure
[:create-user {:name "John Doe" :image image1}]
```

There are two command endpoints:

## Normal Command Endpoint

This endpoint allows normal command without attachments. The batch command endpoint supports attachments. The command is encoded in the body of a POST request. The encoding is done with Transit.

The batch command endpoint is reachable under `/command`.

## Batch Command Endpoint

The batch command endpoint supports attachments. POST requests with attachments have to use multipart form data. The command is encoded in the `command` field. Attachments are in field named after the references in the command.

The batch command endpoint is reachable under `/batch-command`.

## Usage

FIXME

## License

Copyright © 2016 Alexander Kiel

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
