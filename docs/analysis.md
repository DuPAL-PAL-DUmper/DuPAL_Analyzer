# PAL Analysis

## Introduction

From [Wikipedia](https://en.wikipedia.org/wiki/Programmable_Array_Logic):

> Programmable Array Logic (PAL) is a family of programmable logic device semiconductors used to implement logic functions in digital circuits [...]
>
> PAL devices consisted of a small PROM (programmable read-only memory) core and additional output logic used to implement particular desired logic functions with few components.

This PROM is used to implement a programmable logic plane that routes the signals present on input pins (and the feedbacks from the outputs) to the output macrocells.

This plane is arranged in a **fixed-OR, programmable-AND** configuration,  and is used to implement a binary logic equation for every output pin in the form of **sum-of-products**.

In short, PAL chips programmable content can be defined as a set of equations like the following:

```text
/o13 = /i9 & /i11 & o17 +
       /i7 & /i11 +
       /i6 & /i11

/o14 = /i9 & /o13 & o18 +
       /i7 & /o13 +
       /i6 & /o13

/o15 = i1 & /i9 & /o14 +
       /i7 & /o14 +
       /i6 & /o14
```

**Note:** The `&` is an AND, the `+` is an OR, the `/` is a NOT and the `/` on the result of the equation indicates that the pin is active-low.

Once programmed, most of the chips have their PROM set to **read-protected**, meaning their content cannot be trivially recovered and leaving a party interested in the recovery with just a few options:

- Decapping the chip and using a microscope to analyze the PROM
- There are stories around where a PROM can be glitched to disable read protection, but I've never found meaningful details
- **Blackbox analysis**

This tool aims to automate part of the process for the last of these options.
Ideally, a successfull analysis should recover the original equations, but we'll see how this is not always possible or straightforward.

## PAL Variants

PAL chips come in different variants, each with different mix-and-match features that impact their internal structure and outward capabilities. For our analysis, we can differentiate them according to their inputs, ending up with 3 categories:

- "simple" inputs
- asynchronous feedbacks from the outputs
- synchronous feedbacks from the registered outputs

Only one of these types of inputs is under direct control of the external circuit.

### "Simple" inputs

These inputs are directly connected to an external pin of the chip, and can be piloted by an external stimulus.
Some pins, called I/O, can be programmed to act as an Input or as an Output (in which case, the output value is then used as an asynchronous feedback).

### Asynchronous feedbacks

This type of input is not piloted directly by the external circuit: it is a function of the other inputs and is piloted by the PAL itself.

The value is taken from one of the outputs and then fed back into the logic plane. It's asynchronous because its value changes as soon as the output value tied to it changes, and this happens immediately after inputs that feed the function that define this outputs it are modified.

### Synchronous feedbacks

This type of input is similar to the *asynchronous feedback*, with the difference that the output tied to it is a **registered output** that changes its value only in correspondence of a clock pulse, and not immediately after its inputs change value.

The function that calculates the value of the output whose value is feed back into the plane, is computed only at a clock pulse.

## Analysis & Limitations

Ideally, recovering the structure of the logic plane of a PAL would be done by feeding the logic plane every input combination and recording the corresponding outputs.

With such information we could then build a truth table that ties input combinations to output values, and from there, obtain logical equations equivalent to the ones used to program the PAL.

Alas, as described above, **we are not in control of all the inputs**, so we can try only the combinations that are realistically possible on the circuit, but we **won't be able to feed the logic plane all the input combinations**.

Take this set of equations, for example:

```text
o1 = /i1

o2 = i1

o3 = o1 * /i2 +
     o2 *  i2
```

We see that for `o1`  to be true, `i1` must be false. We also see that for `o2` to be true, `i1` must be true. From this, we gather that we'll never see both `o1` and `o2` be true or false together.

Then we have `o3`, which depends from the "simple" input `i2` and the asynchronous feedbacks of `o1` and `o2`. While we can control `i2` and set it to what we want, `o1` and `o2` are not under our control, and we cannot try every possible combination.

```text
i2 o1 o2      o3
 0  0  1       0
 1  0  1       1
 0  1  0       1
 1  1  0       0

 0  0  0       Impossible to test
 1  0  0       Impossible to test
 0  1  1       Impossible to test
 1  1  1       Impossible to test
```

Now, let's create an input table for `espresso` containing only the combinations we can test, and save it to an `example.tbl` file:

```text
.i 3
.o 1
.ilb i2 o1 o2
.ob o3
.phase 1

001 0
101 1
010 1
110 0
.e
```

And let's minimize it to obtain the corresponding equations:

```sh
$ espresso -Dexact -oeqntott example.tbl
o3 = (i2&!o1&o2) | (!i2&o1&!o2);
```

The equations are **different from the ones we used to program the PAL**, and yet, they respect the limited truth of the combinations that can be obtained on the chip while inserted in the circuit. The reason for this is that the minimizer did not have the result for all the 8 combinations (2^3 = 8) of inputs to rebuild the table.

For the sake of example, let's try to build a table with the remaining combinations:

```text
.i 3
.o 1
.ilb i2 o1 o2
.ob o3
.phase 1

001 0
101 1
010 1
110 0
000 0
100 0
011 1
111 1
.e
```

Then let's try to minimize it:

```sh
$ espresso -Dexact -oeqntott full-table.tbl
o3 = (!i2&o1) | (i2&o2);
```

With the full set of combinations at hand, the minimizer will give back the original equations.

So, we'll have to make ourselves content in trying all the possible combinations of feedbacks that the PAL can produce while in-circuit.

---

Another issue worth mentioning is what I call **intermediate states**.
Suppose we have the following equations that define outputs in a PAL:

```text
/o18 = /i9 +
       /i2 & /i3 & i4 & i6 & i7 & /i8 +
       fio15 & /fio18

/o15 = /i2 & i3 & i4 & i6 & i7 & /i8
```

`fio15` and `fio18` are the feedbacks from `o15` and `o18` outputs.

Suppose that `fio15` is currently `true` and `fio18` is `false` (so that `/fio18` is `true`). In this condition, `/o18` is in a stable `true` condition.

We now set the following inputs:

```text
i2 = false ===> /i2 = true
i3 = true
i4 = true
i6 = true
i7 = true
i8 = false ===> /i8 = true
i9 = true
```

And, considering the feedbacks as the remaining inputs:

```text
fio15 = true
fio18 = false ===> /fio18 = true
```

One would say that `/o18` is going to remain `true` (as both `fio15` and `/fio18` are `true`), but what happens is that `/o15 = /i2 & i3 & i4 & i6 & i7 & /i8` becomes `true`, so `fio15` becomes `false`, making `fio15 & /fio18` false, and thus `/o18` is going to become `false` too.

So, even though we started from a condition that should have given us a `true` for `/o18`, a feedback changed our conditions as an intermediate step.

---

Another important limitations is that the DuPAL Board is able to capture **ONLY STABLE STATES**, if a PAL has an output that is continuously flipping state because it's piloted by an equation like

```text
o18 = /o18
```

then the board won't be able to capture it properly.

Intermediate states are also impossible to capture with the current hardware: as in the previous example, when we have a situation with intermediate states, what will happen is the following

1. Inputs will be set on the PAL
2. Outputs will change accordingly (Intermediate state)
3. Feedbacks of the outputs will be fed back into the PAL
4. Outputs will change again. Repeat 3/4 until we reach a stable combination
5. Stable state reached (this will be captured by the DuPAL Board)

It may be possible to capture the intermediate state **if the outputs are sampled quickly enough** (the timing is also dependent on the type of PAL being under analisys). This might prove to be helpful with PALs that are using feedbacks extensively, but **it will require a new hardware project and a new firmware** and also a new analysis procedure.

---

Currently, if the PAL is programmed in such a way that it can only come out of a state (for example, the state it is in right after power up), but never come back into it again via a combination of inputs, we won't be able to perform a complete map of the states graph.

### A representation of the PAL

To analyze all the possible states of a PAL device we can draw a directed **graph**:

- Every vertex in the graph represents a combination of the **outputs** of the PAL
- Every edge is associated with a combination of the (simple, directly modifiable) **inputs** of the PAL

With this representation, we can see, for every state of the PAL, how the outputs (and, as a consequence, the feedbacks) change depending on the inputs. Obviously we'll have 2^x edges out of a vertex, where x is the number of the inputs to the PAL (Actually, if the PAL has synchronous AND asynchronous feedbacks, every combination will have to be tried twice, once with a clock pulse and once without, getting a total of 2*2^x edges per vertex).

#### The Map analogy

To build the graph, we can use this analogy: Imagine You get parachuted in the middle of a city with the mission to map every connection this city has with neighbouring cities and then the connections the neighbouring cities have between themselves. Every road out of a city is one-way, and there are no road signs to tell you where every road goes. A road could very well loop you back to the city you are attempting to leave. To draw a map you could follow this simple algorithm:

0. Look around and note in which city you're in as a start.
1. Search for an yet-unexplored road out of the the current city
   - Have you found an unexplored road? Go to 2.
   - Have you not found one? Search on the map you have already drawn for the shortest road that gets you to a city with still unexplored roads
      - Found a path? Follow it and go back to 1.
      - No path? Then we're finished. You have a complete map of all the roads.
2. Follow the road you found at the previous step. Remember to draw a line from the city you depart to the city you get to (which could very well be the same), and note the road number. Go back to 1.

This is exactly the same procedure we follow while analyzing a PAL. Obviously, there could be cities not connected by roads (the unreachable states). Those will never be reachable in reality, but they will still cause our map to be incomplete.
