;   Copyright (c) Alan Thompson. All rights reserved.
;   The use and distribution terms for this software are covered by the Eclipse Public License 1.0
;   (http://opensource.org/licenses/eclipse-1.0.php) which can be found in the file epl-v10.html at
;   the root of this distribution.  By using this software in any fashion, you are agreeing to be
;   bound by the terms of this license.  You must not remove this notice, or any other, from this
;   software.
(ns tupelo.string
  "Tupelo - Making Clojure even sweeter"
  (:refer-clojure :exclude [drop take contains?])
  (:require
    [schema.core :as s]
    [clojure.core :as cc]
    [clojure.string :as str]
    [tupelo.core :as t :refer [spy spyx spyx-pretty let-spy forv]]
    [tupelo.char :as char]
    #?(:clj [clojure.java.io :as io]))
  #?(:clj
     (:import [java.io InputStream ByteArrayInputStream]
              [java.nio.charset StandardCharsets])))

(def phonetic-alphabet
  "A map from keyword character to string phonetic name:
   {:a \"alpha\"    :b \"bravo\"    :c \"charlie\"  :d \"delta\"    :e \"echo\"     :f \"foxtrot\"  :g \"golf\"
    :h \"hotel\"    :i \"india\"    :j \"juliett\"  :k \"kilo\"     :l \"lima\"     :m \"mike\"     :n \"november\"
    :o \"oscar\"    :p \"papa\"     :q \"quebec\"   :r \"romeo \"   :s \"sierra\"   :t \"tango\"    :u \"uniform\"
    :v \"victor\"   :w \"whiskey\"  :x \"x-ray\"    :y \"yankee\"   :z \"zulu\" } "
   {:a "alpha"    :b "bravo"    :c "charlie"  :d "delta"    :e "echo"     :f "foxtrot"  :g "golf"
    :h "hotel"    :i "india"    :j "juliett"  :k "kilo"     :l "lima"     :m "mike"     :n "november"
    :o "oscar"    :p "papa"     :q "quebec"   :r "romeo "   :s "sierra"   :t "tango"    :u "uniform"
    :v "victor"   :w "whiskey"  :x "x-ray"    :y "yankee"   :z "zulu" } )

(s/defn quotes->single :- s/Str ; #todo readme & blog
  [arg :- s/Str]
  (str/replace arg "\""  "'"))

(s/defn quotes->double :- s/Str ; #todo readme & blog
  [arg :- s/Str]
  (str/replace arg "'" "\"" ))

(s/defn ^:no-doc tab-space-oneline-impl :- s/Str
  [tab-size :- s/Int
   src-str :- s/Str]
  (let [idx->spaces (apply t/glue
                      (t/forv [idx (range tab-size)]
                        {idx (vec (repeat (- tab-size idx) \space))}))]
    (loop [result []
           chars  (vec src-str)]
      (if (empty? chars)
        (str/join result)
        (let [c         (t/xfirst chars)
              remaining (t/xrest chars)]
          (if (not= c \tab)
            (recur (t/append result c) remaining)
            (let [curr          (count result)
                  base          (t/it-> (double curr)
                                  (/ it tab-size)
                                  (Math/floor it)
                                  (* it tab-size)
                                  (int it))
                  interval-idx  (- curr base)
                  spaces-needed (idx->spaces interval-idx)]
              (recur (t/glue result spaces-needed) remaining))))))))

(s/defn tabs->spaces :- s/Str
  "Replaces all tabs with appropriate number of spaces (default tab-size => 8)

     Usage:   (tabs->spaces   'abc<tab>def'  => 'ab      cd'
              (tabs->spaces 4 'ab<tab>cd'    => 'ab  cd'
  "
  ([src-str :- s/Str] (tabs->spaces 8 src-str))
  ([tab-size :- s/Int
    src-str :- s/Str]
    (let [lines (str/split-lines src-str)]
      (str/join \newline
        (for [line lines]
          (tab-space-oneline-impl tab-size line))))))

; #todo -> tupelo.string
(defn collapse-whitespace ; #todo readme & blog
  "Replaces all consecutive runs of whitespace characters (including newlines) with a single space.
   Removes any leading or trailing whitespace. Returns a string composed of all tokens
   separated by a single space."
  [arg]
  (-> arg
    str/trim
    (str/replace #"\s+" " ")))

; #todo need test
(defn not-blank?
  "Returns true if the string is not blank."
  [it]
  (not (str/blank? it)))

(s/defn equals-ignore-spacing? :- s/Bool  ; #todo readme & blog
  "Compares arguments for equality using tupelo.misc/collapse-whitespace.
   Equivalent to separating tokens by whitespace and comparing the resulting sequences."
  [& args :- [s/Str]]
  (let [ws-collapsed-args (mapv collapse-whitespace args)]
    (apply = ws-collapsed-args)))

(comment ; #todo finish & use in Lumanu
  (s/defn equals-ignore-spacing-seq? :- s/Bool ; #todo readme & blog
    "Given N sequences of strings, compares corresponding strings from each sequence for equality
    after collapsing continugous whitespace to a single blank. "
    [& string-seqs]
    (every? t/truthy? (apply mapv #(tstr/equals-ignore-spacing? %1 %2) string-seqs)))
  )

; #todo need (squash)         -> (collapse-whitespace (strcat args))       ; (smash ...)         ?
; #todo need (squash-equals?) -> (apply = (mapv squash args))              ; (smash-equals? ...)  ?
;    or (equals-base) or (equals-root) or (squash-equals) or (base-equals) or (core-equals) or (equals-collapse-string...)

(defn ^:deprecated ^:no-doc double-quotes->single-quotes [& args] (apply quotes->single args))
(defn ^:deprecated ^:no-doc single-quotes->double-quotes [& args] (apply quotes->double args))

(defn alphanumeric?       [& args] (every? char/alphanumeric?        (t/strcat args)))
(defn whitespace-horiz?   [& args] (every? char/whitespace-horiz?    (t/strcat args)))
(defn whitespace-eol?     [& args] (every? char/whitespace-eol?      (t/strcat args)))
(defn whitespace?         [& args] (every? char/whitespace?          (t/strcat args)))
(defn lowercase?          [& args] (every? char/lowercase?           (t/strcat args)))
(defn uppercase?          [& args] (every? char/uppercase?           (t/strcat args)))
(defn digit?              [& args] (every? char/digit?               (t/strcat args)))
(defn hex?                [& args] (every? char/hex?                 (t/strcat args)))
(defn alpha?              [& args] (every? char/alpha?               (t/strcat args)))
(defn visible?            [& args] (every? char/visible?             (t/strcat args)))
(defn text?               [& args] (every? char/text?                (t/strcat args)))

; #todo make general version vec -> vec; str-specific version str -> str
; #todo need (substring {:start I :stop J                 } ) ; half-open (or :stop)
; #todo need (substring {:start I :stop J :inclusive true } ) ; closed interval
; #todo need (substring {:start I :count N })

; #todo need (idx "abcdef" 2) -> [ \c ]
; #todo need (indexes "abcde" [1 3 5]) -> (mapv #(idx "abcde" %) [1 3 5]) -> [ \b \d \f ]
; #todo need (idxs    "abcde" [1 3 5]) -> (mapv #(idx "abcde" %) [1 3 5])   ; like matlab

(s/defn clip-text :- s/Str
  "Given a multi-line string, returns a string with each line clipped to a max of N chars "
  [N       :- s/Int
   src-str :- s/Str ]
  (str/join \newline
    (let [lines (str/split-lines src-str)]
      (for [line lines]
        (t/clip-str N line)))))

; #todo need tests
(defn normalize-str
  "Returns a 'normalized' version of str-in, stripped of leading/trailing
   blanks, and with all non-alphanumeric chars converted to hyphens."
  [str-in]
  (-> str-in
    str/trim
    (str/replace #"[^a-zA-Z0-9]" "-")))
; #todo replace with other lib

; %todo define current mode only for (str->kw "ab*cd #()xyz" :sloppy), else throw
(defn str->kw-normalized       ; #todo need test, README
  "Returns a keyword constructed from a normalized string"
  [arg]
  (keyword (normalize-str arg)))

(defn snake->kabob
  "Converts a string from a_snake_case_value to a-kabob-case-value"
  [arg]
  (str/replace arg \_ \- ))

(defn kabob->snake
  "Converts a string from a-kabob-case-value to a_snake_case_value"
  [arg]
  (str/replace arg \- \_ ))

(defn kw-snake->kabob [kw]
  (-> kw
    (t/kw->str)
    (snake->kabob)
    (t/str->kw)))

(defn kw-kabob->snake [kw]
  (->> kw
    (t/kw->str)
    (kabob->snake)
    (t/str->kw)))

; #todo ch->ascii
; #todo ascii->ch
; #todo ch->str
; #todo str->ch

; #todo tupelo.ascii
; #todo (def return 13)
; #todo (def escape 27)

;-----------------------------------------------------------------------------

(s/defn drop :- s/Str  ; #todo add readme
  "Drops the first N chars of a string, returning a string result."
  [n    :- s/Int
   txt  :- s/Str]
  (str/join (cc/drop n txt)))

(s/defn take :- s/Str  ; #todo add readme
  "Drops the first N chars of a string, returning a string result."
  [n    :- s/Int
   txt  :- s/Str]
  (str/join (cc/take n txt)))

(s/defn indent :- s/Str  ; #todo add readme
  "Indents a string by pre-pending N spaces. Returns a string result."
  [n    :- s/Int
   txt  :- s/Str]
  (let [indent-str (str/join (repeat n \space))]
    (str indent-str txt)))

(s/defn indent-lines :- s/Str ; #todo add readme
  "Splits out each line of txt using clojure.string/split-lines, then
  indents each line by prepending N spaces. Joins lines together into
  a single string result, with each line terminated by a single \newline."
  [n :- s/Int
   txt :- s/Str]
  (let [indent-str (str/join (repeat n \space))]
    (t/indent-lines-with indent-str txt)))

(s/defn indent-lines-with :- s/Str  ; #todo delete?  else rename (prefix-lines txt prefix-str) ; add (suffix-lines txt suffix-str)
  "Splits out each line of txt using clojure.string/split-lines, then
  indents each line by prepending it with the supplied string. Joins lines together into
  a single string result, with each line terminated by a single \newline."
  [indent-str :- s/Str
   txt  :- s/Str]
  (t/indent-lines-with indent-str txt))

; #todo add undent (verify only leading whitespace removed)
; #todo add undent-lines

(s/defn increasing? :- s/Bool ; #todo merge with general in tupelo.core
  "Returns true if a pair of strings are in increasing lexicographic order."
  [a :- s/Str
   b :- s/Str ]
  (t/string-increasing? a b) )

(s/defn increasing-or-equal? :- s/Bool ; #todo merge with general in tupelo.core
  "Returns true if a pair of strings are in increasing lexicographic order, or equal."
  [a :- s/Str
   b :- s/Str ]
  (t/string-increasing-or-equal? a b))

(s/defn contains-match?  :- s/Bool
  "Returns true if the regex matches any portion of the intput string."
  [search-str :- s/Str
   re :- s/Any]
  #?(:clj (assert (instance? java.util.regex.Pattern re)))
  (t/truthy? (re-find re search-str)))

(s/defn contains-str?  :- s/Bool
  "Returns true if the intput string contains the target string."
  [search-str :- s/Str
   tgt-str :- s/Str]
  (t/truthy? (str/includes? search-str tgt-str)))

(s/defn grep
  "Given a multi-line text string, returns a string containing lines matching a regex pattern."
  [pattern :- s/Regex
   text :- s/Str]
  (let [lines  (str/split-lines text)
        result (t/keep-if #(contains-match? % pattern) lines)]
    (str/join result)))

(s/defn fgrep
  "Given a multi-line text string, returns a string containing lines matching the target string."
  [tgt :- s/Str
   text :- s/Str]
  (let [lines  (str/split-lines text)
        result (t/keep-if #(contains-str? % tgt) lines)]
    (str/join result)))

#?(:clj
   (s/defn string->stream :- InputStream
     [str-val :- s/Str]
     (io/input-stream
       (.getBytes str-val StandardCharsets/UTF_8))))

(s/defn pad-left :- s/Str
  "Pads a string on the left until it is at least N chars in size"
  ([str-val :- s/Str
    N :- s/Int] (pad-left str-val N \space))
  ([str-val :- s/Str
    N :- s/Int
    pad-char]
    (let [len    (count str-val)
          needed (max 0 (- N len))
          result (str (str/join (repeat needed pad-char)) str-val)]
      result)))

(s/defn pad-right :- s/Str
  "Pads a string on the right until it is at least N chars in size"
  ([str-val :- s/Str
    N :- s/Int] (pad-right str-val N \space))
  ([str-val :- s/Str
    N :- s/Int
    pad-char]
    (let [len    (count str-val)
          needed (max 0 (- N len))
          result (str str-val (str/join (repeat needed pad-char)))]
      result)))

(defn pluralize-with
  "Returns `base-str` when N=1; otherwise appends an `s`"
  [N base-str]
  (if (= N 1)
    base-str
    (t/glue base-str \s)))




















