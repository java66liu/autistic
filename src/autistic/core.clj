(ns autistic.core
    (:require
        [clojurewerkz.neocons.rest :as neocons]
        [clojurewerkz.neocons.rest.nodes :as node]
        [clojurewerkz.neocons.rest.relationships :as relationship]
        [clojurewerkz.neocons.rest.cypher :as cypher]
    )
)


;;
;; 连接数据库
;;

(neocons/connect! "http://localhost:7474/db/data")



;;
;; 与索引有关的常量
;;

(def user-index "user")

(def key-of-user-index "uid")

(def follow-index "follow")



;;
;; 添加用户 / 查找用户 / 删除用户
;;

(defn add-user!
    "将给定用户添加到数据库中。"
    [uid]
    (node/create-unique-in-index user-index
                                 key-of-user-index
                                 uid                ; index value
                                 {:uid uid}         ; node property
    )
)

(defn get-user
    "根据给定 id ，在数据库中查找用户。
    没找到则回 nil 。"
    [uid]
    (node/find-one user-index
                   key-of-user-index
                   uid                  ; index value
    )
)

(defn remove-user!
    "根据给定 id ，将用户从数据库中移除。
    在删除用户前，必须先删除所有和该用户有关的关系，否则删除失败并抛出异常。"
    [uid]
    (when-let [user-node (get-user uid)]
        ; 先删除节点的索引，再删除节点
        (node/delete-from-index user-node
                                user-index
                                key-of-user-index
                                uid                 ; index value
        )
        (node/delete user-node)
    )
)



;;
;; 关注 / 取消关注
;;

(defn follow!
    "将 target 添加到 user 的关注当中。"
    [user-id target-id]
    (let [
            user-node (get-user user-id)
            target-node (get-user target-id)
         ]
        (relationship/create-unique-in-index user-node      ; to
                                             target-node    ; from
                                             :follow        ; relationship
                                             follow-index
                                             user-id        ; index key
                                             target-id      ; index value
        )
    )
)

(defn get-following-relationship
    "根据给定 user 和 target ，返回 user 关注 target 的关系。
    如果 user 没有关注 target ，那么返回 nil 。"
    [user-id target-id]
    (relationship/find-one follow-index
                           user-id      ; index key
                           target-id    ; index value
    )
)

(defn unfollow!
    "将 target 从 user 的关注中移除。"
    [user-id target-id]
    (when-let [
                rel (get-following-relationship user-id target-id)
              ]
        ; 先删除关系的索引，再删除关系
        (relationship/delete-from-index rel             ; relationship
                                        follow-index
                                        user-id         ; index key
                                        target-id       ; index value
        )
        (relationship/delete rel)
    )
)



;;
;; 关系谓词
;;

(defn following?
    "检查 user 是否正在关注 target 。"
    [user-id target-id]
    (not (nil? (get-following-relationship user-id target-id)))
)

(defn following-by?
    "检查 user 是否正在被 target 关注。"
    [user-id target-id]
    (following? target-id user-id)
)

(defn following-each-other?
    "检查 user 和 target 是否互相关注了对方。"
    [user-id target-id]
    (and 
        (following? user-id target-id)
        (following? target-id user-id)
    )
)



;;
;; 返回所有正在关注 / 返回所有关注者
;; 

(defn get-all-following
    "返回所有 user 正在关注的用户的 uid 。"
    [uid skip-number limit-number]
    (let [
            query-statement (str "START user = node:user(uid = {uid})
                                  MATCH user-[:follow]->target
                                  RETURN target.uid
                                  SKIP " skip-number
                                  "LIMIT " limit-number)
            result (cypher/tquery query-statement {:uid uid})
         ]
        (map #(get % "target.uid") result)
    )
)

(defn get-all-follower
    "返回所有正在关注 user 的用户的 uid 。"
    [uid skip-number limit-number]
    (let [
            query-statement (str "START user = node:user(uid = {uid})
                                  MATCH follower-[:follow]->user
                                  RETURN follower.uid
                                  SKIP " skip-number
                                  "LIMIT " limit-number)
            result (cypher/tquery query-statement {:uid uid})
         ]
        (map #(get % "follower.uid") result)
    )
)



;; 
;; 统计正在关注人数 / 统计关注者人数
;;

(defn count-following-number
    "返回给定用户正在关注的用户数量。"
    [uid]
    (let [
            result (cypher/tquery "START user=node:user(uid = {uid})
                                   MATCH user-[:follow]->target
                                   RETURN count(target)"
                                  {:uid uid}
            )
         ]
        (-> result first (get "count(target)"))
    )
)

(defn count-follower-number
    "返回正在关注给定用户的人数。"
    [uid]
    (let [
            result (cypher/tquery "START user = node:user( uid = {uid} )
                                   MATCH follower-[:follow]->user
                                   RETURN count(follower)"
                                   {:uid uid}
            )
         ]
        (-> result first (get "count(follower)"))
    )
)
