# OOP Code Shapes — Quick Structure Reference (Java → Python)

Purpose: Fast recall of OOP syntax shapes. Minimal, crisp, working examples.  
Use this as a structure template during interviews or coding.

---

# ========================
# JAVA — OOP SHAPES
# ========================

## 1️⃣ Enum — basic shape

```java
enum Status {
    NEW,
    RUNNING,
    DONE
}
```

Enum with fields + constructor + method:

```java
enum Direction {
    NORTH(0, -1),
    EAST(1, 0);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int dx() { return dx; }
}
```

---

## 2️⃣ Simple Class — base shape

```java
class User {
    private String name;

    public User(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
```

---

## 3️⃣ Child Class (Inheritance) — extends

```java
class AdminUser extends User {

    public AdminUser(String name) {
        super(name);
    }

    public void banUser() {
        System.out.println("Ban user");
    }
}
```

Override method:

```java
@Override
public String getName() {
    return "Admin: " + super.getName();
}
```

---

## 4️⃣ Interface — shape

```java
interface Payment {
    void pay(int amount);
}
```

Implementation class:

```java
class CardPayment implements Payment {
    @Override
    public void pay(int amount) {
        System.out.println("Paid by card: " + amount);
    }
}
```

Multiple interfaces:

```java
class SmartCardPayment implements Payment, Runnable {
    public void pay(int amount) { }
    public void run() { }
}
```

Interface with default method:

```java
interface Logger {
    default void log(String s) {
        System.out.println(s);
    }
}
```

---

## 5️⃣ Abstract Class — shape

```java
abstract class Vehicle {

    protected String model;

    public Vehicle(String model) {
        this.model = model;
    }

    public abstract void start();   // must implement

    public void stop() {            // already implemented
        System.out.println("Stopped");
    }
}
```

Concrete child:

```java
class Car extends Vehicle {

    public Car(String model) {
        super(model);
    }

    @Override
    public void start() {
        System.out.println("Car starts");
    }
}
```

---

## 6️⃣ Using Everything Together — quick demo

```java
public class Main {
    public static void main(String[] args) {

        Vehicle v = new Car("Tesla");
        v.start();
        v.stop();

        Payment p = new CardPayment();
        p.pay(100);

        User u = new AdminUser("Alice");
        System.out.println(u.getName());
    }
}
```

---

# ========================
# PYTHON — OOP SHAPES
# ========================

## 1️⃣ Enum — shape

```python
from enum import Enum

class Status(Enum):
    NEW = 1
    RUNNING = 2
    DONE = 3
```

Enum with method:

```python
class Direction(Enum):
    NORTH = (0, -1)

    def dx(self):
        return self.value[0]
```

---

## 2️⃣ Simple Class — shape

```python
class User:
    def __init__(self, name):
        self.name = name

    def get_name(self):
        return self.name
```

---

## 3️⃣ Child Class — inheritance

```python
class AdminUser(User):

    def __init__(self, name):
        super().__init__(name)

    def ban_user(self):
        print("Ban user")
```

Override method:

```python
def get_name(self):
    return "Admin: " + super().get_name()
```

---

## 4️⃣ Interface Equivalent — using ABC

Python has no true interface → use abstract base class.

```python
from abc import ABC, abstractmethod

class Payment(ABC):

    @abstractmethod
    def pay(self, amount):
        pass
```

Implementation:

```python
class CardPayment(Payment):

    def pay(self, amount):
        print("Paid by card:", amount)
```

---

## 5️⃣ Abstract Class — shape

```python
from abc import ABC, abstractmethod

class Vehicle(ABC):

    def __init__(self, model):
        self.model = model

    @abstractmethod
    def start(self):
        pass

    def stop(self):
        print("Stopped")
```

Child implementation:

```python
class Car(Vehicle):

    def start(self):
        print("Car starts")
```

---

## 6️⃣ Using Everything — demo

```python
if __name__ == "__main__":

    v = Car("Tesla")
    v.start()
    v.stop()

    p = CardPayment()
    p.pay(100)

    u = AdminUser("Alice")
    print(u.get_name())
```

---

# ========================
# Mental Model Summary
# ========================

Java:
- class → `class A {}`
- child → `class B extends A`
- interface → `interface X {}`
- implement → `class C implements X`
- abstract → `abstract class A`
- enum → `enum X {}`

Python:
- class → `class A:`
- child → `class B(A):`
- interface → `ABC + @abstractmethod`
- abstract → `ABC`
- enum → `Enum`

Use this file as your quick OOP syntax skeleton reference.
```
