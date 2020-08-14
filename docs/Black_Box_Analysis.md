# Black Box Analisys of a Registered PAL

## Introduction

### Combinatorial PALs and bruteforcing

Combinatorial PALs are built around an internal network of wires, whose interconnections are broken at flashing time. These wires connect the inputs to an array of AND gates, whose outputs are in turn connected to an array of OR gates (implementing a sum of products).

Even when the chip lock is set (thus blocking direct reading via a programmer), recovering the functions that map the internal connections is relatively easy: the output state is dependant only on the current state of the inputs and nothing else, making the creation of a truth table trivial: just feed the PAL every combination of inputs and record the outputs.

### Registered PALs

Things get a bit harder when we consider registered PALs: these ICs add an array of flip-flops to the network.
These flip-flops have one of their outputs connected to a pin and the other feeding back into the internal network. A **clock** pin is also brought out to an external pin so that the flip-flops can be toggled.

When toggling the clock, the flip flop take their new value not only from a combinations of the inputs, but also from the feedback lines coming from out of the flip-flops themselves, i.e. when the clock is pulsed, the PAL changes its internal state, and **the new state is dependant on the previous one**.

## Analyzing a registered PAL device

### The Not-So-Black Box

From what I wrote in the introduction follows that a registered PAL device can be viewed as a stateful system whose current state is dependant on the previous one combined the state of the inputs at the moment the state is changed.

Luckily for us, the number of the possible states is known and dependant on the number of flip-flops inside the device (and, as every flip-flop is connected to a specific output, equal to the number of possible combinations that can be taken by the so-called **registered outputs**). Being connected directly to output pins, these flip-flop give us an important insight on the status of the PAL: *we are able to identify exactly in which state it is at the moment*.

To summarize and simplify, a registered PAL has the following types of outputs:

- **Combinatorial outputs**: their state is dependant on the current state of the inputs and on the current state of the registered outputs (via their feedbacks).
- **Registered outputs**: these only change when the clock pin is pulsed, and their new state depends on the state of the inputs and on their own feedback before the pulsing.

### Unboxing the PAL

It is now clear that a registered PAL is a state machine whose current state is tied to the flip-flops, in turn connected to the registered outputs.

The number of these registered outputs depends on the PAL model, but is fixed and known and cannot be changed via programming. From this we can gather that **a registered PAL device has 2^X possible states**, where **X is the number of registered outputs for that model**.

I will call these states defined by the registered outputs "**MacroStates**", to distinguish them from another type of state, the **SubStates**, which I'll describe below.

While the **MacroState** is defined by the status of the registered outputs, we also need to take into account the state of the combinatorial outputs. The combinatorial outputs state is dependant only on the inputs and the registered outputs, and changes without the need of pulsing the clock (we can think each MacroState in a registered PAL as a single simple combinatorial PAL).

From this we get that for every MacroState we need to test all the input combinations in order to calculate their corresponding SubStates. For every MacroState we'll have 2^Y possible substates, where Y is the number of the combinatorial outputs, but we need to map them to 2^Z combinations of inputs (Z is the number of the input pins).

#### Some simple math

We can see that for a registered PAL we have:

- 2^X theoretically possible MacroStates.
- 2^Z combinations of inputs for every MacroState, every combination maps to one of the 2^Y possible SubStates of that specific MacroState.
- 2^Z combination of inputs that could bring forth a movement to another MacroState when coupled with a clock pulse.

Where:

- X is the number of registered output pins.
- Y is the number of combinatorial output pins.
- Z is the number of input pins.

#### PAL as a Graph

The inner workings of a registered PAL can then be represented by a **directed graph** (or **digraph**), where every **vertex** is a **MacroState**, every **edge** is a link between a "starting" and a "destination" MacroState, and this link (which I'll call **StateLink**) is defined by the state of the inputs and the state of the registered outputs from the starting MacroState.

- Every MacroState will have 2^Z StateLinks (edges) coming out of it, some pointing to another MacroState, some pointing back at itself.
- We will have 2^X MacroStates (vertices) in the graph. Some will be connected to others, some will not be connected at all, depending on the programming of the device.
- Every MacroState will contain an array of 2^Z SubStates inside, defining all the possible states that the combinatorial outputs can take within that MacroState, while receiving different input combinations.

Reversing the inner working of the PAL device means that we need to find every possible StateLink (edge) in the digraph, and calculate all the SubStates for every MacroState (vertex) we can visit.

One this is done, we can use the graph we built to print out a truth table that represents all the possible states of the PAL, and from that, recover the logic equations.

##### Mapping the Graph

We can summarize the algorithm to build the graph this way:

1. Read the current state of the registered outputs: this will identify our MacroState
2. If the state was not yet visited, build the array of its substates by trying all the input combinations and registering the outputs.
3. Check if we have still unvisited StateLinks for this MacroState
    - If we have unvisited links, pick the first and visit it. Go back to 1.
    - If we have no unvisited links, search a path in the graph to a visited MacroState with yet unvisited links and follow it. Go back to 3 after following the path and feeding the PAL the input combinations.
        - If no visited MacroStates with unvisited links are found, we completed our mapping.
