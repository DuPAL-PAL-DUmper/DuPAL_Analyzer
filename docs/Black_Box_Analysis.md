# Black Box Analisys of a Registered PAL

## Introduction

### Combinatorial PALs and bruteforcing

Combinatorial PAL are built around an ideal internal network of wires, whose interconnections are broken at flashing time. These wires then connect the inputs to an array of AND gates, whose outputs are connected to an array of OR gates (the physical implementation of a sum of products).

Even when the chip lock is set (thus blocking direct reading via a programmer), recovering the functions that then map the internal connections is relatively easy: the output state is a dependant only on the current state of the inputs and nothing else, making the creation of a truth table a trivial matter: just feed the PAL every combination of inputs and record the outputs.

### Registered PALs

Things get a bit harder when we consider registered PALs: these ICs add an array of flip-flops to the network.
These flip-flops have one of their outputs connected to a pin and another feeding back into the network of wires. A **clock** pin is also added to the IC so that the flip-flops can be toggled.

This boils down to the fact that, when toggling the clock, the new value of the outputs connected to the flip-flops is no longer dependant on the inputs alone, but also on the feedback lines coming from the flip-flops themselves, i.e. when the clock is pulsed, the PAL changes its internal state, and the new state is dependant on the previous one.

## Analyzing a registered PAL device

### The Not-So-Black Box

As I hited at in the introduction, a registered PAL device can be viewed as a stateful system, whose current state is dependant on the previous one plus the state of the inputs at the time of moving to the new state.

Luckily for us, the number of the possible states is known and dependant on the number of flip-flops in the device (and, as every flip-flop is connected to a specific output, dependant on the number of so called **registered outputs**). Having said flip-flops directly connected to outputs gives us an important insight on the PAL, as *we know exactly in which state it is at the moment*.

To summarize and simplify, a registered PAL has the following types of outputs:

- **Combinatorial outputs**: their state is dependant on the current state of the inputs and on the current state of the registered output feedbacks.
- **Registered outputs**: these only change when the clock pin is pulsed, and their new state depends on the state of the inputs and on their own state before the pulsing.
