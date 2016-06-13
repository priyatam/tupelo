;   Copyright (c) Alan Thompson. All rights reserved. 
;   The use and distribution terms for this software are covered by the Eclipse Public
;   License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which can be found in the
;   file epl-v10.html at the root of this distribution.  By using this software in any
;   fashion, you are agreeing to be bound by the terms of this license.
;   You must not remove this notice, or any other, from this software.
(ns tupelo.base64
  "Convert to/from traditional base64 encoding."
  (:use tupelo.core)
  (:require [clojure.string :as str]
            [tupelo.misc    :as misc]
            [tupelo.types   :as types]
            [schema.core    :as s])
  (:gen-class))

; Prismatic Schema type definitions
(s/set-fn-validation! true)   ; #todo add to Schema docs


(def base64-chars
  "A set of chars used for traditional base64 encoding (incl. padding char)"
  (into #{} (flatten [ (misc/char-seq  \a \z)
                       (misc/char-seq  \A \Z)
                       (misc/char-seq  \0 \9) 
                       [\+ \/ \=] ] )))

(def ^:private encoder (java.util.Base64/getEncoder))
(def ^:private decoder (java.util.Base64/getDecoder))

(defn encode-bytes
  "Encodes a byte array into base64, returning a new byte array."
  [data-bytes]
  (assert (types/byte-array? data-bytes))
  (.encode encoder data-bytes))

(defn decode-bytes
  "Decodes a byte array from base64, returning a new byte array."
  [code-bytes]
  (assert (types/byte-array? code-bytes))
  (.decode decoder code-bytes))

(s/defn encode-bytes->str :- s/Str
  "Encodes a byte array into base64, returning a String."
  [data-bytes]
  (assert (types/byte-array? data-bytes))
  (.encodeToString encoder data-bytes))

(s/defn decode-str->bytes
  "Decodes a base64 encoded String, returning a byte array"
  [code-str :- s/Str]
  (.decode decoder code-str))

(s/defn encode-str :- s/Str
  "Encodes a String into base64, returning a String."
  [data-str :- s/Str]
  (-> data-str types/str->bytes encode-bytes->str))

(s/defn decode-str :- s/Str
  "Decodes a base64 encoded String, returning a String."
  [code-str :- s/Str]
  (-> code-str decode-str->bytes types/bytes->str))

