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
(def GetGamesIdResponse
 [:map
  {:closed true}
  [:id {:optional false} :string]
  [:videoUrl {:optional false} :string]
  [:questions {:optional false} [:vector Question]]])
(def Question
 [:map
  {:closed true}
  [:id {:optional false} :string]
  [:text {:optional false} :string]
  [:alternatives {:optional false} [:vector Alternative]]])
(def Alternative
 [:map
  {:closed true}
  [:id {:optional false} :string]
  [:text {:optional false} :string]])
```

## Setup:
Requirements:
- babashka
- clj-kondo (optional)

> clj-kondo --lint "$(clojure -Spath)" --dependencies --parallel --copy-configs

## Run:
From the project root, run
```
bb -m into-malli -f <path-to-openapi-spec>
```

For development, just run as a normal deps-project