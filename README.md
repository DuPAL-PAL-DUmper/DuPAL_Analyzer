# DuPAL Analyzer
## Introduction
The **DuPAL Analyzer** is a companion software to the **DuPAL** board.
It uses the board's *Remote Control* mode to remotely toggle the pins and read the outputs, and is meant to perform **blackbox analisys** on the registered PAL devices, which are a bit too much for the MCU firmware to handle by itself.

### What it can do
This analyzer works in tandem with the DuPAL board to:
- Do a black-box analisys of a set of **registered** PAL devices and produce a truth table that can be minimized and transformed into logic equations functionally equivalent to those used to program the chip.

### What it can't do
- Analyze non-registered PAL devices (these can be done directly by the DuPAL board by connecting to its serial port with a terminal emulator)
- Produce logic equations out of the PAL: reiventing the wheel is not an intelligent thing to do, you can feed the truth table to software like `espresso` heuristic logic minimizer and get the equations automatically. You can then further refine them with a tool like **Logic Friday**, if needed. See below.
- While the logic equations that are going to be produced by using this + a minimizer are going to be functionally equivalent to those used to program the chip (at least for all the possible inputs), there is a chance they're not going to be identical. This is because the minimizer might further optimize them and because they won't make use of the feedback outputs during optimization. 
    - If the feedback outputs are required (e.g. to further reduce the number of terms so they can fit in a new chip), the equations can be placed into **Logic Friday** and massaged (e.g. factor them, turn them into sums of products, invert them, etc) until a direct substitution with a feedback output becomes easy enough to be performed.

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
    - I haven't found a way to do an automatic replacement of these terms, but they're usually apparent when looking at the equations that make use of them. **Logic Friday** is a good tool to put the equations in, and either factorize them, transform the in *sums of products* or *products of sums* until You can spot a member that contains the same operands as one of the feedback outputs. At that point it becomes a matter of doing a simple replacement.

### Supported devices
The following PAL models are supported
- PAL16R4
- PAL16R6
- PAL16R8 **(untested)**

### Command line
The format for command line execution is the following:
```
java -jar /path/to/dupal_analyzer.jar <serial_port> <pal_type> <output_directory> [hex_output_mask]
```
- **serial_port:** is just the serial port to use to connect to the DuPAL board. Connection is hardcoded at **57600bps 8n1** without flow control.
- **pal_type:** is the type of PAL device that is going to be analyzed. Only registered PALs are supported (non-registered PALs can be dumped directly by the board by accessing the serial interface). Supported types are:
    - PAL16R4
    - PAL16R6
    - PAL16R8
- **output_directory:** Where DuPAL Analyzer will output the generated truth table and where it will periodically save the status of the analisys so it can be stopped and recovered later.
- **hex_output_mask:** This mask (an byte written as an *hex number*) is used to tell the Analyzer which IOs are configured as outputs. If it's not present, the Analyzer will try to guess it by itself. It's usually advisable to let the guessing run for a few minutes, then restart the analisys by specifying the guessed mask. If the mask is wrong, during the analisys an error will be thrown as soon as what was thought as an input is found to be an output. At that point the analisys can be restarted with the new mask.

#### The output mask format
The output mask is a byte represented as an hex value, where a bit is set when the corrisponding pin is considered an output.
From LSB to MSB:
```
18 17 16 15 14 13 19 12
```
Pay attention to the weird position for pin 19.

#### Requirements
Make sure you have at least a **Java 1.8** compatible JRE installed and have access to your serial port devices (In linux it's usually sufficient to add your user to the `dialout` group).

## Dependencies
Remember to install `jssc` 2.9.2 by [java-native](https://github.com/java-native/jssc) launching this command inside the project directory after checkout:
```
mvn install:install-file -Dfile=/path/to/jssc-2.9.2.jar -DgroupId=io.github.java-native -DartifactId=jssc -Dversion=2.9.2 -Dpackaging=jar
```