# fgen

![](https://raw.githubusercontent.com/babashka/babashka/master/logo/badge.svg)

Generate functions with [test.check](https://github.com/clojure/test.check) to allow writing property-based (generative) tests for higher-order functions in Clojure and ClojureScript.

- [Dependency](#dependency)
- [Usage](#usage)
- [Tip](#tip)
- [Generators](#generators)

## Dependency

[deps.edn](https://clojure.org/reference/deps_and_cli)
```clojure
skylize/fgen {:git-tag "0.1.0" :git/sha "..."}
```

## Usage
Require alongside relevant `test.check` namespaces.
```clojure
(ns my.project-test
  (:require [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop
             #?@(:cljs [:include-macros true])]
            [skylize.fgen :as fgen]))
```
An `fgen` takes a generator for creating `x` (input) values and a generator for creating `y` (output) values, and generates a function from a generated `x` to generated `y`. The corresponding `x` and `y` values are returned along with the function in a map.
```clojure
(def small-int->small-int
  (fgen/unary gen/small-integer gen/small-integer))

(gen/sample small-int->small-int 3)
; =>
;; ({:x 0  :y 0 :f #function[my.project-test/x-y->map/fn--10458]}
;;  {:x 1  :y 1 :f #function[my.project-test/x-y->map/fn--10458]}
;;  {:x -1 :y 2 :f #function[my.project-test/x-y->map/fn--10458]})
```

Create test properties for a higher-order functions by generating functions to pass into them as values.

The function under test can call the generated function `f` with the generated value `x`, and receive a known generated `y` value to transform or react to.
```clojure
;; This sample fn `inc-fn` takes a fn `f` as one of its inputs.

(defn inc-fn
  "Increment the result of calling function `f` with value `x`"
  [f x]
  (inc (f x)))

;; The type of input to `f` should not affect the behavior of
; `inc-fn`. But the output of `f` must be a number, and `inc-fn`
;  should act predicatably for any number. This property tests
; `inc-fn` for a large range of ints being returned by `f`.

(def any->large-int
  (fgen/unary gen/any gen/large-integer))

(def inc-fn-large-int
  (prop/for-all [{:keys [f x y]} any->large-int]
    (= (+ 1 y) (inc-fn f x))))

(tc/quick-check 100 inc-fn-large-int)
; =>
;; {:num-tests       100
;;  :pass?           true
;;  :result          true
;;  :seed            1671340300610
;;  :time-elapsed-ms 21           }

;; You can add more similar properties to test other number
;  types.
```

## Tip

Choose the simplest `fgen` and the simplest input generators you can (based on the needs of the property being tested). More complicated generators mean slower test runs and poor shrinkage.

You can expect that, even with fairly simple inputs, an `n-aries_xs<-y` test will likely be quite slow and mostly unshrinkable.

## Generators
- [Unary](#unary)
  - [`unary`](#unary-1)
  - [`unary_x->y`](#unary_x-y)
  - [`unary_x<-y`](#unary_x-y-1)
- [N-ary](#n-ary)
  - [`n-ary`](#n-ary-1)
  - [`n-ary_xs->y`](#n-ary_xs-y)
  - [`n-ary_xs<-y`](#n-ary_xs-y-1)
- [Unaries](#unaries)
  - [`unaries`](#unaries-1)
  - [`unaries_x->y`](#unaries_x-y)
  - [`unaries_x<-y`](#unaries_x-y-1)
- [N-aries](#n-aries)
  - [`n-aries`](#n-aries-1)
  - [`n-aries_xs->y`](#n-aries_xs-y)
  - [`n-aries_xs<-y`](#n-aries_xs-y-1)

### Unary

Unary functions mapping one value to one value.

#### `unary`

The simplest fgen. Generates a function from a generated `x` value to a generated `y` value.

```
x-gen = Generator => x
y-gen = Generator => y
fgen = Generator => {:x x, :y y, :f (x -> y)}

unary = x-gen -> y-gen -> fgen
```
```clojure
(let [prop (unary gen/boolean gen/nat)

      describe (fn [{:keys [f x y]}]
                 {:x x
                  :y y
                  :fx (f x)})]
  (map describe (gen/sample prop 4)))
; =>
;; ({:x false :y 0 :fx 0}
;;  {:x true  :y 0 :fx 0}
;;  {:x true  :y 2 :fx 2}
;;  {:x true  :y 1 :fx 1})
```
#### `unary_x->y`

Like `fgen/unary`, but with a relational constraint. Generates a function from a generated `x` value to a generated `y` value, with the generator for `y` being dependent on the value of `x`.

```
x-gen = Generator => x
y-gen = x -> (Generator => y)
fgen = Generator => {:x x, :y y, :f (x -> y)}

unary_y->x = x-gen -> y-gen -> fgen
```
```clojure
(let [x-gen gen/boolean
      y-gen (fn [b]
              (if b gen/keyword gen/nat))
      prop (unary_x->y x-gen y-gen)

      describe (fn [{:keys [f x y]}]
                 {:x x
                  :y y
                  :fx (f x)})]
  (map describe (gen/sample prop 4)))
; =>
;; ({:x false :y 0   :fx 0  }
;;  {:x true  :y :F! :fx :F!}
;;  {:x false :y 1   :fx 1  }
;;  {:x true  :y :Q  :fx :Q })
```

#### `unary_x<-y`

Like `fgen/unary_x->y` except the relational constraint is reversed. Generates a function from a generated `x` value to a generated `y` value, with the generator for `x` being dependent on the value of `y`.

Reversing the constraint noticably increases complexity, so you should favor the `x->y` variant.

```
y-gen = Generator => y
fgen = Generator => {:x x, :y y :f (x -> y)}
x-gen = y -> (Generator => x)

unary_x<-y = x-gen -> y-gen -> fgen
```
```clojure
(let [x-gen (fn [b]
              (if b gen/keyword gen/nat))
      y-gen gen/boolean
      prop (unary_x<-y x-gen y-gen)

      describe (fn [{:keys [f x y]}]
                 {:x x
                  :y y
                  :fx (f x)})]
  (map describe (gen/sample prop 4)))
; =>
;; ({:x :c :y true  :fx true }
;;  {:x 0  :y false :fx false}
;;  {:x 0  :y false :fx false}
;;  {:x :k :y true  :fx true })
```

### N-ary

N-ary functions mapping application of *n* values to one value.

#### `n-ary`

Like `fgen/unary`, except generates functions of *n* arity. Generates a function from the application of a generated list of values `xs` to a generated `y` value.

```
xs = (x₁, ..., xₙ)
xs-gen = Generator => xs
y-gen = Generator => y
fgen = Generator => {:xs xs, :y y, :f (x₁ -> ...-> xₙ -> y)}

n-ary = xs-gen -> y-gen -> fgen
```
```clojure
(let [xs-gen (gen/tuple gen/nat gen/keyword gen/string)
      y-gen gen/nat
      prop (n-ary xs-gen y-gen)

      describe (fn [{:keys [f xs y]}]
                 {:xs xs
                  :y y
                  :fxs (apply f xs)})]
  (map describe (gen/sample prop 4)))
; =>
;; ({:fxs 0 :xs [0 :m  ""  ] :y 0}
;;  {:fxs 0 :xs [0 :VU ""  ] :y 0}
;;  {:fxs 2 :xs [0 :J? "%;"] :y 2}
;;  {:fxs 3 :xs [0 :H0 "" ] :y 3})
```

#### `n-ary_xs->y`

Like `fgen/n-ary`, except with a relational constraint. Generates a function from the application of a generated list of values `xs` to a generated `y` value, with the generator for `y` being dependent on the values of `xs`.

```
xs = (x₁, ..., xₙ)
xs-gen = Generator => xs
y-gen = xs -> (Generator => y)
fgen = Generator => {:xs xs, :y y, :f (x₁ -> ...-> xₙ -> y)}

n-ary_xs->y = xs-gen -> y-gen -> fgen
```
```clojure
(let [xs-gen (gen/tuple gen/boolean gen/string)
      y-gen (fn [[bool _]]
              (if bool gen/keyword gen/nat))
      prop (n-ary_xs->y xs-gen y-gen)

      describe (fn [{:keys [f xs y]}]
                 {:xs xs
                  :y y
                  :fxs (apply f xs)})]
  (map describe (gen/sample prop 4)))
; =>
;; ({:fxs :Q :xs [true ""   ] :y :Q}
;;  {:fxs :h :xs [true "C"  ] :y :h}
;;  {:fxs 2  :xs [false "%" ] :y 2 }
;;  {:fxs 0  :xs [false "r¶"] :y 0 })
```

#### `n-ary_xs<-y`

Like `fgen/n-ary_xs->y`, except with the relational constraint reversed. Generates a function from the application of a generated list of `xs` to a generated `y` value, with the generator for `xs` being dependent on the value of `y`.

Reversing the constraint increases complexity, so you should favor the xs->y variant.

```
xs = (x₁, ..., xₙ)
xs-gen = y -> (Generator => xs)
y-gen = Generator => y
fgen = Generator => {:xs xs, :y y, :f (x₁ -> ...-> xₙ -> y)}

n-ary_xs<-y = xs-gen -> y-gen -> fgen
```
```clojure
(let [xs-gen (fn [bool]
               (gen/tuple (if bool gen/keyword gen/nat)
                          gen/string))
      y-gen gen/boolean
      prop (n-ary_xs<-y xs-gen y-gen)

      describe (fn [{:keys [f xs y]}]
                 {:xs xs
                  :y y
                  :fxs (apply f xs)})]
  (map describe (gen/sample prop 4)))
; =>
;; ({:fxs true  :xs [:- "" ] :y true }
;;  {:fxs false :xs [0  "" ] :y false}
;;  {:fxs true  :xs [:q "³"] :y true }
;;  {:fxs false :xs [2  "" ] :y false})
```

### Unaries

Unary functions mapping *n* values to *n* values.

#### `unaries`

Like `fgen/unary`, except with multiple `x->y` mappings. Generates a function from any one of *n* generated `x` values to a corresponding selection from *n* generated `y` values.

Generating multiple mappings significantly increases complexity, so you should strongly favor single mapping variants.

```
x-gen = Generator => x
y-gen = Generator => y
fgen = Generator => {:mappings {x y}, :f (x -> y)}

unaries = x-gen -> y-gen -> fgen
```
```clojure
(let [count-gen (gen/return 3)
      x-gen gen/nat
      y-gen gen/boolean
      prop (unaries count-gen x-gen y-gen)

      describe (fn [{:keys [mappings f]}]
                 {:f f
                  :mappings mappings
                  :each-mapping (map (fn [[x y]]
                                       {:x x
                                        :y y
                                        :fx (f x)})
                                     mappings)})]
  (map describe (gen/sample prop 2)))
; =>
;; ({:f            #function[my.project-test/xs-ys->map/fn--12729]
;;   :mappings     {3 false
;;                  1 false
;;                  0 true }
;;   :each-mapping ({:x 3 :y false :fx false}
;;                  {:x 1 :y false :fx false}
;;                  {:x 0 :y true  :fx true })}
;;  {:f            #function[my.project-test/xs-ys->map/fn--12729]
;;   :mappings     {0 false
;;                  1 true
;;                  2 false}
;;   :each-mapping ({:x 0 :y false :fx false}
;;                  {:x 1 :y true  :fx true }
;;                  {:x 2 :y false :fx false})})
```

#### `unaries_x->y`

Like `fgen/unaries`, except with a relational constraint. Generates a function from any one of *n* generated `x` values to a corresponding selection from *n* generated `y` values, with the generator for `y` being dependent on the value of `x`.

Generating multiple mappings significantly increases complexity, so you should strongly favor single mapping variants.

```
count-gen = Generator => positive-integer
x-gen = Generator => x
y-gen = x -> (Generator => y)
fgen = Generator => {:mappings {x y}, :f (x -> y)}

unaries_x->y = count-gen -> x-gen -> y-gen -> fgen
```
```clojure
(let [count-gen (gen/return 2)
      x-gen gen/nat
      y-gen (fn [nat]
              (if (> nat 0) gen/keyword gen/string))
      prop (unaries_x->y count-gen x-gen y-gen)

      describe (fn [{:keys [mappings f]}]
                 {:f f
                  :mappings mappings
                  :each-mapping (map (fn [[x y]]
                                       {:x x
                                        :y y
                                        :fx (f x)})
                                     mappings)})]
  (map describe (gen/sample prop 3)))
; =>
;; ({:f            #function[skylize.fgen/xs-ys->map/fn--12729]
;;   :mappings     {0 ""
;;                  1 :.}
;;   :each-mapping ({:x 0 :y "" :fx ""}
;;                  {:x 1 :y :. :fx :.})}
;;  {:f            #function[skylize.fgen/xs-ys->map/fn--12729]
;;   :mappings     {0 ""
;;                  1 :a}
;;   :each-mapping ({:x 0 :y "" :fx ""}
;;                  {:x 1 :y :a :fx :a})}
;;  {:f            #function[skylize.fgen/xs-ys->map/fn--12729]
;;   :mappings     {2 :*
;;                  0 ""}
;;   :each-mapping ({:x 2 :y :* :fx :*}
;;                  {:x 0 :y "" :fx ""})})
```

#### `unaries_x<-y`

Like `fgen/unaries_x->y`, except with the relational constraint reversed. Generates a function from any one of count *n* generated `x` values to a corresponding selection from *n* generated `y` values, with the generator for `x` being dependent on the value of `y`.

Generating multiple mappings significantly increases complexity, so you should strongly favor single mapping variants.

```
count-gen = Generator => positive-integer
x-gen = y -> (Generator => x)
y-gen = Generator => y
fgen = Generator => {:mappings {x y}, :f (x -> y)}

unaries_x<-y = count-gen -> x-gen -> y-gen -> fgen
```
```clojure
(let [count-gen (gen/return 2)
      x-gen (fn [nat]
              (if (> 0 nat) gen/keyword gen/string))
      y-gen gen/nat
      prop (unaries_x<-y count-gen x-gen y-gen)

      describe (fn [{:keys [mappings f]}]
                 {:f f
                  :mappings mappings
                  :each-mapping (map (fn [[x y]]
                                       {:x x
                                        :y y
                                        :fx (f x)})
                                     mappings)})]
  (map describe (gen/sample prop 3)))
; =>
;; ({:f            #function[skylize.fgen/xs-ys->map/fn--12729]
;;   :mappings     {""   0
;;                  "þÿ" 2}
;;   :each-mapping ({:x ""   :y 0 :fx 0}
;;                  {:x "þÿ" :y 2 :fx 2})}
;;  {:f            #function[skylize.fgen/xs-ys->map/fn--12729]
;;   :mappings     {"" 0
;;                  ""  0}
;;   :each-mapping ({:x "" :y 0 :fx 0}
;;                  {:x ""  :y 0 :fx 0})}
;;  {:f            #function[skylize.fgen/xs-ys->map/fn--12729]
;;   :mappings     {"µ" 2
;;                  ""  1}
;;   :each-mapping ({:x "µ" :y 2 :fx 2}
;;                  {:x ""  :y 1 :fx 1})})
```
### N-aries

N-ary functions mapping *n₁* applications of *n₂* values to *n₁* values.

#### `n-aries`

Like `fgen/unaries`, except generates *n*-ary functions. Generates a function from the application of any one of *n* lists of values `xs` to a corresponding selection from *n* generated `y` values.

Generating multiple mappings significantly increases complexity, so you should strongly favor single mapping variants.

```
xs = (x₁, ..., xₙ)
count-gen = Generator => positive-integer
xs-gen = Generator => xs
y-gen = Generator => y
fgen = Generator => {:mappings {xs y}, :f (x₁ -> ...-> xₙ -> y)}

n-aries = count-gen -> xs-gen -> y-gen -> fgen
```
```clojure
(let [count-gen (gen/return 2)
      xs-gen (gen/tuple gen/boolean gen/char)
      y-gen gen/keyword
      prop (n-aries count-gen xs-gen y-gen)

      describe (fn [{:keys [mappings f]}]
                 {:f f
                  :mappings mappings
                  :each-mapping (map (fn [[xs y]]
                                       {:xs xs
                                        :y y
                                        :fxs (apply f xs)})
                                     mappings)})]
  (map describe (gen/sample prop 3)))
; =>
;; ({:f            #function[skylize.fgen/xss-ys->map/fn--12759]
;;   :mappings     {[false \ñ        ] :-
;;                  [false \backspace] :+}
;;   :each-mapping ({:xs [false \ñ        ] :y :- :fxs :-}
;;                  {:xs [false \backspace] :y :+ :fxs :+})}
;;  {:f            #function[skylize.fgen/xss-ys->map/fn--12759]
;;   :mappings     {[true \] :hg
;;                  [true \] :!2}
;;   :each-mapping ({:xs [true \] :y :hg :fxs :hg}
;;                  {:xs [true \] :y :!2 :fxs :!2})}
;;  {:f            #function[skylize.fgen/xss-ys->map/fn--12759]
;;   :mappings     {[false \ð] :RW
;;                  [true  \z] :T+}
;;   :each-mapping ({:xs [false \ð] :y :RW :fxs :RW}
;;                  {:xs [true  \z] :y :T+ :fxs :T+})})
```

#### `n-aries_xs->y`

Like `fgen/n-aries`, except with a relational constraint. Generates a function from the application of any one of *n* lists of values `xs` to a corresponding selection from *n* generated `y` values, with the generator for `y` being dependent on the values of `xs`.

Generating multiple mappings significantly increases complexity, so you should strongly favor single mapping variants.

```
xs = (x₁, ..., xₙ)
count-gen = Generator => positive-integer
xs-gen = Generator => xs
y-gen = xs -> (Generator => y)
fgen = Generator => {:mappings {xs y}, :f (x₁ -> ...-> xₙ -> y)}

n-aries_xs->y = count-gen -> xs-gen -> y-gen -> fgen
```
```clojure
(let [count-gen (gen/return 2)
      xs-gen (gen/tuple gen/boolean gen/nat gen/char)
      y-gen (fn [[b]]
              (if b gen/keyword gen/string))
      prop (n-aries_xs->y count-gen xs-gen y-gen)

      describe (fn [{:keys [mappings f]}]
                 {:f f
                  :mappings mappings
                  :each-mapping (map (fn [[xs y]]
                                       {:xs xs
                                        :y y
                                        :fxs (apply f xs)})
                                     mappings)})]
  (map describe (gen/sample prop 3)))
; =>
;; ({:f            #function[skylize.fgen/xss-ys->map/fn--12759]
;;   :mappings     {[false 0 \i] ""
;;                  [true  0 \ê] :?}
;;   :each-mapping ({:xs [false 0 \i] :y "" :fxs ""}
;;                  {:xs [true  0 \ê] :y :? :fxs :?})}
;;  {:f            #function[skylize.fgen/xss-ys->map/fn--12759]
;;   :mappings     {[false 0 \7] "]"
;;                  [false 1 \Ú] ""}
;;   :each-mapping ({:xs [false 0 \7] :y "]" :fxs "]"}
;;                  {:xs [false 1 \Ú] :y "" :fxs ""})}
;;  {:f            #function[skylize.fgen/xss-ys->map/fn--12759]
;;   :mappings     {[true 1 \x] :M
;;                  [true 0 \j] :M}
;;   :each-mapping ({:xs [true 1 \x] :y :M :fxs :M}
;;                  {:xs [true 0 \j] :y :M :fxs :M})})
```

#### `n-aries_xs<-y`

Like `fgen/n-aries`, except with the relational constraint reversed. Generates a function from the application of any one of *n* lists of values `xs` to a corresponding selection from *n* generated `y` values, with the generator for `xs` being dependent on the value of `y`.

Reversing the constraint increases complexity, so you should favor the `xs->y` variant. Generating multiple mappings significantly increases complexity, so you should strongly favor single mapping variants.

```
xs = (x₁, ..., xₙ)
count-gen = Generator => positive-integer
xs-gen = y -> (Generator => xs)
y-gen = Generator => y
fgen = Generator => {:mappings {xs y}, :f (x₁ -> ...-> xₙ -> y)}

n-aries_xs<-y = count-gen -> xs-gen -> y-gen -> fgen
```
```clojure
(let [count-gen (gen/return 2)
      xs-gen (fn [nat]
               (let [x1 (if (> nat 0) gen/keyword gen/string)]
                 (gen/tuple x1 gen/keyword gen/char)))
      y-gen gen/nat
      prop (n-aries_xs<-y count-gen xs-gen y-gen)

      describe (fn [{:keys [mappings f]}]
                 {:f f
                  :mappings mappings
                  :each-mapping (map (fn [[xs y]]
                                       {:xs xs
                                        :y y
                                        :fxs (apply f xs)})
                                     mappings)})]
  (map describe (gen/sample prop 3)))
; =>
;; ({:f            #function[skylize.fgen/xss-ys->map/fn--12759]
;;   :mappings     {["" :- \Ü] 0
;;                  ["" :W \G] 0}
;;   :each-mapping ({:xs ["" :- \Ü] :y 0 :fxs 0}
;;                  {:xs ["" :W \G] :y 0 :fxs 0})}
;;  {:f            #function[skylize.fgen/xss-ys->map/fn--12759]
;;   :mappings     {["©" :h \s] 0
;;                  ["õ" :* \I] 0}
;;   :each-mapping ({:xs ["©" :h \s] :y 0 :fxs 0}
;;                  {:xs ["õ" :* \I] :y 0 :fxs 0})}
;;  {:f            #function[skylize.fgen/xss-ys->map/fn--12759]
;;   :mappings     {["Ú" :!- \}] 0
;;                  [:kw :_D \Î] 2}
;;   :each-mapping ({:xs ["Ú" :!- \}] :y 0 :fxs 0}
;;                  {:xs [:kw :_D \Î ] :y 2 :fxs 2})})
```
