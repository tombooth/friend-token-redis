(ns tombooth.friend-token.redis
  (:require [taoensso.carmine :as car :refer (wcar)]
            [tombooth.friend-token.token :as token]
            [tombooth.friend-token.token-store :as store]))

(defrecord RedisTokenStore
  [key ttl server-conn]

  store/TokenStore

  (create [_ user]
    (let [id (:username user)
          token-hex (token/create-token key id)]
      (car/wcar server-conn
        (car/setex token-hex ttl {:id id}))
      token-hex))

  (get-metadata [_ token-hex]
    (if-let [token-data (car/wcar server-conn (car/get token-hex))]
      (if (token/verify-token key token-hex (:id token-data))
        token-data)))

  (verify [this token-hex]
    (if-let [token-data (store/get-metadata this token-hex)]
      (:id token-data)
      (store/destroy this token-hex)))

  (extend-life [this token-hex]
    (if-let [token-data (store/get-metadata this token-hex)]
      (car/wcar server-conn (car/expire token-hex ttl))
      (store/destroy this token-hex)))

  (destroy [this token-hex]
    (car/wcar server-conn (car/del token-hex))))




