# OOP Patterns & Syntax Cheat-Sheet — Java first, then Python equivalents  
**Single-file reference**: enums, classes, inheritance (child classes), interfaces, abstract classes & implementations, extending, and small factory/usage examples. Copy this entire file as `oop_patterns.md` and use it as your concise syntax destination.

---

## Table of contents
1. Java — Quick explanations + runnable examples  
   1.1 Enum with methods  
   1.2 Plain class and fields (POJO)  
   1.3 Inheritance (base class → child class)  
   1.4 Interface and multiple-interface implementation  
   1.5 Abstract class and concrete implementation  
   1.6 Extending classes and overriding methods  
   1.7 Simple Factory pattern to create instances  
   1.8 Example `Main` showing usage  
2. Python — Equivalent patterns using `enum`, `abc`, dataclasses, and duck typing  
   2.1 Enum with methods (`enum.Enum`)  
   2.2 Plain class / dataclass  
   2.3 Inheritance (base → child)  
   2.4 Interface-like via `abc.ABC` or `typing.Protocol`  
   2.5 Abstract base class and implementation (`abc`)  
   2.6 Extending and overriding  
   2.7 Simple factory function  
   2.8 Example `if __name__ == "__main__"` showing usage  
3. Short comparison notes (Java vs Python)
4. Common gotchas & best practices

---

# 1) **JAVA** — examples & notes

> To compile/run: save as `Main.java` (or use the multi-file structure described in comments), then:
> ```bash
> javac Main.java
> java Main
> ```
> (If you split classes into files use each filename appropriately.)

```java
/* ===== File: Main.java =====
   This single-file example declares multiple classes (only Main is public).
   It demonstrates enums, classes, inheritance, interface, abstract class, factory, and usage.
*/
public class Main {
    public static void main(String[] args) {
        // Using enum
        Direction d = Direction.NORTH;
        System.out.println("Direction: " + d + " vector: " + d.getDx() + "," + d.getDy());

        // Using factory to create vehicles
        Vehicle car = VehicleFactory.create("car", "Toyota");
        Vehicle bike = VehicleFactory.create("bike", "Trek");

        car.start();
        bike.start();

        // Demonstrate polymorphism and interface
        Flyer falcon = new Bird("Falcon");
        falcon.fly();

        // Abstract class usage
        Animal dog = new Dog("Rex");
        dog.speak();
        dog.eat();

        // Extending / overriding example
        ElectricCar tesla = new ElectricCar("Tesla Model 3", 75);
        tesla.start();
        tesla.charge(10);
    }
}

/* ===== 1.1 Enum with methods ===== */
enum Direction {
    NORTH(0, -1),
    EAST(1, 0),
    SOUTH(0, 1),
    WEST(-1, 0);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int getDx() { return dx; }
    public int getDy() { return dy; }

    public Direction opposite() {
        switch(this) {
            case NORTH: return SOUTH;
            case SOUTH: return NORTH;
            case EAST:  return WEST;
            case WEST:  return EAST;
            default:    return null;
        }
    }
}

/* ===== 1.2 Plain class (POJO) ===== */
class Vehicle {
    protected String model;
    protected boolean running;

    public Vehicle(String model) {
        this.model = model;
        this.running = false;
    }

    public void start() {
        running = true;
        System.out.println(model + " started.");
    }

    public void stop() {
        running = false;
        System.out.println(model + " stopped.");
    }

    public String getModel() { return model; }
}

/* ===== 1.3 Inheritance (child class) ===== */
class Car extends Vehicle {
    public Car(String model) { super(model); }

    // override
    @Override
    public void start() {
        super.start();
        System.out.println("Car-specific start checks passed.");
    }
}

class Bike extends Vehicle {
    public Bike(String model) { super(model); }

    @Override
    public void start() {
        super.start();
        System.out.println("Bike bell rings!");
    }
}

/* ===== 1.4 Interface and implementing class ===== */
interface Flyer {
    void fly();          // implicitly public abstract
    default void land() {
        System.out.println("Landing (default implementation).");
    }
}

class Bird implements Flyer {
    private final String name;
    public Bird(String name) { this.name = name; }
    @Override
    public void fly() {
        System.out.println(name + " is flying.");
    }
    // land() uses default, override if needed
}

/* ===== 1.5 Abstract class and implementation ===== */
abstract class Animal {
    protected String name;
    public Animal(String name) { this.name = name; }

    // concrete method
    public void eat() { System.out.println(name + " eats."); }

    // abstract method (must be implemented by subclasses)
    public abstract void speak();
}

class Dog extends Animal {
    public Dog(String name) { super(name); }
    @Override
    public void speak() {
        System.out.println(name + " says: Woof!");
    }
}

/* ===== 1.6 Extending classes (specialization) ===== */
class ElectricCar extends Car {
    private int batteryKWh;
    public ElectricCar(String model, int batteryKWh) {
        super(model);
        this.batteryKWh = batteryKWh;
    }

    @Override
    public void start() {
        if (batteryKWh <= 0) {
            System.out.println(getModel() + " won't start — battery empty.");
        } else {
            super.start();
            System.out.println(getModel() + " is electric. Battery: " + batteryKWh + " kWh");
        }
    }

    public void charge(int kWh) {
        batteryKWh += kWh;
        System.out.println(getModel() + " charged by " + kWh + " kWh. Now: " + batteryKWh);
    }
}

/* ===== 1.7 Simple Factory pattern ===== */
class VehicleFactory {
    // factory method returns base type Vehicle
    public static Vehicle create(String type, String model) {
        switch(type.toLowerCase()) {
            case "car": return new Car(model);
            case "bike": return new Bike(model);
            case "electric": return new ElectricCar(model, 50);
            default: return new Vehicle(model);
        }
    }
}
