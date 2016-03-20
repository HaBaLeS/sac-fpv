package de.habales.sacfpv;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by falko on 01.03.2016.
 *  raspivid -t 0 -ih -fl  -g 1 -fps 90  -pf baseline  -w 640 -h 480  -b 10000000 -o - | socat - UDP4-DATAGRAM:192.168.1.204:5561

 */
public class H264DecodeThreadUDP extends Thread {
    static final String TAG = "DCT";

    private static final String HEXES = "0123456789ABCDEF";


    private final static int UDP_MAX_SIZE = 256*256;
    private final static byte[] inBuf = new byte[UDP_MAX_SIZE];


    static final int TYPE_NONE_IDR_FRAME = 1;
    static final int TYPE_IDR_FRAME = 5;
    static final int TYPE_SPS = 7;
    static final int TYPE_SPP = 8;

    //MediaFormat mediaFormat;
    MediaCodec mediaCodec;
    MediaCodec.BufferInfo info;


    boolean sppFound = false;
    boolean spsFound = false;
    boolean foundStart = false;
    boolean codecStarted = false;

    //NEEDED IN SDK < 20
    ByteBuffer[] inputBuffers;
    ByteBuffer[] outputBuffers;

    byte[] spsBytes;
    byte[] sppBytes;

    int outSize = 1024 * 1024;
    byte[] outBuffer = new byte[outSize];

    int cnt = 0;
    int nzc = 0; //Count of Zeros in a row
    int outBufPos = 0;


    public static String VIDEO_MULTICAST_IP = "224.0.0.222";
    public static int VIDEO_MULTICAST_PORT = 50666;
    byte[] message = new byte[outSize];

    /**
     * Construct a new decoder thread for Surface
     * @param surfaceToDrawOn
     */
    public H264DecodeThreadUDP(Surface surfaceToDrawOn){
        try {
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            MediaFormat format = MediaFormat.createVideoFormat("video/avc", 1280, 720);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 90);
            mediaCodec.configure(format, surfaceToDrawOn, null, 0);
            info = new MediaCodec.BufferInfo();
            mediaCodec.start();
        } catch ( IOException iox){
            throw new RuntimeException("grr",iox);
        }
    }

    @Override
    public void run() {


        try {

            DatagramSocket dataSocket = new DatagramSocket(VIDEO_MULTICAST_PORT);
            DatagramPacket packet = new DatagramPacket(message, message.length);



            while (true) {
                dataSocket.receive(packet);
                feed(message, packet.getOffset(), packet.getLength());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error processing file!",e);
        }

    }


    public void feed(byte[] data, int offest ,int lenth)throws Exception{

                for(int i=offest; lenth > i; i++){
                    byte b = data[i];//get on byte
                    outBuffer[outBufPos] = b;
                    outBufPos++;

                    //we have 0x00 and we have less than 2 0x00 in a row
                    if(b == 0x00 && nzc <3){
                        nzc++;
                    } else if(nzc >=3) {
                        //count is > 2 so we found a NAL. this means a new Block starts now
                        //System.out.println("Data: " + getHex(Arrays.copyOfRange(outBuffer, 0, outBufPos)));
                        outBuffer[0] = 0;
                        outBuffer[1] = 0;
                        outBuffer[2] = 0;
                        outBuffer[3] = 1;
                        if(outBufPos > 4) {
                            //System.out.println("LEN: " + (outBufPos-4));
                            String frame = getHex(Arrays.copyOfRange(outBuffer, 0, outBufPos - 4));
                            //System.out.println("Data: " + frame + " EOF");

                            if(!foundStart) {
                                process(outBuffer,outBufPos-4);
                            }

                            if(foundStart) {
                                processFrame(outBuffer, outBufPos - 4);
                            }
                        }
                        outBufPos = 4;

                        nzc = 0; //Reset 0x00 counter
                    } else {
                        nzc = 0;
                    }
                }


    }

    void processFrame(byte[] frameData, int len){
        /*
        In broad terms, a codec processes input data to generate output data.
        It processes data asynchronously and uses a set of input and output buffers.
        At a simplistic level, you request (or receive) an empty input buffer, fill it
        up with data and send it to the codec for processing. The codec uses
        up the data and transforms it into one of its empty output buffers.
        Finally, you request (or receive) a filled output buffer, consume its
        contents and release it back to the codec.
         */
        int inputBufIndex = mediaCodec.dequeueInputBuffer(0);
        if (inputBufIndex >= 0) {
            //NEEDED IN SDK < 20
            inputBuffers = mediaCodec.getInputBuffers();

           // Log.d(TAG, " input inputBufIndex" + inputBufIndex + " LEN:" + len);
            ByteBuffer inputBuffer = inputBuffers[inputBufIndex]; //changed with android >= 20
            inputBuffer.put(frameData, 0, len);
            mediaCodec.queueInputBuffer(inputBufIndex, 0, len, System.currentTimeMillis(), 0); //Set data back to codec
        } else {
            Log.d(TAG, "NO Input buffer for me :-( " + inputBufIndex );
        }





        /*
        Using an Output Surface
        The data processing is nearly identical to the ByteBuffer mode when using an output Surface;
        however, the output buffers will not be accessible, and are represented as null values. E.g. getOutputBuffer/Image(int) will
        return null and getOutputBuffers() will return an array containing only null-s.

        When using an output Surface, you can select whether or not to render
        each output buffer on the surface. You have three choices:

        Do not render the buffer: Call releaseOutputBuffer(bufferId, false).
        Render the buffer with the default timestamp: Call releaseOutputBuffer(bufferId, true).
        Render the buffer with a specific timestamp: Call releaseOutputBuffer(bufferId, timestamp).

        Since M, the default timestamp is the presentation timestamp of the buffer (converted to nanoseconds). It was not defined prior to that.
                    Also since M, you can change the output Surface dynamically using setOutputSurface.
         */


        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(info, 0);
        if (outputBufferIndex >= 0) {
            //ByteBuffer outputBuffer = decoder.getOutputBuffers()[outputBufferIndex]; not needed
            mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
        }else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            outputBuffers = mediaCodec.getOutputBuffers();
        } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            // Subsequent data will conform to new format.
            MediaFormat format = mediaCodec.getOutputFormat();
        } else {
            Log.d(TAG, "NR No output buffer" + outputBufferIndex);
        }
    }

    private void process(byte[] naluBytes,int len) {
        int type = analyzeNALU(naluBytes);
        switch (type){
            case TYPE_IDR_FRAME:
                break;
            case TYPE_NONE_IDR_FRAME:
                break;
            case TYPE_SPP:
                foundStart = true;
                break;
            case TYPE_SPS:
                foundStart = true;
                break;
        }
    }

    /**
     * Take the NALU and identify he Type for furtherprocessing
     */
    private int analyzeNALU(byte[] outBuffer) {
        byte b01 = outBuffer[4];
        //String binarybyte = Integer.toBinaryString(b01);
        //System.out.println(binarybyte + "\t" + bytes.length);

        //first bit should be 0 sp mask with 1000 0000 an test > 0
        if( (b01 & 0x80) != 0){
            return -1;
            //throw new RuntimeException("First Bit must be 0");
        }

        //we want bt 2-3 so bitmask = 0110 0000
        int  nal_ref_idc =  (b01 & 0x60) >>> 4;
        if(nal_ref_idc == 0){
            throw  new RuntimeException("I am interested in that .. show me more ");
        }

        //We want 3-7 with 0001 1111 which s 0x1F
        int  nal_unit_type =  (b01 & 0x1F);
        switch (nal_unit_type) {
            case 1:
                //System.out.println("Coded slice of a non-IDR picture");
                return nal_unit_type;
            case 5:
                //System.out.println("Coded slice of an IDR picture");
                return nal_unit_type;
            case 7:
                //System.out.println("Sequence parameter set");
                return nal_unit_type;
            case 8:
                //System.out.println("Picture parameter set");
                return nal_unit_type;
            default:
                //throw new RuntimeException("New an unhandled nale_unit_type " + nal_unit_type);
                Log.w(TAG, "analyzeNALU: Unknown NALU TYPE: " + nal_unit_type);
                return -1;
        }
    }

  static String getHex(byte[] raw) {
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }
}
