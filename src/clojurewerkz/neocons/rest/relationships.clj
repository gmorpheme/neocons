(ns clojurewerkz.neocons.rest.relationships
  (:import  [java.net URI URL]
            [clojurewerkz.neocons.rest Neo4JEndpoint]
            [clojurewerkz.neocons.rest.nodes Node])
  (:require [clj-http.client               :as http]
            [clojure.data.json             :as json]
            [clojurewerkz.neocons.rest :as rest])
  (:use     [clojurewerkz.neocons.rest.statuses]
            [clojurewerkz.neocons.rest.helpers]
            [clojure.string :only [join]])
  (:refer-clojure :exclude (get)))

;;
;; Implementation
;;

(defrecord Relationship
    [id location-uri start-uri end-uri type data])

(defn- rel-location-for
  [^Neo4JEndpoint endpoint ^long id]
  (str (:relationships-uri endpoint) "/" id))

(defn- relationships-location-for
  [^Neo4JEndpoint endpoint ^Node node kind types]
  (let [query-params (if types
                       (str "/" (join "&" (map name types)))
                       "")]
    (str (:node-uri endpoint) "/" (:id node) "/relationships/" (name kind) query-params)))

(defn- relationships-for
  [^Node node kind types]
  (let [{ :keys [status headers body] } (rest/GET (relationships-location-for rest/*endpoint* node kind types))
        payload  (json/read-json body true)]
    (if (missing? status)
      nil
      (map (fn [rel]
             (Relationship. (extract-id (:self rel)) (:self rel) (:start rel) (:end rel) (:type rel) (:data rel))) payload))))


;;
;; API
;;

(defn create
  [^Node from ^Node to rel-type &{ :keys [data] :or { data {} } }]
  (let [{ :keys [status headers body] } (rest/POST (:create-relationship-uri from)
                                                   :body (json/json-str { :to (:location-uri to) :type rel-type :data data }))
        payload  (json/read-json body true)
        location (:self payload)]
    (Relationship. (extract-id location) location (:start payload) (:end payload) (:type payload) (:data payload))))


(defn get
  [^long id]
  (let [{ :keys [status headers body] } (rest/GET (rel-location-for rest/*endpoint* id))
        payload  (json/read-json body true)]
    (if (missing? status)
      nil
      (Relationship. id (:self payload) (:start payload) (:end payload) (:type payload) (:data payload)))))


(defn delete
  [^long id]
  (let [{ :keys [status headers] } (rest/DELETE (rel-location-for rest/*endpoint* id))]
    (if (or (missing? status)
            (conflict? status))
      [nil status]
      [id  status])))

(defn all-for
  [^Node node &{ :keys [types] }]
  (relationships-for node :all types))

(defn incoming-for
  [^Node node &{ :keys [types] }]
  (relationships-for node :in types))

(defn outgoing-for
  [^Node node &{ :keys [types] }]
  (relationships-for node :out types))
