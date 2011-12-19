(ns varys.test.spider
  (:use clojure.test
        varys.spider))

(alter-var-root (var *base-url*) (constantly "http://example.com"))

(deftest test-normalize
  (testing "when the url is already normal"
    (is (= "http://foo.com" (normalize "http://foo.com"))))
  (testing "when the url is relative"
    (is (= "http://example.com/about" (normalize "/about"))))
  (testing "when the url is relative and doesn't start with a '/'"
    (is (= "http://example.com/about" (normalize "about"))))
  (testing "removes the anchor portion of a url"
    (is (= "http://example.com/about" (normalize "about#person")))))

(deftest test-push
  (testing "when the url is not already on the queue"
    (.clear queue)
    (let [s (.size queue)
          url "http://foo.com"]
      (is (= false (.contains queue url)))
      (push url)
      (is (= (+ 1 s) (.size queue)))
      (is (= true (.contains queue url)))))
  (testing "when the url is already on the queue"
    (let [s (.size queue)
          url "http://foo.com"]
      (is (= true (.contains queue url)))
      (push url)
      (is (= s (.size queue)))
      (is (= true (.contains queue url))))))

(deftest test-extract
  (testing "removes mailto links"
    (is (= '("http://example.com")
           (extract '("mailto:jimbob@foo.com" "http://example.com")))))
  (testing "normalizes urls during extraction"
    (is (= '("http://example.com/contacts" "http://example.com/about")
           (extract '("contacts" "about")))))
  (testing "removes urls that don't contain the base url"
    (is (= '("http://example.com")
           (extract '("http://foo.com" "http://example.com"))))))
