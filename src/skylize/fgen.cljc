(ns skylize.fgen
  ^{:doc "Function generators for test.check"
    :author "skylize"}
  (:require [clojure.test.check :as tc]
            [clojure.test.check.clojure-test.assertions :as assert]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop
             #?@(:cljs [:include-macros true])]))

(defn- x-y->map [X Y]
  {:x X
   :y Y
   :f (fn [x]
        (if (= X x)
          Y
          (throw (ex-info "generated function called with incorrect arguments"
                          {:expected X
                           :actual x}))))})

(defn unary
  "Generates a map of: value `x` generated by `x-gen`, value `y` generated by `y-gen`, and function `f` such that `(f x)` yields `y`. Calling `f` with any value besides `x` throws an `ExeptionInfo`.
   
   returns {:x (val-from-xs-gen)
            :y (val-from-y-gen)
            :f (fn-from-x-to-y)}
   ```"
  {:test (fn []
           (let [inc-fn (fn [f x] (inc (f x)))
                 prop (prop/for-all [{:keys [f x y]}
                                     (unary gen/small-integer
                                            gen/small-integer)]
                        (= (+ 1 y) (inc-fn f x)))]
             (assert/check-results (tc/quick-check 5 prop))))}
  [x-gen y-gen]
  (gen/let [x x-gen
            y y-gen]
    (x-y->map x y)))

(defn unary_x->y
  "Generates a map of: value `x` generated by `x-gen`, value `y` generated by passing generated `x` to `y-gen`, and function `f` such that `(f x)` yields `y`.
   
   Calling `f` with any value besides `x` throws an `ExeptionInfo`.
   
   returns {:x (val-from-x-gen)
            :y (val-from-y-gen)
            :f (fn-from-x-to-y)}"
  [x-gen y-gen] 
  (gen/let [x x-gen
            y  (y-gen x)]
    (x-y->map x y)))

(defn unary_x<-y
  "Generates a map of: value `y` generated by `y-gen`, value `x` generated by passing generated `y` to `x-gen`, and function `f` such that `(f x)` yields `y`.
   
   Calling `f` with any value(s) besides applying `x` returns an `ExeptionInfo`.
   
   returns {:x (val-from-x-gen)
            :y (val-from-y-gen)
            :f (fn-from-x-to-y)}"
  [x-gen y-gen]
  (gen/let [y y-gen
            x (x-gen y)]
    (x-y->map x y)))

(defn- xs-y->map [Xs Y]
  {:xs Xs
   :y Y
   :f (fn [& xs]
        (if (or (= Xs xs) (and (empty? Xs) (empty? xs)))
          Y
          (throw (ex-info "generated function called with incorrect arguments"
                          {:expected Xs
                           :actual xs}))))})

(defn n-ary
  "Generates a map of: collection `xs` generated by `xs-gen`, value `y` generated by `y-gen`, and function `f` such that `(apply f xs)` yields `y`.
   
   Calling `f` with any value(s) besides applying `x` returns an `ExeptionInfo`.
   
   returns {:xs (val-from-xs-gen)
            :y (val-from-y-gen)
            :f (fn-from-x-to-y)}"
  [xs-gen y-gen]
  (gen/let [xs xs-gen
            y y-gen]
    (xs-y->map xs y)))

(defn n-ary_xs->y
  "Generates a map of: collection `xs` generated by `xs-gen`, value `y` generated by passing `xs` to `y-gen`, and function `f` such that `(apply f xs)` yields `y`.
   
   Calling `f` with any value(s) besides application of `xs` returns an `ExeptionInfo`.
   
   returns {:xs (val-from-xs-gen)
            :y (val-from-y-gen)
            :f (fn-from-x-to-y)}"
  [xs-gen y-gen]
  (gen/let [xs xs-gen
            y (y-gen xs)]
    (xs-y->map xs y)))

(defn n-ary_xs<-y
  "Generates a map of: value `y` generated by `y-gen`, collection `xs` generated by passing generated value `y` to `xs-gen`, and function `f` such that `(apply f xs)` yields `y`.
   
   Calling `f` with any value(s) besides application of `xs` returns an `ExeptionInfo`.
   
   returns {:xs (val-from-xs-gen)
            :y (val-from-y-gen)
            :f (fn-from-x-to-y)}"
  [xs-gen y-gen]
  (gen/let [y y-gen
            x (xs-gen y)]
    (xs-y->map x y)))

(defn- xs-ys->map [xs ys]
  (let [mappings (zipmap xs ys)]
    {:mappings mappings
     :f (fn [x]
          (if (some #(= % x) xs)
            (mappings x)
            (throw (ex-info "generated function called with incorrect arguments"
                            {:expected {:one-of xs}
                             :actual x}))))}))

(defn unaries
  "Generates a map of: a map `mappings` with keys generated by `x-gen` and values generated by `y-gen`, and fn `f` such that for each key `x` and value `y` in `mappings`, `(f x)` yields `y`.
   
   Calling `f` with any value not in `(keys mappings)` throws an `ExeptionInfo`. Sensitive to uniqueness of x-gen.
   
   returns {:mappings {x-val y-val, ...}
            :f fn-from-key-x-to-value-y}"
  [count-gen x-gen y-gen]
  (gen/let [n (gen/such-that (complement zero?) count-gen)
            xs (gen/vector-distinct x-gen {:num-elements n})
            ys (gen/vector y-gen n)]
    (xs-ys->map xs ys)))

(defn unaries_x->y
  "Generates a map of: a map `mappings` with keys generated by `x-gen` and values generated by passing each key `x` into `y-gen`, and fn `f` such that for each key `x` and value `y` in `mappings`, `(f x)` yields `y`.
   
   Calling `f` with any value(s) besides applying `x` returns an `ExeptionInfo`. Sensitive to uniqueness of `x-gen`.
   
   returns {:mappings {x-val y-val, ...}
            :f fn-from-key-x-to-value-y}"
  [count-gen x-gen y-gen]
  (gen/let [n (gen/such-that (complement zero?) count-gen)
            xs (gen/vector-distinct x-gen {:num-elements n})
            ys (apply gen/tuple (map y-gen xs))]
    (xs-ys->map xs ys)))

(defn unaries_x<-y
  "Generates a map of: a map `mappings` with values generated by `y-gen` and keys generated by passing each value `y` into `x-gen`, and fn `f` such that for each key `x` and value `y` in `mappings`, `(f x)` yields `y`.
   
   Calling `f` with any value(s) besides applying `x` returns an `ExeptionInfo`. Sensitive to uniqueness of `x-gen`.
   
   returns {:mappings {x-val y-val, ...}
            :f fn-from-key-x-to-value-y}"
  [count-gen x-gen y-gen]
  (let [xy-gen (gen/let [y y-gen]
                 (gen/tuple (x-gen y) (gen/return y)))]
    (gen/let [n (gen/such-that (complement zero?) count-gen)
              xs-ys (gen/vector-distinct-by first
                                         xy-gen
                                         {:num-elements n})]
      (xs-ys->map (map first xs-ys) (map second xs-ys)))))

(defn- xss-ys->map [xss ys]
  (let [mappings (zipmap xss ys)]
    {:mappings mappings
     :f (fn [& xs]
          (let [Xs (or xs [])]
            (if (some #(= % Xs) xss)
              (mappings Xs)
              (throw (ex-info "generated function called with incorrect arguments"
                              {:expected {:one-seq-from Xs}
                               :actual Xs})))))}))

(defn n-aries
  "Generates a map of: a map `xss-ys` with keys `xss` generated by `xs-gen`, where `xss` is a collection of 0+ values `x`, and values generated by `y-gen`, and fn `f` such that for each key `x` and value `y` in `mappings`, `(f x)` yields `y`.
   
   Calling `f` with any value(s) besides applying one collection from `(keys mappings)` returns an `ExeptionInfo`. Sensitive to uniqueness when `xs-gen` repeatedly returns an empty collection or a small collection with few value options.
   
   returns {:mappings ({(x1-val, x2-val, ...) y-val,}, ...)
            :f (fn-from-key-x-to-value-y)}"
  [count-gen xs-gen y-gen]
  (gen/let [n (gen/such-that (complement zero?) count-gen)
            xss (gen/vector-distinct xs-gen {:num-elements n})
            ys (gen/vector y-gen n)]
    (xss-ys->map xss ys)))

(defn n-aries_xs->y
  "Generates a map of: a map `mappings` with map keys `xss` generated by `xs-gen` with each key `xs` being a collection of 0+ `x` values to use as input to an n-ary function, and map values `y` generated by passing each `x` value to `y-gen`, and fn `f` such that for each key `xs` and value `y` in `mappings`, `(apply f xs)` yields `y`.
   
   Calling `f` with any value(s) besides applying one collection from `(keys mappings)` returns an `ExeptionInfo`. Sensitive to uniqueness when `xs-gen` repeatedly returns an empty collection or a small collection with few value options.
   
   returns {:mappings ({(x1-val, x2-val, ...) y-val,}, ...)
            :f (fn-from-key-x-to-value-y)}"
  [count-gen xs-gen y-gen]
  (gen/let [n (gen/such-that (complement zero?) count-gen)
            xss (gen/vector-distinct xs-gen {:num-elements n})
            ys (apply gen/tuple (map y-gen xss))]
    (xss-ys->map xss ys)))

(defn n-aries_xs<-y
  "Generates a map of: a map `mappings` with map values `ys` generated by `y-gen`, and map keys `xss` generated by passing each `y` value to `xs-gen` with each key `xs` being a collection of 0+ `x` values to use as input to an n-ary function, and fn `f` such that for each key `xs` and value `y` in `mappings`, `(apply f xs)` yields `y`
   
   Calling `f` with any value(s) besides applying one collection from `(keys mappings)` returns an `ExeptionInfo`. Sensitive to uniqueness when `xs-gen` repeatedly returns an empty collection or a small collection with few value options.
   
   returns {:mappings ({(x1-val, x2-val, ...) y-val,}, ...)
            :f (fn-from-key-x-to-value-y)}"
  [count-gen xs-gen y-gen]
  (let [xss-ys-gen (gen/let [y y-gen]
                     (gen/tuple (xs-gen y) (gen/return y)))]
    (gen/let [n (gen/such-that (complement zero?) count-gen)
              xss-ys (gen/vector-distinct-by first
                                             xss-ys-gen
                                             {:num-elements n})]
      (xss-ys->map (map first xss-ys) (map second xss-ys)))))