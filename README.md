# com.tombooth/friend-token-redis

A Redis implementation of the com.tombooth/friend-token TokenStore protocol.

## Usage

```clojure
[com.tombooth/friend-token-redis "0.1.0-SNAPSHOT"]
```

## Example

```clojure
(ns api.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [cemerick.friend :as friend]
            (cemerick.friend [credentials :as creds])
            [tombooth.friend-token.workflow :as token-workflow]
            [tombooth.friend-token.token-store :as store]
            [tombooth.friend-token.token :as token]
            [tombooth.friend-token.redis :as redis-store]))

(def users {"friend" {:username "friend"
                      :password (creds/hash-bcrypt "clojure")
                      :roles #{::user}}})

(defonce secret-key (token/generate-key))

(def token-header "X-Auth-Token")
(def token-store
  (redis-store/->RedisTokenStore secret-key 30 {:pool {} :spec {:host "127.0.0.1" :port 6379}}))

(defroutes app-routes
  (GET "/" [] (friend/authenticated "Authenticated Hello!!"))
  (GET "/un" [] "Unauthenticated Hello")
  (POST "/extend-token" [:as request]
    (if-let [token-hex (token/from-request request token-header)]
      (do (store/extend-life token-store token-hex) {:status 200})
      {:status 401}))
  (POST "/destroy-token" [:as request]
    (if-let [token-hex (token/from-request request token-header)]
      (do (store/destroy token-store token-hex) {:status 200})
      {:status 401}))
  (route/resources "/")
  (route/not-found "Not Found"))

(def secured-app (friend/authenticate
                   app-routes
                   {:allow-anon? true
                    :unauthenticated-handler #(token-workflow/token-deny %)
                    :login-uri "/authenticate"
                    :workflows [(token-workflow/token
                                  :token-header token-header
                                  :credential-fn (partial creds/bcrypt-credential-fn users)
                                  :token-store token-store
                                  :get-user-fn users )]}))

(def app
  (handler/api secured-app))
```

## License

Copyright © 2013 Thomas Booth

Distributed under the Eclipse Public License, the same as Clojure.
