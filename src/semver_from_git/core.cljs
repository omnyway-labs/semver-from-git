(ns semver-from-git.core
  (:require [cljs.core.async :as async])
  (:require-macros [cljs.core.async.macros :as async-macros]))

(enable-console-print!)

;; version-file set to default (VERSION) or to argument if set
;;
;; latest-tag = Get the latest semver 
;;   If no latest-tag
;;     latest-tag = 0.0.0
;;
;; If HEAD is > latest-tag <(git rev-list ${latest-tag}..HEAD --count>
;;   new-tag = increment the build (most right) digit of latest-tag
;;
;; major-minor = Get the largest value of a tag that starts with RELEASE-x.y
;; If there is a major-minor and its greater than the major-minor of new-tag
;;  Update the major and/or minor values of new-tag
;;  Reset the build number to 0
;;
;; If current-branch != master
;;  new-tag = (str current-branch "-" new-tag)
;;
;; Print the value of new-tag
;; Write new-tag to version-file

(def git-latest (js/require "git-latest-semver-tag"))
(def git-latest-res-chan (async/chan))

(defn get-latest-semver-tag
  "Use the git-latest-semver-tag npm library to get the latest semver style tag
  of the current repo Its asynchronous so a callback is used to get the resutls.
  This uses core.async to get the results back into the main program"
  []
  (git-latest (fn [err,tag] (async-macros/go (async/>! git-latest-res-chan tag)))))

(defn -main []
  (async-macros/go
    (get-latest-semver-tag)
    (println "Git-Latest RESULTS: " (async/<! git-latest-res-chan))))
