# DuPAL Analyzer

## Introduction

The **DuPAL Analyzer** is a companion software to the **DuPAL** board.
It uses the board's *REMOTE CONTROL* mode to remotely toggle the pins and read the outputs, and is meant to perform **blackbox analisys** on the registered PAL devices, which are a bit too much for the MCU firmware to handle by itself.

### What this is NOT

Despite the "DUmper" part of the name, this tool is **NOT** meant to produce 1:1 binary dumps of the content of a PAL device, it is meant as an aid to the reversing procedure of an unknown PAL, automating a good part of the black box analisys.

It will produce a JSON file containing every recorded state change of the PAL (outputs states at the beginning, applied inputs and output states at the end), that can then be converted into an espresso truth table or manipulated for further analisys.

## The Analyzer

The analyzer lets the user select which type of PAL is inserted in the board's ZIF socket, whether the IO pins that are set as outputs are known (which saves some time by avoiding autodetection), what is the board's serial interface, and where to save the output file.
Once this is known, the application will:

1. Connect to the board, reset it, and enable the *REMOTE MODE*, so it accepts command from the application.
2. If which I/O pins are actually outputs is not known, the board will try to guess this and print the result. This procedure is not bulletproof (or it would take the same time as the proper analisys: in case it did not detect some outputs, these will be found during the analisys and will halt the procedure, allowing the user to specify them correctly for the next run).
3. The analisys will start. The procedure can take hours to complete.
4. A JSON file that collects every state of the outputs of the PAL and how they change depending on the inputs is saved to file.

### Supported devices

The following PAL models are supported:

#### Combinatorial

- PAL10L8
- PAL16L8
- PAL20L8

#### Registered

- PAL16R4
- PAL16R6
- PAL16R8

### Command line

The format for command line execution is the following:

```text
java -jar /path/to/dupal_analyzer.jar <serial_port> <pal_type> <output_file> [hex_output_mask]
```

- **serial_port:** is just the serial port to use to connect to the DuPAL board. Connection is hardcoded at **57600bps 8n1** without flow control.
- **pal_type:** is the type of PAL device that is going to be analyzed.
- **output_file:** The file where the analyzer will save the JSON output.
- **hex_output_mask:** This mask (a byte represented as an *hex number*) is used to tell the Analyzer which IOs are configured as outputs. If it's not present, the Analyzer will try to guess it by itself. It's usually advisable to let the guessing run for a few minutes, then restart the analisys by specifying the guessed mask. If the mask is wrong, during the analisys an error will be thrown as soon as what was thought as an input is found to be an output. At that point the analisys can be restarted with the new mask.

#### The output mask format

The output mask is a byte represented as an hex value, where a bit is set when the corrisponding pin is considered an output.
From MSB to LSB for a 20 pins PAL:

```text
   7    6    5    4    3    2    1    0
.----.----.----.----.----.----.----.----.
| 12 | 19 | 13 | 14 | 15 | 16 | 17 | 18 |
'----'----'----'----'----'----'----'----'
```

From MSB to LSB for a 24 pins PAL:

```text
   7    6    5    4    3    2    1    0
.----.----.----.----.----.----.----.----.
| 22 | 21 | 20 | 19 | 18 | 17 | 16 | 15 |
'----'----'----'----'----'----'----'----'
```

Setting the mask to `0x02`, for example, will notify the analyzer that pin 17 on a 20 pins PAL or pin 16 on a 24 pins PAL is configured as an OUTPUT.

Pay attention to the weird position for pin 19, that position is caused by a desire to save a few lines on the firmware.

#### Requirements

Make sure you have at least a **Java 1.8** compatible JRE installed and have access to your serial port devices (In linux it's usually sufficient to add your user to the `dialout` group).

## Credits

- Thanks to [jammarcade.net](https://www.jammarcade.net/) for hosting all those PAL dumps, I used a lot of them to test my implementation.
- Thanks to @mesillo for taking time to read my documentation and pointing out possible improvements.
