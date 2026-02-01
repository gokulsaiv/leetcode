# LLD Design Patterns — Shape-Based Revision Guide (Java + Python)

Goal: Rapid recall of implementation SHAPE during interviews.
Focus: Interfaces, composition, delegation, flow of control.

============================================================
COMMAND PATTERN
Encapsulate a request as an object.
Shape memory: Invoker → Command → Receiver

------------------------------------------------------------
Java
------------------------------------------------------------

interface Command {
    void execute();
}

class Light {
    void on() { System.out.println("Light ON"); }
    void off() { System.out.println("Light OFF"); }
}

// Concrete Commands wrap receiver calls
class LightOnCommand implements Command {
    private Light light;
    LightOnCommand(Light light) { this.light = light; }

    public void execute() {
        light.on();
    }
}

class LightOffCommand implements Command {
    private Light light;
    LightOffCommand(Light light) { this.light = light; }

    public void execute() {
        light.off();
    }
}

// Invoker knows only Command
class RemoteControl {
    private Command slot;

    void setCommand(Command cmd) {
        this.slot = cmd;
    }

    void pressButton() {
        slot.execute();
    }
}

------------------------------------------------------------
Python
------------------------------------------------------------

class Command:
    def execute(self):
        pass

class Light:
    def on(self):
        print("Light ON")

    def off(self):
        print("Light OFF")

class LightOnCommand(Command):
    def __init__(self, light):
        self.light = light

    def execute(self):
        self.light.on()

class Remote:
    def __init__(self):
        self.cmd = None

    def set_command(self, cmd):
        self.cmd = cmd

    def press(self):
        self.cmd.execute()

============================================================
STATE PATTERN
Object changes behavior when internal state changes.
Shape memory: Context HAS-A State, state swaps itself.

------------------------------------------------------------
Java
------------------------------------------------------------

interface State {
    void handle(Context ctx);
}

class Context {
    private State state;

    Context(State init) {
        this.state = init;
    }

    void setState(State s) {
        this.state = s;
    }

    void request() {
        state.handle(this);
    }
}

class LockedState implements State {
    public void handle(Context ctx) {
        System.out.println("Unlocking...");
        ctx.setState(new UnlockedState());
    }
}

class UnlockedState implements State {
    public void handle(Context ctx) {
        System.out.println("Locking...");
        ctx.setState(new LockedState());
    }
}

------------------------------------------------------------
Python
------------------------------------------------------------

class State:
    def handle(self, ctx):
        pass

class Context:
    def __init__(self, state):
        self.state = state

    def request(self):
        self.state.handle(self)

class Locked(State):
    def handle(self, ctx):
        print("Unlocking")
        ctx.state = Unlocked()

class Unlocked(State):
    def handle(self, ctx):
        print("Locking")
        ctx.state = Locked()

============================================================
CHAIN OF RESPONSIBILITY (CoR)
Pass request along handler chain.
Shape memory: Handler → next → next → next

------------------------------------------------------------
Java
------------------------------------------------------------

abstract class Handler {
    protected Handler next;

    Handler setNext(Handler h) {
        next = h;
        return h;
    }

    abstract void handle(int req);
}

class Level1 extends Handler {
    void handle(int req) {
        if (req < 10) {
            System.out.println("Level1 handled");
        } else if (next != null) {
            next.handle(req);
        }
    }
}

class Level2 extends Handler {
    void handle(int req) {
        if (req < 100) {
            System.out.println("Level2 handled");
        } else if (next != null) {
            next.handle(req);
        }
    }
}

------------------------------------------------------------
Python
------------------------------------------------------------

class Handler:
    def __init__(self):
        self.next = None

    def set_next(self, h):
        self.next = h
        return h

    def handle(self, req):
        if self.next:
            self.next.handle(req)

class Level1(Handler):
    def handle(self, req):
        if req < 10:
            print("Level1 handled")
        else:
            super().handle(req)

============================================================
FACTORY PATTERN
Central object creation logic.
Shape memory: Static creator method → returns interface type.

------------------------------------------------------------
Java
------------------------------------------------------------

interface Vehicle {
    void move();
}

class Car implements Vehicle {
    public void move() {
        System.out.println("Car moving");
    }
}

class Bike implements Vehicle {
    public void move() {
        System.out.println("Bike moving");
    }
}

class VehicleFactory {
    static Vehicle create(String type) {
        switch(type) {
            case "car": return new Car();
            case "bike": return new Bike();
            default: throw new IllegalArgumentException();
        }
    }
}

------------------------------------------------------------
Python
------------------------------------------------------------

class Car:
    def move(self):
        print("Car moving")

class Bike:
    def move(self):
        print("Bike moving")

class VehicleFactory:
    @staticmethod
    def create(t):
        if t == "car":
            return Car()
        if t == "bike":
            return Bike()
        raise ValueError()

============================================================
STRATEGY PATTERN
Swap algorithms at runtime.
Shape memory: Context HAS-A Strategy.

------------------------------------------------------------
Java
------------------------------------------------------------

interface PricingStrategy {
    int price(int base);
}

class NormalPricing implements PricingStrategy {
    public int price(int base) { return base; }
}

class DiscountPricing implements PricingStrategy {
    public int price(int base) { return base - 20; }
}

class Checkout {
    private PricingStrategy strategy;

    Checkout(PricingStrategy s) {
        strategy = s;
    }

    int total(int base) {
        return strategy.price(base);
    }
}

------------------------------------------------------------
Python
------------------------------------------------------------

class Strategy:
    def price(self, base):
        pass

class Discount(Strategy):
    def price(self, base):
        return base - 20

class Checkout:
    def __init__(self, strategy):
        self.strategy = strategy

    def total(self, base):
        return self.strategy.price(base)

============================================================
OBSERVER PATTERN
Publish → notify subscribers.
Shape memory: Subject holds LIST of observers.

------------------------------------------------------------
Java
------------------------------------------------------------

import java.util.*;

interface Observer {
    void update(String msg);
}

class Subject {
    private List<Observer> observers = new ArrayList<>();

    void subscribe(Observer o) {
        observers.add(o);
    }

    void notifyAllObs(String msg) {
        for (Observer o : observers) {
            o.update(msg);
        }
    }
}

class User implements Observer {
    public void update(String msg) {
        System.out.println("Received: " + msg);
    }
}

------------------------------------------------------------
Python
------------------------------------------------------------

class Subject:
    def __init__(self):
        self.observers = []

    def subscribe(self, o):
        self.observers.append(o)

    def notify(self, msg):
        for o in self.observers:
            o.update(msg)

class User:
    def update(self, msg):
        print("Received:", msg)

============================================================
DECORATOR PATTERN
Wrap object to add behavior.
Shape memory: Decorator IMPLEMENTS same interface + HAS wrapped object.

------------------------------------------------------------
Java
------------------------------------------------------------

interface Notifier {
    void send();
}

class EmailNotifier implements Notifier {
    public void send() {
        System.out.println("Email sent");
    }
}

abstract class NotifierDecorator implements Notifier {
    protected Notifier wrappee;

    NotifierDecorator(Notifier n) {
        wrappee = n;
    }
}

class SMSDecorator extends NotifierDecorator {
    SMSDecorator(Notifier n) { super(n); }

    public void send() {
        wrappee.send();
        System.out.println("SMS sent");
    }
}

------------------------------------------------------------
Python
------------------------------------------------------------

class Notifier:
    def send(self):
        print("Email sent")

class Decorator(Notifier):
    def __init__(self, notifier):
        self.notifier = notifier

class SMSDecorator(Decorator):
    def send(self):
        self.notifier.send()
        print("SMS sent")

============================================================
END OF FILE
