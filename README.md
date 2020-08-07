# DuPAL Analyzer
## Introduction
The **DuPAL Analyzer** is a companion software to the **DuPAL** board.
It uses the board's *Remote Control* mode to remotely toggle the pins and read the outputs, and is meant to perform **blackbox analisys** on the registered PAL devices, which are a bit too much for the MCU firmware to handle by itself.

## The Analyzer
The analyzer lets the user select which type of registered PAL is inserted in the board's ZIF socket, whether the output pins are known (which saves some time), what is the board's serial interface, and where to save the output files.
Once this is known, the application will:
1. Connect to the board, reset it, and enable the *remote mode*
2. If which I/O pins are actually outputs is not known, the board will try to guess this and print the result. This procedure is not bulletproof (or it would take the same time as the proper analisys: in case it did not detect some outputs, these will be found during the analisys and will halt the procedure, allowing the user to specify them for the next run)
3. The analisys will start. The status will be saved every 5 minutes, so it can be stopped and resumed at leisure. The procedure can take hours to complete.
4. A truth table formatted in a way that the `espresso` heuristic logic minimizer likes is saved to a file.

### Caveats
Once the table is obtained, its content should be minimized and equations calculated: `espresso` is a good tool for this.
Often, some of the resulting equations for the registered outputs will not fit into a new PAL device.
There are multiple reasons for this:
1. To obtain the original equations, one should in theory produce a table where every input is fed to the black box and every output is recorded: with registered devices this is alas impossible: the input of a state depends on the previous state, and not every state can be reached.
    - The result will be a set of equations functionally identical to the originals, for all the states that can be reached in reality, but one cannot always recover the original equations.
2. To save products in the PAL circuitry, feedback outputs are used. This means some outputs take their value and feed it back to the network inside the PAL. As all the outputs are fed by the same inputs, the equations will be equivalent, but won't contain this simplification that can save important space on the device.
    - I haven't found a way to do an automatic replacement of these terms, but they're usually apparent when looking at the equations that make use of them. `Logic Friday` is a good tool to put the equations in, and either factorize them, transform the in *sums of products* or *products of sums* until You can spot a member that contains the same operands as one of the feedback outputs. At that point it becomes a matter of doing a simple replacement.

### Supported devices
The following PAL models are supported
- PAL16R4
- PAL16R6
- PAL16R8 **(untested)**

### Command line
**TODO**