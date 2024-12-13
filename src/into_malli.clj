#!/usr/bin/env bb

(ns into-malli
  (:require [clojure.test :refer [is]]
            [clj-yaml.core :as yaml]))


(def ^:private test-openapi-specification (->> (slurp "./test-contract.yml")
                                               (yaml/parse-string)))

(def ^:private test-schemas (get-in test-openapi-specification [:components :schemas]))

(def type-map {"object"  :map
               "array"   :vector
               "string"  {"uuid" :uuid
                          :default :string}
               "integer" :int
               "number"  {"integer" :int
                          "double"  :double
                          "float"   :float
                          :default  :int}
               "boolean" :boolean})

(defn get-malli-type
  {:test (fn []
           (is (= (get-malli-type "object" nil)
                  :map))
           (is (= (get-malli-type "array" nil)
                  :vector))
           (is (= (get-malli-type "string" nil)
                  :string))
           (is (= (get-malli-type "number" "double")
                  :double))
           (is (= (get-malli-type "number" nil)
                  :int))
           (is (= (get-malli-type "boolean" nil)
                  :boolean)))}
  ([{type :type
     format :format}]
   (get-malli-type type format))
  ([type format]
   (let [t (get type-map type)]
     (cond
       (map? t)
       (let [format (or (get t format)
                        (:default t))]
         format)

       :else
       t))))

(defn get-ref-type
  {:test (fn []
           (is (= (get-ref-type "#/components/schemas/TestSchemaObject")
                  'TestSchemaObject)))}
  [ref-type]
  (let [ref-name (->> (clojure.string/split ref-type #"/")
                      (last))]
    (symbol ref-name)))

(defn openapi-object?
  [{type       :type
    properties :properties}]
  (or (= "object" type)
      (and (nil? type)
           properties)))

(defn openapi-array?
  [{type :type}]
  (= "array" type))

(defn openapi-ref?
  [{ref :$ref}]
  ref)

(declare parse-array)
(defn parse-object
  {:test (fn []
           (is (= (parse-object (:TestSchemaObject test-schemas))
                  [:map {:closed true}
                   [:prop1 {:optional false} :int]
                   [:prop2 {:optional false} :string]
                   [:prop3 {:optional false} :boolean]
                   [:prop4 {:optional true} :int]]))
           (is (= (parse-object (:TestSchemaObjectWithArray test-schemas))
                  [:map {:closed true}
                   [:prop1 {:optional false} :int]
                   [:prop2 {:optional false} [:vector :string]]]))
           (is (= (parse-object (:TestSchemaObjectWithRefProp test-schemas))
                  [:map {:closed true}
                   [:prop1 {:optional false} :string]
                   [:prop2 {:optional false} 'TestSchemaObject]]))
           (is (= (parse-object (:TestSchemaWithObjectAndNestedObjectArrays test-schemas))
                  [:map {:closed true}
                   [:prop1 {:optional false} [:map {:closed true}
                                              [:nestedProp1 {:optional true} :double]]]
                   [:prop2 {:optional false} [:vector
                                              [:map {:closed true}
                                               [:nestedProp2 {:optional false} :string]]]]]))
           (is (= (parse-object (:TestSchemaObjectWithoutType test-schemas))
                  [:map {:closed true}
                   [:prop1 {:optional false} :int]]))
           )}
  [o]
  (let [required-properties (->> (:required o)
                                 (map keyword)
                                 (into #{}))]
    (->> (:properties o)
         (reduce-kv (fn [a k v]
                      (let [is-optional (not (contains? required-properties k))]
                        (conj a (cond
                                  (openapi-array? v)
                                  [k {:optional is-optional} (parse-array v)]

                                  (openapi-object? v)
                                  [k {:optional is-optional} (parse-object v)]

                                  (openapi-ref? v)
                                  [k {:optional is-optional} (get-ref-type (:$ref v))]

                                  :else
                                  [k {:optional is-optional} (get-malli-type v)]))))
                    [:map {:closed true}]))))

(defn parse-array
  {:test (fn []
           (is (= (parse-array (:TestSchemaArrayRef test-schemas))
                  [:vector 'TestSchemaObject]))
           (is (= (parse-array (:TestSchemaArray test-schemas))
                  [:vector :string]))
           )}
  [a]
  (let [element (get-in a [:items])]
    (cond
      (openapi-ref? element)
      [:vector (get-ref-type (:$ref element))]

      (openapi-object? element)
      [:vector (parse-object element)]

      (openapi-array? element)
      [:vector (parse-array element)]

      :else
      [:vector (get-malli-type element)])
    ))

(defn parse-schema-value
  [v]
  (cond
    (openapi-object? v)
    (parse-object v)

    (openapi-array? v)
    (parse-array v)

    :else
    (get-malli-type (:type v) (:format v))))

(defn openapi->malli
  [schemas]
  (reduce-kv
    (fn [a k v]
      (let [top-level-name (name k)]
        (conj a (list (symbol "def") (symbol top-level-name) (parse-schema-value v)))))
    []
    schemas))

(defn print-the-malli-spec-as-clojure-code
  [specs]
  (doseq [spec specs]
    (clojure.pprint/with-pprint-dispatch clojure.pprint/code-dispatch
                                         (clojure.pprint/pprint spec))))

(def help-str "Usage: -f [filename]")

(defn -main
  [& args]
  (try
    (let [filename (nth args 1)
          openapi-specification (->> (slurp filename)
                                     (yaml/parse-string))
          schemas (get-in openapi-specification [:components :schemas])]
      (print-the-malli-spec-as-clojure-code (openapi->malli schemas)))
    (catch Exception _
      (println help-str))))





























