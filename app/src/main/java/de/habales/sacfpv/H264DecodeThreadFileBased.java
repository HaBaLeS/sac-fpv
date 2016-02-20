package de.habales.sacfpv;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class H264DecodeThreadFileBased extends Thread {

    static final String TAG = "DCT";
    static final String H264_MIME = "video/avc";

    private final static int UDP_MAX_SIZE = 256*256;
    private final static byte[] inBuf = new byte[UDP_MAX_SIZE];

    private Surface surfaceToDrawOn;
    private MediaCodec decoder;
    private MediaCodec.BufferInfo bi;

    private ByteBuffer[] inputBuffers;

    /**
     * Construct a new decoder thread for Surface
     * @param surfaceToDrawOn
     */
    public H264DecodeThreadFileBased(Surface surfaceToDrawOn){
        this.surfaceToDrawOn = surfaceToDrawOn;
    }

    @Override
    public void run() {



        try {

            String filePath = Environment.getExternalStorageDirectory().getPath() + "/vv/somefile.264"; //???
            //String filePath = Environment.getExternalStorageDirectory().getPath() + "/rpi960mal810.h264"; //???

            FileInputStream fin = new FileInputStream(filePath);

            MediaFormat fmt = getMediaFormat(1920,1080,30);

            MediaCodec.BufferInfo bi = new MediaCodec.BufferInfo();
            decoder = MediaCodec.createDecoderByType(H264_MIME);
            decoder.configure(fmt, surfaceToDrawOn, null, 0); //Flag to set ENCODER not DECODER
            decoder.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            decoder.start();

            //NEEDED IN SDK < 20
            inputBuffers = decoder.getInputBuffers();

            int outSize = 1024*1024;
            byte[] outBuffer = new byte[outSize];
            //outBuffer[0] = 0x00;
            //outBuffer[1] = 0x00;
            //outBuffer[2] = 0x00;
            //outBuffer[3] = 0x01;

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
                        //System.out.println("Data: " + getHex(Arrays.copyOfRange(outBuffer, 0, outBufPos)));
                        processNALU(outBuffer, 0, outBufPos);
                        outBufPos = 0;

                        nzc = 0; //Reset 0x00 counter
                    } else {
                        outBuffer[outBufPos] = b; //copy to outBuffer
                        outBufPos++;
                        nzc = 0;
                    }
                }
            }

            fin.close();

        } catch (Exception e) {
            throw new RuntimeException("Error processing file!",e);
        }
    }

    private void processNALU(byte[] outBuffer, int i, int outBufPos) {
         /*
                In broad terms, a codec processes input data to generate output data.
                It processes data asynchronously and uses a set of input and output buffers.
                At a simplistic level, you request (or receive) an empty input buffer, fill it
                up with data and send it to the codec for processing. The codec uses
                up the data and transforms it into one of its empty output buffers.
                Finally, you request (or receive) a filled output buffer, consume its
                contents and release it back to the codec.
                 */
        int bufferTimeout = 1000;
        int inputBufIndex = decoder.dequeueInputBuffer(bufferTimeout);
        if (inputBufIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufIndex]; //changed with android >= 20
            inputBuffer.put(outBuffer,0,outBufPos);
            decoder.queueInputBuffer(inputBufIndex, 0, outBufPos, 0, 0); //Set data back to codec
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
        int outputBufferIndex = decoder.dequeueOutputBuffer(bi, bufferTimeout);
        if (outputBufferIndex >= 0) {
            Log.d(TAG, "SR " + outputBufferIndex);
            //ByteBuffer outputBuffer = decoder.getOutputBuffers()[outputBufferIndex]; not needed
            decoder.releaseOutputBuffer(outputBufferIndex, true);
        } else {
            Log.d(TAG, "NR No output buffer" + outputBufferIndex);
        }
    }




    public MediaFormat getMediaFormat(int w, int h, int fps) {
        MediaFormat format = MediaFormat.createVideoFormat(H264_MIME, w, h);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
        return format;
    }

}
