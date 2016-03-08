package de.habales.sacfpv;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Created by falko on 01.03.2016.
 */
public class H264DecodeThreadBlafasel extends Thread {
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
    boolean firstIDRFound = false;
    boolean codecStarted = false;

    //NEEDED IN SDK < 20
    ByteBuffer[] inputBuffers;
    ByteBuffer[] outputBuffers;

    byte[] spsBytes;
    byte[] sppBytes;

    /**
     * Construct a new decoder thread for Surface
     * @param surfaceToDrawOn
     */
    public H264DecodeThreadBlafasel(Surface surfaceToDrawOn){
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




            //mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1280, 720);
            String blaPath = Environment.getExternalStorageDirectory() + "/vv/blafasel2.264";
           // String blaPath = Environment.getExternalStorageDirectory() + "/rpi960mal810.h264";
            BufferedInputStream bin = new BufferedInputStream(new FileInputStream(blaPath));

            int outSize = 1024*1024;
            byte[] outBuffer = new byte[outSize];

            int cnt = 0;
            int nzc = 0; //Count of Zeros in a row
            int outBufPos = 0;

            while((cnt = bin.read(inBuf,0,UDP_MAX_SIZE)) > 0) {
                for(int i=0; inBuf.length > i; i++){
                    byte b = inBuf[i];//get on byte
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
                            System.out.println("LEN: " + (outBufPos-4));
                            String frame = getHex(Arrays.copyOfRange(outBuffer, 0, outBufPos - 4));
                            System.out.println("Data: " + frame + " EOF");
                            //process(outBuffer,outBufPos-4);
                            Thread.sleep((long)1000/10);
                            processFrame(outBuffer,outBufPos-4);
                        }
                        outBufPos = 4;

                        nzc = 0; //Reset 0x00 counter
                    } else {
                        nzc = 0;
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error processing file!",e);
        }
    }

    void processFrame(byte[] frameData, int len){

       Log.d(TAG, "NEXT");

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

            Log.d(TAG, " input inputBufIndex" + inputBufIndex + " LEN:" + len);
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
            Log.d(TAG, "SR " + outputBufferIndex);
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
                processFrame(naluBytes, len);
                break;
            case TYPE_NONE_IDR_FRAME:
                processFrame(naluBytes, len);
                break;
            case TYPE_SPP:
                processFrame(naluBytes, len);
                break;
            case TYPE_SPS:
                processFrame(naluBytes, len);
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
            throw new RuntimeException("First Bit mus be 0");
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
                throw new RuntimeException("New an unhandled nale_unit_type " + nal_unit_type);
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
