# Tic-Tac-Toe Game Engine — Complete LLD Design (Interview Revision File)

Purpose: Interview-ready low level design for a flexible Tic-Tac-Toe engine.
Supports:
- 2 players
- NxN board
- configurable win streak (K in a row)
- undo support
- clean separation of responsibilities
- extensible rule logic

============================================================

FUNCTIONAL REQUIREMENTS

- Game played on a board (default 3×3)
- Two players with symbols (X and O)
- Players take turns
- Each move places symbol at (row, col)
- Cell cannot be overwritten
- After every move check:
  - win
  - draw
  - continue
- Game stops on win or draw
- Status returned after each move
- Undo last successful move supported
- Board size configurable (NxN)
- Win streak configurable (K)

============================================================

CORE ENUMS

Symbol
- X
- O

GameState
- IN_PROGRESS
- DRAW
- COMPLETED

============================================================

PLAYER

Fields:
- name : String
- symbol : Symbol

Rules:
- Immutable after creation
- No setters during gameplay
- Symbol assigned via constructor

Responsibility:
- Identity + symbol ownership only

============================================================

MOVE

Represents one reversible move action.

Fields:
- row : int
- col : int
- player : Player
- symbol : Symbol

Purpose:
- Stored in undo stack
- Enables O(1) undo
- Supports move history
- Allows replay/inspection later

============================================================

BOARD

Pure storage + cell operations. No rule logic.

Fields:
- rows : int
- cols : int
- grid : Symbol[][]

Constructor:
Board(rows, cols)

Methods:

isValidCell(row,col) -> bool
isEmpty(row,col) -> bool

placeSymbol(row,col,symbol) -> bool
clearCell(row,col) -> void

getCell(row,col) -> Symbol

isFull() -> bool

Rules:
- Board does NOT know win rules
- Board does NOT track turns
- Board does NOT manage undo

============================================================

WINNING STRATEGY (Rule Abstraction)

Interface:
WinningStrategy

Fields:
- winLength : int   // K in a row

Method:
checkWin(Board board, Move lastMove) -> bool

Algorithm Shape:

Check only from last move position.

Directions:
- horizontal
- vertical
- diagonal ↘
- diagonal ↙

For each direction:
count forward + count backward − 1

If >= winLength → WIN

Time Complexity: O(K)

Purpose:
- Decouples rule logic from board
- Supports NxN + custom K
- Strategy pattern ready

============================================================

GAME (Main Orchestrator)

Owns flow, rules, state, undo, turn order.

Fields:

- board : Board
- players : List<Player> (size 2)
- currentPlayerIndex : int
- state : GameState
- winner : Player | null
- winStrategy : WinningStrategy
- moveStack : Stack<Move>

============================================================

GAME CONSTRUCTOR

Game(players, boardSize, winLength)

Steps:
- create board(boardSize, boardSize)
- create winStrategy(winLength)
- set players
- currentPlayerIndex = 0
- state = IN_PROGRESS
- winner = null
- moveStack empty

============================================================

MAKE MOVE FLOW

makeMove(row,col) -> GameState

Steps:

1. if state != IN_PROGRESS → reject
2. get current player
3. validate coordinates via board
4. validate cell empty
5. board.placeSymbol(row,col,player.symbol)
6. create Move(row,col,player,symbol)
7. push move into moveStack
8. if winStrategy.checkWin(board, move)
      winner = player
      state = COMPLETED
      return state
9. if board.isFull()
      state = DRAW
      return state
10. switchTurn()
11. return IN_PROGRESS

Only successful moves are recorded in stack.

============================================================

UNDO FLOW

undo() -> bool

Policy (state clearly in interview):
Either:
- allow undo always
OR
- disallow after terminal state

Steps:

1. if moveStack empty → return false
2. pop lastMove
3. board.clearCell(lastMove.row,lastMove.col)
4. currentPlayerIndex = indexOf(lastMove.player)
5. winner = null
6. state = IN_PROGRESS
7. return true

Time Complexity: O(1)

Key Point:
Undo reverses BOTH board and game state.

============================================================

TURN SWITCH

switchTurn()

currentPlayerIndex = (currentPlayerIndex + 1) % 2

============================================================

QUERY METHODS

getStatus() -> GameState
getWinner() -> Player | null
getCurrentPlayer() -> Player

============================================================

EDGE CASE HANDLING

- Move outside board → reject
- Move on filled cell → reject
- Move after game completed → reject
- Wrong player turn → reject
- Undo with empty history → reject
- Undo after win → allowed or blocked (declare policy)

============================================================

DESIGN JUSTIFICATIONS (INTERVIEW TALKING POINTS)

- Board only stores state → single responsibility
- Win logic separated via strategy → extensible
- Move object enables undo + history
- Undo uses stack → O(1)
- Win check from last move → O(K) not O(N²)
- Player immutable → prevents symbol mutation bugs
- Game owns turn + state transitions
- Supports NxN and K-streak without redesign

============================================================

OPTIONAL EXTENSIONS (IF ASKED)

- BotPlayer with move strategy
- Replay game from move history
- Command pattern for moves (execute/undo)
- Multiple win strategies
- Timed turns
- Score tracking

============================================================

COMPLEXITY SUMMARY

makeMove → O(K)
undo → O(1)
space → O(N² + moves)

============================================================

END OF FILE
