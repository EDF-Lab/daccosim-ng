# DACCOSIM NG (Distributed Architecture for Controlled CO-SIMulation)

[![License: LGPL v3](https://img.shields.io/badge/License-LGPL--3.0-blue.svg)](https://www.gnu.org/licenses/lgpl-3.0)

**DACCOSIM** is an open-source co-simulation environment designed for the simulation of complex, multi-physical, and distributed systems.

Initially initiated by **Électricité de France (EDF R&D)** and **CentraleSupélec** through the [RISEGrid](https://www.risegrid.fr/) institute, DACCOSIM is developed in collaboration with **EIFER** (Germany), **SIANI Institute** (Spain), and **Monentia S.L.** (Spain).

---

## What is DACCOSIM?

In industrial engineering (energy, smart grids, building performance, aerospace, automotive), full-system simulation requires interconnecting heterogeneous domains (electrical, thermal, mechanical, control systems). Each discipline typically uses its own specialized tool (such as MATLAB/Simulink, Dymola, or OpenModelica).

DACCOSIM acts as the **master orchestrator**: it connects numerical models from different software environments and coordinates their data exchanges to simulate the global system behavior.

---

## Key Features

- **FMI/FMU Standard Native**: Relies on the Functional Mock-up Interface (FMI) standard. Models are imported and interconnected as Functional Mock-up Units (FMUs) arranged in a co-simulation graph.
- **High-Performance & Distributed Computing**: Executes models in parallel across multiple CPU cores (multithreading) or distributes them over computing cluster nodes (ActiveMQ/JMS).
- **Advanced Master Algorithms**: Offers fixed-step and variable-step coordination algorithms, error control, and algebraic loop resolution (e.g., Newton-Raphson).
- **Matryoshka FMU**: Enables packaging an entire co-simulation graph into a single, hierarchical "super-FMU" reusable in any FMI-compliant tool.
- **Dual Interface**:
  - A visual **graphical editor** to design, inspect, and connect models intuitively.
  - A **command-line shell** for batch automation, script integration, and cluster runs.

---

## Typical Applications

- **Smart Grids & Cyber-Physical Systems**: Joint simulation of electrical distribution/transmission grids and automated control/telecom networks.
- **Building Energy Efficiency**: Coupling building envelope thermodynamics with HVAC systems and smart controllers.
- **Multi-domain Systems**: Integrating physics-based plant models with digital controllers.

---

## Partners

- **EDF R&D** (France)
- **CentraleSupélec** (France)
- **EIFER** (Germany)
- **SIANI Institute** (Spain)
- **Monentia S.L.** (Spain)

---

## References & Publications

If you use DACCOSIM in your research or studies, please refer to the following publications:

- **DACCOSIM NG Overview**:  
  J. Évora, J. J. Hernández Cabrera, J.-P. Tavella, S. Vialle, E. Kremers, L. Frayssinet,  
  *"Daccosim NG: co-simulation made simpler and faster"*,  
  Proceedings of the 13th International Modelica Conference, Regensburg, Germany, 2019. [DOI: 10.3384/ecp1915782](https://doi.org/10.3384/ecp1915782) — [HAL: hal-02121703](https://hal.science/hal-02121703)

- **Matryoshka FMU Technology**:  
  V. Galtier, M. Ianotto, M. Caujolle, R. Corniglion, E. Kremers,  
  *"Experimenting with Matryoshka Co-Simulation: Building Parallel and Hierarchical FMUs"*,  
  Proceedings of the 12th International Modelica Conference, Prague, Czech Republic, 2017. [DOI: 10.3384/ecp17132329](https://doi.org/10.3384/ecp17132329)

- **Distributed Multi-Simulation**:  
  V. Galtier, S. Vialle, C. Dad, J.-P. Tavella, J.-P. Lam-Yee-Mui, G. Plessis,  
  *"FMI-Based Distributed Multi-Simulation with DACCOSIM"*,  
  Proceedings of the Symposium on Theory of Modeling & Simulation (TMS/DEVS), 2015.

---

## License

This software is licensed under the **GNU Lesser General Public License v3.0 (LGPL-3.0)**.
