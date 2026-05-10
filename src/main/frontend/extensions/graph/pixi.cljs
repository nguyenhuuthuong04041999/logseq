(ns frontend.extensions.graph.pixi
  (:require [cljs-bean.core :as bean]
            [camel-snake-kebab.core :as csk]
            [clojure.string :as string]
            ["d3-force"
             :refer [forceCenter forceCollide forceLink forceManyBody forceSimulation forceX forceY]
             :as force]
            [goog.object :as gobj]
            [frontend.colors :as colors]
            ["graphology" :as graphology]
            ["pixi-graph-fork" :as Pixi-Graph]
            ["@pixi/core" :refer [Texture]]
            ["@pixi/sprite" :refer [Sprite]]
            ["@pixi/mixin-get-global-position"]
            ["react" :as react]
            ["react-dom/server.browser" :as react-dom-server]))

(defonce *graph-instance (atom nil))
(defonce *simulation (atom nil))
(defonce *simulation-paused?
  (atom false))

(def Graph (gobj/get graphology "Graph"))

(defonce colors
  ["#1f77b4"
   "#ff7f0e"
   "#2ca02c"
   "#d62728"
   "#9467bd"
   "#8c564b"
   "#e377c2"
   "#7f7f7f"
   "#bcbd22"
   "#17becf"])

;; Icon rendering for graph nodes ----------------------------------------------
;;
;; pixi-graph-fork không render icon — phần icon trong README chỉ là tài liệu
;; của upstream pixi-graph. Ta tự gắn Sprite icon vào nodeGfx sau khi PixiGraph
;; tạo xong. Hai loại icon được hỗ trợ:
;;   - {:type :tabler-icon :id "book"}: render React component qua react-dom/server
;;     thành SVG string -> Image -> Canvas -> PIXI.Texture
;;   - {:type :emoji :id "📚"}: vẽ ký tự emoji trực tiếp lên canvas

(defonce *icon-texture-cache (atom {}))

(defn- normalize-icon
  "Chấp nhận string/keyword (hiểu là tabler-icon) hoặc map sẵn :type/:id."
  [icon]
  (cond
    (or (string? icon) (keyword? icon))
    (let [s (name icon)]
      (when-not (string/blank? s) {:type :tabler-icon :id s}))

    (and (map? icon) (:id icon) (some? (:type icon)))
    (let [t (:type icon)]
      {:type (cond (keyword? t) t
                   (string? t) (keyword t)
                   :else nil)
       :id (:id icon)})

    :else nil))

(defn- relative-luminance
  "Tính luminance tương đối (0–1) của màu hex #RRGGBB."
  [hex]
  (try
    (let [h (string/replace hex #"^#" "")
          r (js/parseInt (subs h 0 2) 16)
          g (js/parseInt (subs h 2 4) 16)
          b (js/parseInt (subs h 4 6) 16)
          ch (fn [c]
               (let [c (/ c 255)]
                 (if (<= c 0.03928)
                   (/ c 12.92)
                   (js/Math.pow (/ (+ c 0.055) 1.055) 2.4))))]
      (+ (* 0.2126 (ch r)) (* 0.7152 (ch g)) (* 0.0722 (ch b))))
    (catch :default _ 0.5)))

(defn- contrast-color
  "Trả về #ffffff hoặc #000000 tùy theo màu nào tương phản hơn với bg-hex."
  [bg-hex]
  (if (> (relative-luminance bg-hex) 0.179) "#000000" "#ffffff"))

(defn- icon-cache-key
  [icon color size]
  (str (some-> icon :type name) "|" (:id icon) "|" color "|" size))

(defn- canvas->texture
  [canvas]
  (.from Texture canvas))

(defn- build-emoji-texture
  [emoji-id size]
  (let [px (* size 2)
        canvas (js/document.createElement "canvas")
        _ (set! (.-width canvas) px)
        _ (set! (.-height canvas) px)
        ctx (.getContext canvas "2d")]
    (set! (.-font ctx) (str (* size 1.6) "px \"Apple Color Emoji\",\"Segoe UI Emoji\",\"Noto Color Emoji\",serif"))
    (set! (.-textAlign ctx) "center")
    (set! (.-textBaseline ctx) "middle")
    (.fillText ctx emoji-id (/ px 2) (/ px 2))
    (canvas->texture canvas)))

(defn- tabler-icon->svg-string
  [icon-id color size]
  (let [tabler-icons (gobj/get js/window "tablerIcons")]
    (if-not tabler-icons
      (do (js/console.warn "[graph-icon] window.tablerIcons not loaded yet") nil)
      (let [pascal (str "Icon" (csk/->PascalCase icon-id))
            comp (gobj/get tabler-icons pascal)]
        (if-not comp
          (do (js/console.warn "[graph-icon] missing tabler icon" pascal) nil)
          (let [el (react/createElement comp #js {:size size
                                                  :color color
                                                  :stroke 2})]
            (.renderToStaticMarkup react-dom-server el)))))))

(defn- svg-string->texture-async
  [svg-str size cb]
  (let [px (* size 2)
        blob (js/Blob. #js [svg-str] #js {:type "image/svg+xml;charset=utf-8"})
        url (js/URL.createObjectURL blob)
        img (js/Image.)]
    (set! (.-onload img)
          (fn []
            (try
              (let [canvas (js/document.createElement "canvas")
                    _ (set! (.-width canvas) px)
                    _ (set! (.-height canvas) px)
                    ctx (.getContext canvas "2d")]
                (.drawImage ctx img 0 0 px px)
                (cb (canvas->texture canvas)))
              (catch :default e
                (js/console.error "icon canvas error" e)
                (cb nil))
              (finally
                (js/URL.revokeObjectURL url)))))
    (set! (.-onerror img)
          (fn [e]
            (js/console.error "icon img load failed" e)
            (js/URL.revokeObjectURL url)
            (cb nil)))
    (set! (.-src img) url)))

(defn- get-icon-texture
  "Trả texture đã cache hoặc build mới rồi gọi cb."
  [icon color size cb]
  (let [k (icon-cache-key icon color size)]
    (if-let [tex (get @*icon-texture-cache k)]
      (cb tex)
      (case (:type icon)
        :emoji
        (when-let [emoji-id (:id icon)]
          (let [tex (build-emoji-texture emoji-id size)]
            (swap! *icon-texture-cache assoc k tex)
            (cb tex)))

        :tabler-icon
        (when-let [svg (tabler-icon->svg-string (:id icon) color size)]
          (svg-string->texture-async
           svg size
           (fn [tex]
             (when tex
               (swap! *icon-texture-cache assoc k tex)
               (cb tex)))))

        nil))))

(defn- do-attach-icon-sprite!
  [pixi-node icon node-size node-color dark?]
  (let [node-gfx (.-nodeGfx pixi-node)
        ;; Ưu tiên dùng màu node để tính contrast; fallback về dark?
        bg-color (or node-color (if dark? "#1f2937" "#e5e7eb"))
        color (contrast-color bg-color)
        tex-px 32
        display-size (max 14 (* node-size 1.5))]
    (try
      (get-icon-texture
       icon color tex-px
       (fn [tex]
         (when (and tex node-gfx)
           (let [sprite (new Sprite tex)]
             (.set (.-anchor sprite) 0.5)
             (set! (.-width sprite) display-size)
             (set! (.-height sprite) display-size)
             (.addChild node-gfx sprite)))))
      (catch :default e
        (js/console.error "attach icon failed" e)))))

(defn- attach-icon-sprite!
  "PixiGraph tạo node bất đồng bộ (batch 20/100ms), nên retry tới khi node có
  trong getNodesObjects() rồi mới gắn Sprite icon."
  ([pixi-graph node-id icon node-size node-color dark?]
   (attach-icon-sprite! pixi-graph node-id icon node-size node-color dark? 0))
  ([pixi-graph node-id icon node-size node-color dark? attempts]
   (let [nodes-objects (.getNodesObjects pixi-graph)
         pixi-node (.get nodes-objects (str node-id))]
     (cond
       pixi-node
       (do-attach-icon-sprite! pixi-node icon node-size node-color dark?)

       (< attempts 60)
       (js/setTimeout
        #(attach-icon-sprite! pixi-graph node-id icon node-size node-color dark? (inc attempts))
        100)

       :else
       (js/console.warn "[graph-icon] gave up waiting for node" node-id)))))

;; -----------------------------------------------------------------------------

(defn default-style
  [dark?]
  {:node {:size   (fn [node]
                    (or (.-size node) 8))
          :border {:width 0}
          :color  (fn [node]
                    (if-let [parent (gobj/get node "parent")]
                      (when-let [parent (if (= parent "ls-selected-nodes")
                                          parent
                                          (.-id node))]
                        (let [v (js/Math.abs (hash parent))]
                          (nth colors (mod v (count colors)))))
                      (.-color node)))
          :label  {:content  (fn [node] (.-label node))
                   :type     (.-TEXT (.-TextType Pixi-Graph))
                   :fontSize 12
                   :color (if dark? "rgba(255, 255, 255, 0.8)" "rgba(0, 0, 0, 0.8)")
                   :padding  4}}
   :edge {:width 1
          :color (if dark? (or (colors/get-accent-color) "#094b5a") "#cccccc")}})

(defn default-hover-style
  [_dark?]
  {:node {:color (or (colors/get-accent-color) "#6366F1")
          :label {:backgroundColor "rgba(238, 238, 238, 1)"
                  :color           "#333333"}}
   :edge {:color "#A5B4FC"}})

(defn layout!
  "Node forces documentation can be read in more detail here https://d3js.org/d3-force"
  [nodes links link-dist charge-strength charge-range]
  (let [simulation (forceSimulation nodes)]
    (-> simulation
        (.force "link"
                ;; The link force pushes linked nodes together or apart according to the desired link distance.
                ;; The strength of the force is proportional to the difference between the linked nodes distance
                ;; and the target distance, similar to a spring force.
                (-> (forceLink)
                    (.id (fn [d] (.-id d)))
                    (.distance link-dist)
                    (.links links)))
        (.force "charge"
                ;; The many-body (or n-body) force applies mutually amongst all nodes.
                ;; It can be used to simulate gravity or electrostatic charge.
                (-> (forceManyBody)
                    ;; The minimum distance between nodes over which this force is considered.
                    ;; A minimum distance establishes an upper bound on the strength of the force between two nearby nodes, avoiding instability.
                    (.distanceMin 1)
                    ;; The maximum distance between nodes over which this force is considered.
                    ;; Specifying a finite maximum distance improves performance and produces a more localized layout.
                    (.distanceMax charge-range)
                    ;; For a cluster of nodes that is far away, the charge force can be approximated by treating the cluster as a single, larger node.
                    ;; The theta parameter determines the accuracy of the approximation
                    (.theta 0.5)
                    ;; A positive value causes nodes to attract each other, similar to gravity,
                    ;; while a negative value causes nodes to repel each other, similar to electrostatic charge.
                    (.strength charge-strength)))
        (.force "collision"
                (-> (forceCollide)
                    (.radius (+ 8 18))
                    (.iterations 2)))
        (.force "x" (-> (forceX 0) (.strength 0.02)))
        (.force "y" (-> (forceY 0) (.strength 0.02)))
        (.force "center" (forceCenter))
        ;; The decay factor is akin to atmospheric friction; after the application of any forces during a tick,
        ;; each node’s velocity is multiplied by 1 - decay. As with lowering the alpha decay rate,
        ;; less velocity decay may converge on a better solution, but risks numerical instabilities and oscillation.
        (.velocityDecay 0.5))
    (reset! *simulation simulation)
    simulation))

(defn- clear-nodes!
  [graph]
  (.forEachNode graph
                (fn [node]
                  (.dropNode graph node))))

;; (defn- clear-edges!
;;   [graph]
;;   (.forEachEdge graph
;;                 (fn [edge]
;;                   (.dropEdge graph edge))))

(defn destroy-instance!
  []
  (when-let [instance (:pixi @*graph-instance)]
    (.destroy instance)
    (reset! *graph-instance nil)
    (reset! *simulation nil))
  (reset! *simulation-paused? false))

(defn stop-simulation!
  []
  (when-let [^js simulation @*simulation]
    (.stop simulation)
    (reset! *simulation-paused? true)))

(defn resume-simulation!
  []
  (when-let [^js simulation @*simulation]
    (.restart simulation))
  (reset! *simulation-paused? false))

(defn- update-position!
  [node obj]
  (when node
    (try
      (.updatePosition node #js {:x (.-x obj)
                                 :y (.-y obj)})
      (catch :default e
        (js/console.error e)))))

(defn- tick!
  [pixi _graph nodes-js links-js]
  (fn []
    (try
      (let [nodes-objects (.getNodesObjects pixi)
            edges-objects (.getEdgesObjects pixi)]
        (doseq [node nodes-js]
          (when-let [node-object (.get nodes-objects (.-id node))]
            (update-position! node-object node)))
        (doseq [edge links-js]
          (when-let [edge-object (.get edges-objects (str (.-index edge)))]
            (.updatePosition edge-object
                             #js {:x (.-x (.-source edge))
                                  :y (.-y (.-source edge))}
                             #js {:x (.-x (.-target edge))
                                  :y (.-y (.-target edge))}))))
      (catch :default e
        (js/console.error e)
        nil))))

(defn- set-up-listeners!
  [pixi-graph]
  (when pixi-graph
    ;; drag start
    (let [*dragging? (atom false)
          nodes (.getNodesObjects pixi-graph)
          on-drag-end (fn [_node event]
                        (.stopPropagation event)
                        (when-let [s @*simulation]
                          (when-not (.-active event)
                            (.alphaTarget s 0)))
                        (reset! *dragging? false))]
      (.on pixi-graph "nodeMousedown"
           (fn [event node-key]
             #_:clj-kondo/ignore
             (when-let [node (.get nodes node-key)]
               (when-let [s @*simulation]
                 (when-not (or (.-active event)
                               @*simulation-paused?)
                   (-> (.alphaTarget s 0.3)
                       (.restart))
                   (js/setTimeout #(.alphaTarget s 0) 2000))
                 (reset! *dragging? true)))))

      (.on pixi-graph "nodeMouseup"
           (fn [event node-key]
             (when-let [node (.get nodes node-key)]
               (on-drag-end node event))))

      (.on pixi-graph "nodeMousemove"
           (fn [event node-key]
             (when-let [node (.get nodes node-key)]
               (when @*dragging?
                 (update-position! node event))))))))

(defn render!
  [state]
  (try
    (when @*graph-instance
      (clear-nodes! (:graph @*graph-instance))
      (destroy-instance!))
    (let [{:keys [nodes links style hover-style height register-handlers-fn dark? link-dist charge-strength charge-range]} (first (:rum/args state))
          style       (or style (default-style dark?))
          hover-style (or hover-style (default-hover-style dark?))
          graph       (Graph.)
          nodes-set   (set (map :id nodes))
          links       (->>
                       (filter
                        (fn [link]
                          (and (nodes-set (:source link)) (nodes-set (:target link))))
                        links)
                       (distinct)) ;; #3331 (@zhaohui0923) seems caused by duplicated links. Why distinct doesn't work?
          nodes       (remove nil? nodes)
          links       (remove (fn [{:keys [source target]}] (or (nil? source) (nil? target))) links)
          nodes-js    (bean/->js nodes)
          links-js    (bean/->js links)
          simulation  (layout! nodes-js links-js link-dist charge-strength charge-range)]
      (doseq [node nodes-js]
        (try (.addNode graph (.-id node) node)
             (catch :default e
               (js/console.error e))))
      (doseq [link links-js]
        (let [source (.-id (.-source link))
              target (.-id (.-target link))]
          (try (.addEdge graph source target link)
               (catch :default e
                 (js/console.error e)))))
      (when-let [container-ref (:ref state)]
        (let [pixi-graph (new (.-PixiGraph Pixi-Graph)
                              (bean/->js
                               {:container  @container-ref
                                :graph      graph
                                :style      style
                                :hoverStyle hover-style
                                :height     height}))]
          (reset! *graph-instance
                  {:graph graph
                   :pixi  pixi-graph})
          (when register-handlers-fn
            (register-handlers-fn pixi-graph))
          (set-up-listeners! pixi-graph)
          (.on simulation "tick" (tick! pixi-graph graph nodes-js links-js))
          (doseq [node nodes]
            (when-let [icon (normalize-icon (:icon node))]
              (attach-icon-sprite! pixi-graph (:id node) icon
                                   (or (:size node) 8) (:color node) dark?))))))
    (catch :default e
      (js/console.error e)))
  state)
