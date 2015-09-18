(ns riemann.xymon-test
  (:use riemann.xymon
        [riemann.time :only [unix-time]]
        clojure.test)
  (:require [riemann.logging :as logging]))

(logging/init)

(deftest ^:xymon-host-xymon host->xymon-test
  (let [pairs [["foo" "foo"]
               ["example.com" "example,com"]
               ["foo.example.com" "foo,example,com"]]]
    (doseq [[hostname xymon-hostname] pairs]
      (is (= xymon-hostname (host->xymon hostname))))))

(deftest ^:xymon-format event->status-test
  (let [pairs [[{}
                "status . unknown \n"]
               [{:host "foo" :service "bar"}
                "status foo.bar unknown \n"]
               [{:host "foo" :service "bar" :state "ok"}
                "status foo.bar ok \n"]
               [{:host "foo" :service "bar" :state "ok" :description "blah"}
                "status foo.bar ok blah\n"]
               [{:host "foo" :service "bar" :state "ok" :ttl 300}
                "status+5 foo.bar ok \n"]
               [{:host "foo" :service "bar" :state "ok" :ttl 61}
                "status+2 foo.bar ok \n"]
               [{:host "foo" :service "bar" :state "ok" :ttl 59}
                "status+1 foo.bar ok \n"]
               [{:host "foo" :service "bar" :state "ok" :ttl 1}
                "status+1 foo.bar ok \n"]
               [{:host "foo" :service "bar" :state "ok" :ttl 0}
                "status+0 foo.bar ok \n"]
               [{:host "example.com" :service "some.metric rate" :state "ok"}
                "status example,com.some_metric_rate ok \n"]]]
    (doseq [[event line] pairs]
      (is (= line (event->status event))))))

(deftest ^:xymon-enable event->enable-test
  (let [pairs [[{}
                "enable .*"]
               [{:host "foo"}
                "enable foo.*"]
               [{:host "foo" :service "bar"}
                "enable foo.bar"]
               [{:host "foo.example.com" :service "bar"}
                "enable foo,example,com.bar"]
               [{:host "foo" :service "b  a.r"}
                "enable foo.b__a_r"]]]
    (doseq [[event line] pairs]
      (is (= line (event->enable event))))))

(deftest ^:xymon ^:integration xymon-test
         (let [k (xymon nil)]
           (k {:host "riemann.local"
               :service "xymon test"
               :state "green"
               :description "all clear, uh, situation normal"
               :metric -2
               :time (unix-time)}))

         (let [k (xymon nil)]
           (k {:service "xymon test"
               :state "green"
               :description "all clear, uh, situation normal"
               :metric 3.14159
               :time (unix-time)}))

         (let [k (xymon nil)]
           (k {:host "riemann.local"
	       :service "xymon test"
               :state "ok"
               :description "all clear, uh, situation normal"
               :metric 3.14159
               :time (unix-time)}))

         (let [k (xymon nil)]
           (k {:host "no-service.riemann.local"
               :state "ok"
               :description "Missing service, not transmitted"
               :metric 4
               :time (unix-time)})))
