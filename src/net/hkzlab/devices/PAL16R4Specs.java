package net.hkzlab.devices;

public class PAL16R4Specs implements PALSpecsInterface {

    @Override
    public int getNumINPins() {
        return 8;
    }

    @Override
    public int getNumIOPins() {
        return 4;
    }

    @Override
    public int getNumOUTPins() {
        return 0;
    }

    @Override
    public int getNumROUTPins() {
        return 4;
    }
    
    @Override
    public int getCLKPinMask() {
        return 0x00000001;
    }

    @Override
    public int getOEPinMask() {
        return 0x00000200;
    }

    @Override
    public int getINMask() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getOUTMask() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getIO_INMask() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getIO_OUTMask() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getROUTMask() {
        // TODO Auto-generated method stub
        return 0;
    }

}