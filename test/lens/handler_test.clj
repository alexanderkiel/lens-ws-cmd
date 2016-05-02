(ns lens.handler-test
  (:require [clojure.test :refer :all]
            [juxt.iota :refer [given]]
            [lens.handler :refer :all]
            [schema.core :refer [Uuid]]
            [schema.test :refer [validate-schemas]]))

(use-fixtures :once validate-schemas)

(deftest decode-test

  (testing "only name"
    (given (decode [:foo])
      :name := :foo
      :id :- Uuid
      :params := nil))

  (testing "name and id"
    (let [id #uuid "612db307-6a67-4945-8823-238950130bde"]
      (given (decode [:foo id])
        :name := :foo
        :id := id
        :params := nil)))

  (testing "name and params"
    (given (decode [:foo {:a 1}])
      :name := :foo
      :id :- Uuid
      :params := {:a 1}))

  (testing "name, id and params"
    (let [id #uuid "612db307-6a67-4945-8823-238950130bde"]
      (given (decode [:foo id {:a 1}])
        :name := :foo
        :id := id
        :params := {:a 1}))))

