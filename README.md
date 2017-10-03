# src-munch

A set of tools used to unobtrusively consume development artifacts and
provide them as clojure edn data. The primary purpose for this is to
make it easy to create our gallery.

The gallery features colors and icons. Rather than hand-code the
display of each color and icon, this library parses the definitions of
our colors and icons, and produces data that the gallery-generating
scripts in Salk can use.

## Install

```bash
brew install lumo
npm install
```

## Play

### Launch lumo repl (with access to reifyhealth.src-munch code)

```
lumo --classpath src
```

### Require src-munch code (from lumo repl)

```
(require '[reifyhealth.src-munch.util])

(reifyhealth.src-munch.util/slurp "README.md")

```

### Require an npm library

```
(require '[goog.object :as go])

(def fs (js/require "fs"))
(.readFileSync fs "README.md")

(def cp (js/require "child_process"))
(def spawnSync (go/get cp "spawnSync"))
(spawnSync "ls" (clj->js ["-la"]))
```

### TODO: Explain what the parsers do...
### TODO: Show an example of how to use parsers that is generic...
