# Black Box Analisys of a Registered PAL

## Introduction

### Combinatorial PALs and bruteforcing

Combinatorial PAL are built around an ideal internal network of wires, whose interconnections are broken at flashing time. These wires then connect the inputs to an array of AND gates, whose outputs are connected to an array of OR gates (the physical implementation of a sum of products).

Even when the chip lock is set (thus blocking direct reading via a programmer), recovering the functions that then map the internal connections is relatively easy: the output state is dependant only on the current state of the inputs and nothing else, making the creation of a truth table a trivial matter: just feed the PAL every combination of inputs and record the outputs.

### Registered PALs

Things get a bit harder when we consider registered PALs: these ICs add an array of flip-flops to the network.
These flip-flops have one of their outputs connected to a pin and another feeding back into the network of wires. A **clock** pin is also added to the IC so that the flip-flops can be toggled.

This boils down to the fact that, when toggling the clock, the new value of the outputs connected to the flip-flops is no longer dependant on the inputs alone, but also on the feedback lines coming from the flip-flops themselves, i.e. when the clock is pulsed, the PAL changes its internal state, and the new state is dependant on the previous one.

## Analyzing a registered PAL device

### The Not-So-Black Box

As I hinted at in the introduction, a registered PAL device can be viewed as a stateful system, whose current state is dependant on the previous one plus the state of the inputs at the time of moving to the new state.

Luckily for us, the number of the possible states is known and dependant on the number of flip-flops in the device (and, as every flip-flop is connected to a specific output, dependant on the number of so called **registered outputs**). Having said flip-flops directly connected to outputs gives us an important insight on the PAL, as *we know exactly in which state it is at the moment*.

To summarize and simplify, a registered PAL has the following types of outputs:

- **Combinatorial outputs**: their state is dependant on the current state of the inputs and on the current state of the registered output feedbacks.
- **Registered outputs**: these only change when the clock pin is pulsed, and their new state depends on the state of the inputs and on their own state before the pulsing.

### Unboxing the PAL

So, we saw that a registered PAL is a state machine, and that the current state is tied to the flip-flops, which are in turn connected to the registered outputs.

The number of these registered outputs depends on the PAL model, but is fixed and known for every specific model. From this we can gather that **a registered PAL device has 2^X possible states**, where **X is the number of registered outputs for that model**.

I will call these states that are defined by the registered outputs "**MacroStates**", to distinguish them from another type of state, the **SubStates**, which I'll describe below.

While the **MacroState** is defined by the status of the registered outputs, we also need to take into account the state of the combinatorial outputs. The combinatorial outputs stat, is dependant only on the inputs and the registered outputs, and changes without the need of pulsing the clock.

From this we get that for every MacroState we need to test all the input combinations to find the SubStates corresponding to them. For every MacroState we'll have 2^Y, where Y is the number of the combinatorial outputs, possible SubStates, but we'll have to try 2^Z combinations, where Z is the number of the inputs.

#### Some simple math

We can see that for a registered PAL we have:

- 2^X theoretically possible MacroStates.
- 2^Z combinations of inputs for every MacroState, every combination maps one of the 2^Y possible SubStates for that specific MacroState.
- 2^Z combination of inputs that can bring forth a movement to another MacroState when coupled with a clock pulse.

Where:

- X is the number of registered outputs
- Y is the number of combinatorial outputs
- Z is the number of inputs

#### PAL as a Graph

The inner workings of a registered PAL can then be represented by a **directed graph** (or **digraph**), where every **vertex** is a **MacroState**, every **edge** is a link between a "starting" and a "destination" MacroState, and this link (which I'll call **StateLink**) is defined by the state of the inputs and the state of the registered outputs from the starting MacroState.

- Every MacroState will have 2^Z StateLinks (edges) coming out of it, some pointing to another MacroState, some pointing back at itself.
- We will have 2^X MacroStates (vertices) in the graph. Some will be connected to others, some will not be connected at all, depending on the programming of the device.
- Every MacroState will contain an array of 2^Z SubStates inside, defining all the possible states that the combinatorial outputs can take within that MacroState, by trying different input combinations.

Reversing the inner working of the PAL device means that we need to find every possible StateLink (edge) in the digraph, and calculate all the SubStates for every MacroState (vertex) we reach.

One this is done, we can use the graph we built to print out a truth table that represents all the possible states of the PAL, and from that, recover the logic equations.

##### Exploring the Graph

We can summarize the algorithm to build the graph this way:

1. Read the current state of the registered outputs: this will identify our MacroState
2. If the state was not yet visited, build the array of its substates by trying all the input combinations and registering the outputs.
3. Check if we have still unvisited StateLinks for this MacroState
    - If we have unvisited links, pick the first and visit it. Go back to 1.
    - If we have no unvisited links, search a path in the graph to a visited MacroState with yet unvisited links and follow it. Go back to 3 after following the path.
        - If no visited MacroStates with unvisited links are found, we completed our mapping.

