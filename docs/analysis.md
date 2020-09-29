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
