# Crazy Putting — Golf Physics Simulator with ODE Solvers, Bots and A* (Java)

A mini-golf ("crazy putting") simulator built on a from-scratch ODE solver framework. The ball's equations of motion on a height-function terrain are integrated with Runge–Kutta 4, and bots find shots that sink the ball, including on maze-like courses using A* pathfinding. A JavaFX GUI lets you play shots manually or watch the bots.

> Team project (Team 16) for **Project 1-2**, BSc Data Science & AI, Maastricht University (2026). Originally developed on the university GitLab; this is a cleaned snapshot of the final version.

## My contributions

- Built the JavaFX GUI: course field rendering, sidebar parameter input with exception handling, putt button wired to the equations of motion, shot counter, chart styling, and a render cache to improve performance
- Added terrain features to the physics: sandpits and trees in the course model, location-dependent friction (sand vs grass) in the equations of motion, and an early stop when the ball's velocity becomes negligible
- Wrote the solver accuracy experiment output, ODE solver tests and the test courses (including water courses)
- Integrated teammates' branches (ML bot, bot comparison, EOM, course input) into `main`

## Team

Minseok Choi, Mohamed Keita, Yuri Makarevich, Nikita Kirillov, Manos Chaloftidis and Andrei.
<!-- TODO(Minseok): add any missing teammates' full names -->

The bots (`MLBot`, `MazeMLBot`, `RuleBasedBot`), A* pathfinding, the RK4 solver and the bot comparison experiments were built mainly by teammates.

## Overview

Our project implements a modular, object‑oriented framework for solving Ordinary Differential Equations (ODEs) in Java, then uses that framework for a Crazy Putting golf simulation. It includes:

- A generic interface for ODE systems (`ODESystem`)
- An abstract base class for ODE solvers (`ODESolver`)
- Two concrete solvers:
  - Euler Method (first‑order)
  - Runge–Kutta 4 (fourth‑order)
- A benchmarking tool (`SolverAccuracy`) for absolute final-time error and runtime evaluation
- Course loading, height parsing, sandpits, trees, and ball motion physics
- Bot players, including `RuleBasedBot`, `MLBot`, and `MazeMLBot`
- A* pathfinding utilities for maze-like courses
- A JavaFX GUI for running the putting simulation and comparing shots
---

## 📁 Project Structure
```
./
├── pom.xml
├── src/main/java/
│   ├── core/
│   │   ├── ODESolver.java
│   │   ├── ODESystem.java
│   │   ├── Vector2D.java
│   │   ├── bots/
│   │   │   ├── Bot.java
│   │   │   ├── MLBot.java
│   │   │   ├── MLBotPrevious.java
│   │   │   ├── MazeMLBot.java
│   │   │   └── RuleBasedBot.java
│   │   ├── course/
│   │   │   ├── Course.java
│   │   │   ├── CourseFileReader.java
│   │   │   ├── CourseParser.java
│   │   │   ├── HeightFunction.java
│   │   │   ├── HeightParser.java
│   │   │   ├── HeightTokenizer.java
│   │   │   └── PartialDerivative.java
│   │   └── pathfinding/
│   │       ├── AStarPathfinder.java
│   │       ├── GridMap.java
│   │       ├── PathSimplifier.java
│   │       └── WaypointSelector.java
│   ├── experiments/
│   │   ├── BotComparison.java
│   │   ├── BotComparisonMain.java
│   │   ├── HeightFunctionTest.java
│   │   ├── SolverAccuracy.java
│   │   └── UnitTest.java
│   ├── gui/
│   │   ├── ChartPanel.java
│   │   ├── MainApp.java
│   │   ├── SidebarPanel.java
│   │   ├── SimulationEngine.java
│   │   ├── SimulationResult.java
│   │   ├── golf/
│   │   │   ├── GolfGameState.java
│   │   │   ├── GolfShotAnimator.java
│   │   │   └── GolfShotSimulator.java
│   │   └── rendering/
│   │       └── GolfCourseRenderer.java
│   ├── solvers/
│   │   ├── EulerSolver.java
│   │   └── RungeKutta4.java
│   └── systems/
│       ├── BallMotionSystem.java
│       ├── ExponentialSystem.java
│       ├── FitzHughNagumo.java
│       ├── LotkaVolterra.java
│       └── SIR.java
└── src/main/resources/
    ├── courses/
    │   ├── test_course.txt
    │   ├── test_course2.txt
    │   ├── test_course3.txt
    │   ├── test_course4.txt
    │   ├── test_course5.txt
    │   ├── test_course6.txt
    │   ├── test_course7.txt
    │   └── demo and maze courses
    └── style.css
```

---

## 🚀 Features
###  ODESystem Interface
Represents any ODE system of the form `dy/dt = f(t, y)`.

###  ODESolver Base Class
Defines the step size and the interface `nextStep(...)`.

###  EulerSolver
Implements the explicit Euler method and includes a `solve(...)` helper.

###  RungeKutta4
Implements the classical RK4 method using 4 internal slope evaluations.

###  SolverAccuracy
Runs benchmarks on the ODE `y' = y` to evaluate the absolute error at the final time and the runtime of the solvers.

###  System Models
Includes reusable ODE systems such as Lotka-Volterra, FitzHugh-Nagumo, and SIR, each implemented independently of the solvers.

###  Crazy Putting Simulation
Loads course files, parses height functions, simulates the ball with RK4 physics, and supports water, tree, and sandpit effects.

###  Bots and Maze Pathfinding
Includes a rule-based bot, an ML-style local search bot, and `MazeMLBot`, which combines A* grid planning with MLBot shot optimization for maze-like courses.

###  JavaFX GUI
Provides the main course simulation interface (`MainApp`) with manual shots, bot shots, course rendering, and shot statistics.

---

##  Mathematical Summary
### **Euler Method**
Uses the update
`x(t + h) = x(t) + h * f(t, x(t))`
and is a first-order method.

### **Runge–Kutta 4**
Uses four slope evaluations per step and combines them into a weighted average for higher accuracy.

---

##  How to Compile & Run
Requires Java 21 and Maven. Launch the GUI:

```bash
mvn clean javafx:run
```

Run the unit tests:
```bash
mvn clean test
```

Run solverAccuracy.java to see the text results or run this command:
```bash
mvn exec:java -Dexec.mainClass="experiments.SolverAccuracy"
```

Run the bot comparison experiment:
```bash
mvn exec:java -Dexec.mainClass="experiments.BotComparisonMain"
```
---

##  UML Diagram (ASCII)
```
                         +------------------+
                         |    ODESystem     |
                         +------------------+
                         | + getDerivatives |
                         | + getVariables   |
                         | + getParameters  |
                         | + setParameter   |
                         +------------------+
                                     ▲
                                     |
                         +----------------------+
                         |     ODESolver        |
                         +----------------------+
                         | - stepSize: double   |
                         | + nextStep()         |
                         | + get/set stepSize   |
                         +----------------------+
                             ▲                ▲
                             |                |
                 +----------------+   +---------------------+
                 |  EulerSolver   |   |   RungeKutta4       |
                 +----------------+   +---------------------+
                 | + nextStep()   |   | + nextStep()        |
                 | + solve()      |   |                     |
                 +----------------+   +---------------------+

                             +----------------------+
                             |   SolverAccuracy     |
                             +----------------------+
                             | + oneExperiment()    |
                             | + allExperiments()   |
                             | + printResults()     |
                             | + filterBySolver()   |
                             +----------------------+
```
