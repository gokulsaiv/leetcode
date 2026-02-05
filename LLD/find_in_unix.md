# Unix `find` — LLD Design in Python (Using `os.walk`)

This is an interview‑ready Low Level Design (LLD) for a simplified Unix `find` command implemented in Python using the `os` module. The design focuses on:

* Clean separation of traversal and filtering
* Extensible filter system
* Support for multiple filters
* AND / OR / NOT logical composition
* Easy explanation in interviews

---

# ✅ Design Goals

We want to:

* Traverse directory trees
* Apply multiple filters to files/directories
* Support AND / OR combinations
* Avoid changing engine logic when new filters are added

Key idea: **Filters are objects**. Logical operators are also filters.

This follows the Specification + Composite pattern style.

---

# 📁 Traversal Engine (uses os.walk)

We use `os.walk` for recursive traversal. It yields:

* `dirpath` → current directory path
* `dirnames` → list of subdirectory names
* `filenames` → list of file names

Engine only handles traversal — not filtering logic.

```python
import os
from abc import ABC, abstractmethod


class Filter(ABC):
    @abstractmethod
    def matches(self, path: str) -> bool:
        pass


class FindEngine:

    def find(self, root_dir: str, flt: Filter):
        results = []

        for dirpath, dirnames, filenames in os.walk(root_dir):
            # check directories
            for d in dirnames:
                full = os.path.join(dirpath, d)
                if flt.matches(full):
                    results.append(full)

            # check files
            for f in filenames:
                full = os.path.join(dirpath, f)
                if flt.matches(full):
                    results.append(full)

        return results
```

---

# 🔍 Basic Filters

Each filter checks one property.

## Name contains

```python
class NameContains(Filter):
    def __init__(self, text):
        self.text = text

    def matches(self, path):
        return self.text in os.path.basename(path)
```

## Extension filter

```python
class ExtensionIs(Filter):
    def __init__(self, ext):
        self.ext = ext

    def matches(self, path):
        return path.endswith(self.ext)
```

## Size filter

```python
class SizeGreaterThan(Filter):
    def __init__(self, size):
        self.size = size

    def matches(self, path):
        try:
            return os.path.getsize(path) > self.size
        except:
            return False
```

## Type filters

```python
class IsFile(Filter):
    def matches(self, path):
        return os.path.isfile(path)


class IsDirectory(Filter):
    def matches(self, path):
        return os.path.isdir(path)
```

---

# 🧠 Logical Combinator Filters

These enable multiple filters without changing the engine.

## AND filter

```python
class And(Filter):
    def __init__(self, *filters):
        self.filters = filters

    def matches(self, path):
        for f in self.filters:
            if not f.matches(path):
                return False
        return True
```

## OR filter

```python
class Or(Filter):
    def __init__(self, *filters):
        self.filters = filters

    def matches(self, path):
        for f in self.filters:
            if f.matches(path):
                return True
        return False
```

## NOT filter

```python
class Not(Filter):
    def __init__(self, flt):
        self.flt = flt

    def matches(self, path):
        return not self.flt.matches(path)
```

---

# 🔗 How Multiple Filters Work (Flow)

The engine receives **one filter object** — but that filter can be a tree of filters.

Example requirement:

Find files where:

* extension is `.py`
* size > 1KB
* name contains "test"

All must match → AND

```python
flt = And(
    ExtensionIs(".py"),
    SizeGreaterThan(1024),
    NameContains("test")
)

engine = FindEngine()
results = engine.find("/root", flt)
```

Evaluation per path:

```
And.matches(path)
 → check ExtensionIs
 → check SizeGreaterThan
 → check NameContains
 → all true → match
```

---

# 🔀 Mixed AND / OR Example

Requirement:

(.py AND size>1KB) OR name contains "config"

```python
flt = Or(
    And(
        ExtensionIs(".py"),
        SizeGreaterThan(1024)
    ),
    NameContains("config")
)
```

Filter tree:

```
        OR
      /     \
    AND     NameContains
   /   \
 ext   size
```

Each file path is evaluated through this tree.

---

# 🧱 Dynamic Filter Lists (Interview Follow‑Up)

If filters come from user input:

```python
user_filters = [
    ExtensionIs(".py"),
    SizeGreaterThan(500)
]

flt = And(*user_filters)
```

No engine change required.

---

# ⚙️ Why This Design Scores Well in LLD Interviews

You can explain clearly:

* Traversal separated from filtering
* Open for new filters without modifying engine
* Logical composition via objects
* Short‑circuit evaluation in AND/OR
* Easily testable filters

Patterns involved (if interviewer asks):

* Specification Pattern → Filter interface
* Composite Pattern → And/Or trees

---

# 🚀 Possible Enhancements (Mention If Asked)

* Depth limit support
* Follow / skip symlinks
* Generator instead of list for large outputs
* BFS vs DFS traversal strategy
* Permission error handling strategy
* Parallel directory scanning

---

# ✅ Key Interview Line

"Traversal and filtering are decoupled. Filters are composable objects, so multiple AND/OR conditions are handled by building a filter tree instead of changing the engine."

That sentence alone has hired people.
