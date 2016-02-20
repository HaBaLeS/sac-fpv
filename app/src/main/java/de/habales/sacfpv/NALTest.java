package de.habales.sacfpv;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by falko on 15.02.2016.
 */
public class NALTest {

    private final static int UDP_MAX_SIZE = 256*256;
    private final static byte[] inBuf = new byte[UDP_MAX_SIZE];
    private static final String HEXES = "0123456789ABCDEF";

    public static void main(String args[]) throws Exception{
        System.out.println("GoGoGo");

        int outSize = 1024*1024;
        byte[] outBuffer = new byte[outSize];
        outBuffer[0] = 0x00;
        outBuffer[1] = 0x00;
        outBuffer[2] = 0x00;
        outBuffer[3] = 0x01;

        FileInputStream fin = new FileInputStream("C:\\Users\\falko\\Pictures\\somefile.264");


        int cnt = 0;
        int nzc = 0; //Count of Zeros in a row

        int outBufPos = 0;


        while((cnt = fin.read(inBuf,0,UDP_MAX_SIZE)) > 0){
            for(int i=0; inBuf.length > i; i++){
                byte b = inBuf[i];//get on byte

                //we have 0x00 and we have less than 2 0x00 in a row
                if(b == 0x00 && nzc <3){
                    nzc++;
                } else if(nzc >=3) {
                    //count is > 2 so we found a NAL. this means a new Block starts now
                    System.out.println("Data: " + getHex(Arrays.copyOfRange(outBuffer,0,outBufPos)));

                    outBufPos = 4;

                    nzc = 0; //Reset 0x00 counter
                } else {
                    outBuffer[outBufPos] = b; //copy to outBuffer
                    outBufPos++;
                    nzc = 0;
                }
            }
            break;
        }

        System.out.println("Bye");
        fin.close();

    }



    static String getHex(byte[] raw) {
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }

}
