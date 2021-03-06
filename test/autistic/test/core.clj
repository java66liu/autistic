(ns autistic.test.core
    (:use 
        [autistic.core]
        [clojure.test]
    )
    (:require
        [clojurewerkz.neocons.rest.cypher :as cypher]
    )
)


;;
;; fixture
;;

(defn flush-db []
    ; 删除所有带有一度关系的节点
    (cypher/tquery "START n = node(*)
                    MATCH n-[r]-()
                    DELETE n, r")
    ; 删除所有无关系的单个节点
    (cypher/tquery "START n = node(*)
                    DELETE n")
)

(defn flush-db-fixture [test]
    (flush-db)
    (test)
    (flush-db)
)

(use-fixtures :each flush-db-fixture)



;;
;; user id
;;

(def huangz "huangz")

(def peter "peter")

(def mary "mary")



;;
;; constant for get-all-following and get-all-follower
;;

(def skip-number 0)
(def limit-number 10086)



;;
;; helpers
;;

(defn add-peter!
    []
    (add-user! peter)
)

(defn add-huangz!
    []
    (add-user! huangz)
)

(defn add-mary!
    []
    (add-user! mary)
)



;;
;; tests
;;

; get-user & add-user!

(deftest get-user-RETURN-NIL-when-USER-NOT-EXISTS
    (is 
        (nil? 
            (get-user huangz)
        )
    )
)

(deftest get-user-RETURN-NOT-NIL-when-USER-EXISTS

    (add-huangz!)

    (is 
        (get-user huangz)
    )
)


; remove-user!

(deftest remove-user!-RETURN-NIL-when-USER-NOT-EXISTS-AND-DELETE-FAIL
    (is 
        (nil? (remove-user! huangz))
    )
)

(deftest remove-user!-RETURN-NOT-NIL-when-USER-DELETE-SUCCESS

    (add-huangz!)

    (is
        (not (nil? (remove-user! huangz)))
    )
)


; get-following-relationship

(deftest get-following-relationship-RETURN-false
    
    (add-huangz!)
    (add-peter!)

    (is
        (nil? (get-following-relationship huangz peter))
    )
)

(deftest get-following-relationship-RETURN-true

    (add-huangz!)
    (add-peter!)
    
    (follow! huangz peter)

    (is
        (get-following-relationship huangz peter)
    )
)


; follow!

(deftest follow!-OK

    (add-huangz!)
    (add-peter!)

    (is 
        (not (nil? (follow! huangz peter)))
    )

    (is
        (following? huangz peter)
    )
)


; unfollow!

(deftest unfollow!-OK

    (add-huangz!)
    (add-peter!)
    (follow! huangz peter)
   
    (unfollow! huangz peter)

    (is 
        (not (following? huangz peter))
    )
)


; get-all-following

(deftest get-all-following-RETURN-EMPTY-LIST-when-NOT-FOLLOWING-ANYBODY

    (add-huangz!)

    (is 
        (empty? (get-all-following huangz skip-number limit-number))
    )
)

(deftest get-all-following-RETURN-NON-EMPTY-LIST-when-FOLLOWING-SOME-PEOPLE
    
    (add-huangz!)

    ; following peter

    (add-peter!)
    (follow! huangz peter)

    (let [seq-of-all-following-id (get-all-following huangz skip-number limit-number)]
        (is 
            (= 1
               (count seq-of-all-following-id)
            )
        )
        (is
            (= peter
               (first seq-of-all-following-id)
            )
        )
    )

    ; following peter and mary

    (add-mary!)
    (follow! huangz mary)

    (let [seq-of-all-following-id (get-all-following huangz skip-number limit-number)]
        (is
            (= 2
               (count seq-of-all-following-id)
            )
        )
        (is
            (= (sort seq-of-all-following-id)
               (sort [peter mary])
            )
        )
    )
)

(deftest get-all-following-WORKS-WITH-LIMIT-AND-SKIP

    (add-huangz!)
    (add-peter!)
    (add-mary!)

    (follow! huangz peter)
    (follow! huangz mary)

    ; two following but only return one
    (is
        (= 1
           (count (get-all-following huangz 0 1))
        )
    )
)


; get-all-follower

(deftest get-all-follower-RETURN-EMPTY-LIST-when-NO-ANY-FOLLOWER
    
    (add-huangz!)

    (is
        (empty? (get-all-follower huangz skip-number limit-number))
    )
)

(deftest get-all-follower-RETURN-NON-EMPTY-LIST-when-HAVING-SOME-FOLLOWER

    (add-huangz!)

    ; one follower
    
    (add-peter!)
    (follow! peter huangz)

    (let [all-follower-id (get-all-follower huangz skip-number limit-number)]
        (is
            (= 1
               (count all-follower-id)
            )
        )
        (is
            (= peter
               (first all-follower-id)
            )
        )
    )

    ; two followers

    (add-mary!)
    (follow! mary huangz)

    (let [all-follower-id (get-all-follower huangz skip-number limit-number)]
        (is
            (= 2
               (count all-follower-id)
            )
        )
        (is
            (= (sort all-follower-id)
               (sort [peter mary])
            )
        )
    )
)

(deftest get-all-follower-WORKS-WITH-LIMIT-AND-SKIP

    (add-huangz!)
    (add-peter!)
    (add-mary!)

    (follow! peter huangz)
    (follow! mary huangz)

    ; two following but only return one
    (is
        (= 1
           (count (get-all-follower huangz 0 1))
        )
    )
)


; following?

(deftest following?-RETURN-false
    
    (add-huangz!)
    (add-peter!)

    (is
        (false? (following? huangz peter))
    )
)

(deftest following?-RETURN-true

    (add-huangz!)
    (add-peter!)
    (follow! huangz peter)

    (is
        (following? huangz peter)
    )
)


; following-by?

(deftest following-by?-RETUREN-false

    (add-huangz!)
    (add-peter!)

    (is
        (false? (following-by? peter huangz))
    )
)

(deftest following-by?-RETUREN-true
    
    (add-huangz!)
    (add-peter!)
    (follow! huangz peter)

    (is
        (following-by? peter huangz)
    )
)


; following-each-other?

(deftest following-each-other?-RETURN-false-when-NO-CONNECT-WITH-TWO-USER
    
    (add-huangz!)
    (add-peter!)

    ; 检查两个方向，确保符合交换率
    (is
        (false? (following-each-other? peter huangz))
    )
    (is
        (false? (following-each-other? huangz peter))
    )
)

(deftest following-each-other?-RETUREN-false-when-SINGLE-DIRECT-FOLLOWING

    (add-huangz!)
    (add-peter!)

    (follow! huangz peter)

    ; 检查两个方向，确保符合交换率
    (is
        (false? (following-each-other? peter huangz))
    )
    (is
        (false? (following-each-other? huangz peter))
    )
)
        

(deftest following-each-other?-RETURN-true
    
    (add-huangz!)
    (add-peter!)

    (follow! huangz peter)
    (follow! peter huangz)

    ; 检查两个方向，确保符合交换率
    (is
        (following-each-other? peter huangz)
    )
    (is
        (following-each-other? huangz peter)
    )
)


; count-following-number

(deftest count-following-number-RETURN-0-when-NO-FOLLOWING
    
    (add-huangz!)

    (is
        (= 0
           (count-following-number huangz)
        )
    )
)

(deftest count-following-number-RETURN-NOT-0-when-HAVE-SOME-FOLLOWING
    
    (add-huangz!)

    ; following peter

    (add-peter!)
    (follow! huangz peter)

    (is
        (= 1
           (count-following-number huangz)
        )
    )

    ; following peter and mary

    (add-mary!)
    (follow! huangz mary)

    (is
        (= 2
           (count-following-number huangz)
        )
    )
)


; count-follower-number

(deftest count-follower-number-RETURN-0-when-NO-FOLLOWER

    (add-huangz!)

    (is
        (= 0
           (count-follower-number huangz)
        )
    )
)

(deftest count-follower-number-RETURN-NOT-0-when-HAVE-SOME-FOLLOWER

    (add-huangz!)

    ; one follower

    (add-peter!)
    (follow! peter huangz)

    (is
        (= 1
           (count-follower-number huangz)
        )
    )

    ; two follower

    (add-mary!)
    (follow! mary huangz)

    (is
        (= 2
           (count-follower-number huangz)
        )
    )
)
