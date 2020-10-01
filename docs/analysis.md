# PAL Analysis

## Introduction

From [Wikipedia](https://en.wikipedia.org/wiki/Programmable_Array_Logic):

> Programmable Array Logic (PAL) is a family of programmable logic device semiconductors used to implement logic functions in digital circuits [...]
>
> PAL devices consisted of a small PROM (programmable read-only memory) core and additional output logic used to implement particular desired logic functions with few components.

This PROM is used to implement a programmable logic plane that routes the signal present on input pins (and on the feedbacks from the outputs) to the output logic macrocells.

This plane is arranged in a **fixed-OR, programmable-AND** configuration,  and is used to implement a binary logic equation for every output pin in the form of **sum-of-products**.

In short, PAL chips programmable content can be defined as a set of equation like the following:

```text
/o13 = /i9 & /i11 & o17 +
       /i7 & /i11 +
       /i6 & /i11
o13.oe = i6 & i7 & /i9 & o17

/o14 = /i9 & /o13 & o18 +
       /i7 & /o13 +
       /i6 & /o13
o14.oe = i6 & i7 & /i9 & o18

/o15 = i1 & /i9 & /o14 +
       /i7 & /o14 +
       /i6 & /o14
o15.oe = i1 & i6 & i7 & /i9
```

Most of the chips have their PROM set to **read-protected** once programmed, meaning their content cannot be trivially recovered and leaving a party interested in the recovery with just a few options:

- Decapping the chip and using a microscope to analyze the PROM
- There are stories around where a PROM can be glitched to disable read protection, but I've never found the details
- **Blackbox analysis**

This tool aims to automate part of the process for the last of these options.
Ideally, a successfull analysis should recover the original equations, but we'll see how this is not always possible or straightforward.

## PAL Variants

PAL chips come in different variants with different features that impact their internal structure and outward capabilities. We can differentiate their input types in 3 categories.

- "simple" inputs
- asynchronous feedbacks from the outputs
- synchronous feedbacks from the registered outputs

Only one of these types of inputs is under direct control of the external circuit.

### "Simple" inputs

These inputs are directly connected to an external pin of the chip, and can be toggled by the external circuit.
Some pins, called I/O, can be configured to act as an Input or as an Output (in which case, the output value is then used as an asynchronous feedback).

### Asynchronous feedbacks

This type of input is not controlled by the external circuit, but by the PAL itself. The value is taken from one of the outputs and then fed back into the logic plane. It's asynchronous because its value changes as soon as the output value tied to it changes, and this happens as soon as the inputs that feed it are modified.

### Synchronous feedbacks

This type of input is similar to the *asynchronous feedback*, with the difference that the output tied to it is a **registered output** that changes its value only in correspondence of a clock pulse, and not immediately after its inputs change value.

## Analysis

Ideally, recovering the structure of the logic plane of a PAL would be done by feeding the logic plane every input combination and record the corresponding outputs.

With such information we could then build a truth table that ties input combinations to output combinations, and from there, obtain logical equations equivalent to the ones used to program the PAL.

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

### A representation of the PAL

To analyze all the possible states of a PAL device we can draw a directed **graph**:

- Every vertex in the graph represents a combination of the **outputs** of the PAL
- Every edge is associated with a combination of the (simple, directly modifiable) **inputs** of the PAL

With this representation, we can see, for every state of the PAL, how the outputs (and, as a consequence, the feedbacks) change depending on the inputs. Obviously we'll have 2^x edges out of a vertex, where x is the number of the inputs to the PAL (Actually, if the PAL has synchronous AND asynchronous feedbacks, every combination will have to be tried twice, once with a clock pulse and once without, getting a total of 2*2^x edges per vertex).

#### The Map analogy

To build the graph, we can use this analogy: Imagine You get parachuted in the middle of a city with the mission to map every connection this city has with neighbouring cities. Every road out of the city is one-way, and there are no road signs to tell you where every road goes. A road could very well loop you back to the city you are attempting to leave. To draw a map you could follow this simple algorithm:

0. Look around and note in which city you're in as a start.
1. Search for an yet-unexplored road out of the the current city
   - Have you found an unexplored road? Go to 2.
   - Have you not found one? Search on the map you have already drawn for the shortest road that gets you to a city with still unexplored roads
      - Found a path? Follow it and go back to 1.
      - No path? Then we're finished. You have a complete map of all the roads.
2. Follow the road you found at the previous step. Remember to draw a line from the city you depart to the city you get to (which could very well be the same), and not the road number. Go back to 1.

This is exactly the same procedure we follow while analyzing a PAL. Obviously, there could be cities not connected by roads (the unreachable states). Those will never be reachable in reality, but they will still cause our map to be incomplete.
