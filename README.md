# openapi-to-malli

- This is a simple clojure application that takes [OpenAPI](https://swagger.io/specification/) component schemas and generates [Malli](https://github.com/metosin/malli) schemas
- Takes everything under the components.schemas path in the OpenAPI spec
- Currently only support yaml-definitions
- Doesn't at all take the formal OpenAPI specification into account
- Doesn't at all take the best malli types into account
- All maps are closed by default
- Optionality is explicit

## Example
```
bb -m into-malli -f ./assets/readme-example.yml
```
```clojure
(def Game
  [:map
   {:closed true}
   [:id :uuid]
   [:videoUrl [:maybe :string]]
   [:questions [:vector Question]]])
(def Question
  [:map
   {:closed true}
   [:id :uuid]
   [:text :string]
   [:alternatives [:vector Alternative]]])
(def Alternative [:map {:closed true} [:id :uuid] [:text :string]])
```

## Setup:
Requirements:
- babashka
- clj-kondo (optional)

> clj-kondo --lint "$(clojure -Spath)" --dependencies --parallel --copy-configs

## Run:
During development, just run it as a regular deps.edn project. When running as a script, you can either do:
```
bb -m into-malli -f <path-to-openapi-spec>
```
Or just add a symbolic link to src/into_malli.clj in some directory that is on PATH and invoke it like
```
into-malli -f <path-to-openapi-spec>
```

For development, just run as a normal deps-project