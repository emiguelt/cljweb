(ns guestbook.app
  (:require [devtools.core :as devtools]
            [guestbook.core :as core]))

(enable-console-print!)

(println "Loading env/dev/cljs/guestbook/app.cljs....")

(devtools/install!)
(core/init!)
