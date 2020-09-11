package info.hkzlab.dupal.analyzer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import info.hkzlab.dupal.analyzer.utilities.BitUtils;

public class UtilsTest 
{
    @Test
    public void bitUtilsShouldCorrectlyModifyBitfields() {
        assertEquals("0b01010101 with a selection mask 0x55 should be consolidated into 0b1111", 0x0F, BitUtils.consolidateBitField(0x55, 0x55));
        assertEquals("0b11111111 with a selection mask 0x55 should be consolidated into 0b1111", 0x0F, BitUtils.consolidateBitField(0xFF, 0x55));
        assertEquals("0b01010101 with a selection mask 0xAA should be consolidated into 0", 0, BitUtils.consolidateBitField(0x55, 0xAA));
        assertEquals("0b01010101 with a selection mask 0xAA should be consolidated into 0b0101", 0x05, BitUtils.consolidateBitField(0x55, 0xF0));
        
        assertEquals("0b00001111 with a scatter mask 0xAA should be scattered into 0b10101010", 0xAA, BitUtils.scatterBitField(0x0F, 0xAA));
        assertEquals("0b00001111 with a scatter mask 0x03 should be scattered into 0b00000011", 0x03, BitUtils.scatterBitField(0x0F, 0x03));
        assertEquals("0b01010101 with a scatter mask 0x0F should be scattered into 0b00000101", 0x05, BitUtils.scatterBitField(0x55, 0x0F));
        assertEquals("0b01011111 with a scatter mask 0xF0 should be scattered into 0b11110000", 0xF0, BitUtils.scatterBitField(0x5F, 0xF0));
    }
}
