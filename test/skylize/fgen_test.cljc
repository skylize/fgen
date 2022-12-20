(ns skylize.fgen-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop
             #?@(:cljs [:include-macros true])]
            [skylize.fgen :as fgen]))

(defspec unary 200
  (prop/for-all [{:keys [x y f]}
                 (fgen/unary
                  gen/any-equatable
                  gen/any-equatable)]
    (= y (f x))))

(defspec unary_x->y 200
  (prop/for-all [{:keys [f x y]}
                 (fgen/unary_x->y
                  gen/any-equatable
                  gen/return)] 
    (= y (f x))))

(defspec unary_x<-y 200
  (prop/for-all [{:keys [f x y]}
                 (fgen/unary_x<-y
                  gen/return
                  gen/any-equatable)]
    (= y (f x))))

(defspec n-ary 200
  (prop/for-all [{:keys [f xs y]}
                 (gen/let [n (gen/resize 10 gen/nat)]
                   (fgen/n-ary
                    (gen/vector gen/any-equatable n)
                    gen/any-equatable))]
    (= y (apply f xs))))

(defspec n-ary_xs->y 200
  (prop/for-all [{:keys [f xs y]}
                 (gen/let [n (gen/resize 10 gen/nat)]
                   (fgen/n-ary_xs->y
                    (gen/vector gen/any-equatable n)
                    gen/return))]
    (= y (apply f xs))))

(defspec n-ary_xs<-y 200
  (prop/for-all [{:keys [f xs y]}
            (gen/let [n (gen/resize 10 gen/nat)]
              (fgen/n-ary_xs<-y
               (fn [y] (gen/vector gen/any-equatable n))
               gen/any-equatable))]
    (= y (apply f xs))))

(defspec unaries 200
  (prop/for-all [{:keys [f mappings]}
                 (fgen/unaries
                  (gen/resize 10 gen/nat)
                  gen/any-equatable
                  gen/any-equatable)]
    (and (pos? (count mappings))
         (let [xs (keys mappings)
               ys (vals mappings)] 
           (= ys (map f xs))))))

(defspec unaries_x->y 200
  (prop/for-all [{:keys [f mappings]}
                 (fgen/unaries_x->y
                  (gen/resize 10 gen/nat)
                  gen/any-equatable
                  gen/return)]
    (and (pos? (count mappings))
         (let [xs (keys mappings)
               ys (vals mappings)]
           (= ys (map f xs))))))

(defspec unaries_x<-y 200
  (prop/for-all [{:keys [f mappings]}
                 (fgen/unaries_x<-y
                  (gen/resize 10 gen/nat)
                  gen/return
                  gen/any-equatable)]
    (and (pos? (count mappings))
         (let [xs (keys mappings)
               ys (vals mappings)]
           (= ys (map f xs))))))

(defspec n-aries 200
  (prop/for-all [{:keys [f mappings]} 
                 (fgen/n-aries
                  (gen/resize 10 gen/nat)
                  (gen/let [n (gen/resize 10 gen/nat)]
                    (gen/vector gen/any-equatable n))
                  gen/any-equatable)]
    (and (pos? (count mappings))
         (let [xs (keys mappings)
               ys (vals mappings)]
           (= ys (map #(apply f %) xs))))))

(defspec n-aries_xs->y 200
  (prop/for-all [{:keys [f mappings]}
                 (fgen/n-aries_xs->y
                  (gen/resize 10 gen/nat)
                  (gen/let [n (gen/resize 10 gen/nat)]
                    (gen/vector gen/any-equatable n))
                  #(if (empty? %)
                     gen/any-equatable
                     (gen/one-of (map gen/return %))))]
    (and (pos? (count mappings))
         (let [xs (keys mappings)
               ys (vals mappings)]
           (= ys (map #(apply f %) xs))))))

(defspec n-aries_xs<-y 200
  (prop/for-all [{:keys [f mappings]}
                 (fgen/n-aries_xs<-y
                  (gen/resize 10 gen/nat)
                  (fn [x]
                    (gen/let [n (gen/resize 10 gen/nat)]
                      (gen/fmap #(conj % x) (gen/vector gen/any-equatable n))))
                  gen/any-equatable)]
    (and (pos? (count mappings))
         (let [xs (keys mappings)
               ys (vals mappings)]
           (= ys (map #(apply f %) xs))))))
