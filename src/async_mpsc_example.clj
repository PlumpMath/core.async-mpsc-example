(ns async-mpsc-example
  (:require [clojure.core.async :as async :refer [>! >!! <!!]]
            [hack :refer [println]]))

(def default-num-producers 10)

(defn consume!
  [channel num-values]
  ; Using 'future' instead of 'thread' because 'thread' returns a channel
  ; with the value of body, and we don't need this.
  (future
   (loop [counter 1]
     (let [{:keys [value when-done]} (<!! channel)]
       (println "consuming" value "...")

       ; This just simulates a "background job" which takes a while to execute;
       ; would not be required in real code.
       (Thread/sleep 1000)

       (deliver when-done (* 2 value))
       (if (= counter num-values)
         (do
           (async/close! channel)
           (println "DONE FOR REAL"))
         (recur (inc counter)))))))

(defn produce!
  [channel num-producers]
  (dotimes [p num-producers]
    ; Normally you wouldn't use 'future' with core.async but in this case
    ; 'future' just simulates threads coming in from the webserver to handle
    ; HTTP requests.
    (future
     ; Another channel could be used here instead of a promise, but that would
     ; be using a power drill when a screwdriver will work just fine.
     (let [when-done (promise)]
       (>!! channel {:when-done when-done
                     :value p})
       (let [result (deref when-done)]
         (println "Result for" p "is" result))))))

(defn main
  ([]
   (main default-num-producers))
  ([num-producers]
   (let [channel (async/chan)]  ; imagine this would be stored in the TK service context
     (consume! channel num-producers)
     (produce! channel num-producers))))

; TODO:
;  * use a buffer and do something based on contents before put (e.g. 503 if too full)
