```markdown
# Low-Level Design (LLD) Interview — Complete Practical Approach Guide (Single-File Reference)

This file is a single, continuous reference for how to approach a Low-Level Design (LLD) interview step by step. It is designed as a memory guide you can revise before interviews. It focuses on object-oriented design interviews (Amazon / Microsoft / Netflix / product companies / India & Asia machine coding rounds). The emphasis is on order of thinking, structure, pseudo code flow, responsibility split, and interview behavior — not perfect syntax and not overengineering with patterns.

The core philosophy: clarify → model → design → implement → defend → extend. Never jump directly into coding.

---

# Master Interview Flow — Always Follow This Order

1. Clarify Requirements  
2. Confirm Scope Boundaries  
3. Write Requirement Summary  
4. Identify Entities (Noun Extraction)  
5. Define Class Responsibilities (SRP split)  
6. Design Classes (Fields + Methods)  
7. Define State Models (prefer enums)  
8. Design Method Contracts  
9. Validate Responsibility Separation  
10. Ask Pseudo vs Real Code  
11. Implement Core Methods in Pseudo Code  
12. Handle Edge Cases Explicitly  
13. Justify Tradeoffs  
14. Avoid Pattern Overengineering  
15. Handle Follow-up Extensibility Questions  

Memorize this order. Interviewers score structure heavily.

---

# Step 1 — Clarify Requirements First (Never Start Coding Immediately)

When the prompt is given, pause and ask clarification questions. You are being evaluated on problem analysis, not speed typing.

Ask questions in three buckets.

Primary Capabilities:
- What actions are supported?
- How do users interact?
- What are valid operations?
- What are end conditions?
- What defines success?

Error Handling:
- What is invalid input?
- What happens on invalid move?
- Return false, throw error, or ignore?
- Should state remain unchanged on error?

Scope Boundaries:
- Single instance or multiple?
- UI in scope or backend only?
- Persistence required?
- Concurrency required?
- Network involved or in-memory only?

Say out loud what you are clarifying. That signals design maturity.

---

# Step 2 — Write Final Requirement Summary On Screen

Before designing anything, restate requirements in bullet form:

Include:
- Main operations
- Constraints
- Win/end rules
- Error rules
- Out-of-scope items

Write “Out of Scope” explicitly. This prevents scope creep and shows discipline.

---

# Step 3 — Identify Entities Using Noun Extraction

Scan the requirements and extract nouns. Nouns usually become classes.

Examples:
Game, Board, Player, Ticket, Order, Account, Slot, Payment, Cart, Session.

List them first. Do not refine yet. Just capture candidates.

This is your entity pool.

---

# Step 4 — Define Responsibilities (Single Responsibility Principle)

Each class should have one clear job.

Ask:
“If requirement X changes, which class changes?”

If answer = multiple classes → responsibility leak.

Example separation pattern:
Game → orchestration and rules  
Board → grid/state mechanics  
Player → identity data  

Avoid mixing orchestration + storage + validation in one class.

You can say:
“I want each class to have a single responsibility so future changes are localized.”

Interviewers like hearing that.

---

# Step 5 — Design Classes Top-Down

Always design orchestrator first.

Order:
1. Main orchestrator (Game / Service / Manager)
2. Core state holders
3. Simple data classes

For each class define two things only:

Fields (state):
What must this class remember?

Methods (behavior):
What must this class do?

Both must come directly from requirements — not imagination.

---

# Step 6 — Prefer Enums Over Multiple Boolean Flags

Never model multi-state systems with multiple booleans.

Bad model:
isOver, hasWinner, isDraw

This creates invalid combinations.

Good model:
enum GameState = IN_PROGRESS, WON, DRAW

You can say:
“Enums prevent invalid state combinations.”

That is a strong design signal.

---

# Step 7 — Derive Methods Directly From Requirements

Each requirement should map to a method.

Examples:
“drop disc” → makeMove()  
“check winner” → checkWin()  
“validate column” → canPlace()  
“board full” → isFull()

Public methods = requirement-driven.  
Private methods = helpers only.

Expose minimal public API.

---

# Step 8 — Validate Responsibility Boundaries

Check that:
Orchestrator does orchestration only  
State classes handle state only  
Validation lives where data lives  

Example:
Game should not scan grid internals  
Game should call Board.checkWin()

Say:
“I want to keep grid logic encapsulated inside Board.”

Encapsulation language scores points.

---

# Step 9 — UML Is Optional, Simple Class Notation Is Fine

Modern interviews do not require strict UML symbols.

Draw simple boxes:

ClassName
- fields
+ methods

If asked about UML:
Offer simplified class notation. That is usually accepted.

---

# Step 10 — Ask Before Implementation Detail Level

Ask interviewer:
“Do you want pseudo code or real code?”

Most LLD interviews prefer pseudo code.

Do not waste time on syntax perfection.

---

# Step 11 — Implement Only Core Methods

You do not need to implement everything.

Usually implement:
- main action method
- state update method
- validation method

Example trio:
makeMove  
placeItem  
checkCondition  

State helpers can remain stubbed.

---

# Step 12 — Pseudo Code Writing Template

Use consistent structure:

function X(input):

  # core logic steps
  step 1
  step 2
  step 3

  # edge cases
  if invalid → return error

  # state updates

  return result

Say:
“I’ll implement happy path first, then edge cases.”

Interviewers like this ordering.

---

# Step 13 — Always Call Out Edge Cases Explicitly

Always speak edge cases aloud.

Typical edge cases:
- invalid index
- wrong turn
- full capacity
- state already finished
- null input
- duplicate action

Even if trivial — mention them. That shows defensive design thinking.

---

# Step 14 — Use Helper Methods Freely

Break complex logic into helpers.

Examples:
countDirection  
isValidCell  
nextFreeRow  
validateMove  

Say:
“I’ll extract this into a helper for clarity.”

Shows decomposition skill.

---

# Step 15 — Discuss Return Type Tradeoffs

Mention options:

boolean  
error object  
status enum  
exception  

Say:
“This depends on caller contract — I’d confirm expected API behavior.”

That shows interface awareness.

---

# Step 16 — Avoid Pattern Overengineering

Do NOT inject patterns just to show knowledge.

Do NOT force:
Strategy  
Factory  
Visitor  
Decorator  

Unless variability truly exists.

Strong interview answer:
“I could use Strategy pattern, but behaviors are fixed and similar, so it would add complexity without benefit.”

That sounds senior.

---

# Step 17 — Tradeoff Language You Should Use

Use phrases like:
simpler design wins here  
pattern adds no value  
fixed behavior doesn’t justify abstraction  
avoid premature abstraction  
keep it simple and extensible  

This is high-signal reasoning.

---

# Step 18 — Extensibility Follow-Up Questions (Very Common)

Expect follow-ups like:

Change board size?
Answer: make dimensions constructor parameters.

Add undo?
Answer: add move history stack in orchestrator.

Add bot player?
Answer: separate decision engine, don’t modify core rules.

Multiple games?
Answer: add GameManager wrapper.

Persistence?
Answer: add repository layer, keep domain classes unchanged.

---

# Step 19 — Extensibility Golden Rule

Extend by adding components — not rewriting core classes.

Say:
“I want to extend behavior without modifying stable core logic.”

Interviewers love this answer.

---

# Step 20 — Concurrency Only If Prompt Mentions It

Do not add locks or threads unless required.

If mentioned:
shared state → lock  
producer-consumer → blocking queue  
resource limit → semaphore  

Otherwise assume single-threaded.

---

# Step 21 — Strong Verbal Signals To Use Naturally

Use these phrases during explanation:

single responsibility  
encapsulation  
edge cases  
happy path first  
constructor injection  
enum state model  
bounded scope  
avoid overengineering  
requirement-driven design  
extensible structure  

These are high-signal interview phrases.

---

# Step 22 — Final Mental Checklist Before You Say “Done”

Before finishing, check:

Did I clarify requirements?  
Did I define scope?  
Did I list entities?  
Did I split responsibilities?  
Did I define fields and methods?  
Did I model state with enum?  
Did I implement core methods?  
Did I mention edge cases?  
Did I justify tradeoffs?  
Did I avoid pattern stuffing?  
Did I answer extensibility questions cleanly?

If yes — you delivered a strong LLD round.

---

# Core Principle To Remember

LLD interviews reward:

clarity over cleverness  
structure over syntax  
reasoning over patterns  
simplicity over abstraction gymnastics  

Design like a calm engineer. Explain like a system builder. That wins rounds.

End of file.
```
