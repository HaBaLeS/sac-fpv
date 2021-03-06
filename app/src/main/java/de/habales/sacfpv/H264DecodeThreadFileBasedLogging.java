package de.habales.sacfpv;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOError;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class H264DecodeThreadFileBasedLogging extends Thread {

    static final String TAG = "DCT";
    static final String H264_MIME = "video/avc";
    private static final String HEXES = "0123456789ABCDEF";

    private Surface surfaceToDrawOn;
    private MediaExtractor mediaExtractor;

    int frameCnt = 0;

    /**
     * Construct a new decoder thread for Surface
     * @param surfaceToDrawOn
     */
    public H264DecodeThreadFileBasedLogging(Surface surfaceToDrawOn){
        this.surfaceToDrawOn = surfaceToDrawOn;
    }

    @Override
    public void run() {

        try {

            initMediaExtractor();

            //MediaFormat fmt = getMediaFormat(640,480,25);
            MediaFormat fmt;// = getMediaFormat(1920,1088,30);

            fmt = mediaExtractor.getTrackFormat(0);
            //fmt.setString(MediaFormat.KEY_FRAME_RATE, null);
            //MediaFormat fmt = getMediaFormat(640,480,25);
            //MediaFormat fmt = getMediaFormat(640,480,25);

            ByteBuffer sps = fmt.getByteBuffer("csd-0");
            ByteBuffer spp = fmt.getByteBuffer("csd-1");

            String SPS = getHex(sps.array());
            String SPP = getHex(spp.array());


            MediaFormat format = mediaExtractor.getTrackFormat(0);
            String mime = format.getString(MediaFormat.KEY_MIME);

            MediaCodec.BufferInfo bi = new MediaCodec.BufferInfo();
            MediaCodec decoder = MediaCodec.createDecoderByType(mime);
            decoder.configure(fmt, surfaceToDrawOn, null, 0); //Flag to set ENCODER not DECODER
            decoder.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            decoder.start();

            //NEEDED IN SDK < 20
            ByteBuffer[] inputBuffers = decoder.getInputBuffers();
            ByteBuffer[] outputBuffers = decoder.getOutputBuffers();

            ByteBuffer inb = ByteBuffer.allocate(2*1024*1024);
            inb.order(ByteOrder.LITTLE_ENDIAN);

            while(true) {
                // Thread.sleep(20); enable for 25FPS
                mediaExtractor.advance();
                frameCnt++;
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
                int bufferTimeout = 1000;
                int inputBufIndex = decoder.dequeueInputBuffer(bufferTimeout);
                int len = -1;
                if (inputBufIndex >= 0) {
                    ByteBuffer inputBuffer = inputBuffers[inputBufIndex]; //changed with android >= 20
                    len = fillInputBuffer(inputBuffer);
                    byte[] mv = new byte[len];
                    inputBuffer.get(mv);
                    inputBuffer.rewind();

                    String frame = getHex(mv);
                    boolean multi = frame.substring(10).contains("000001");
                    if(multi){
                        //throw new RuntimeException("Multi\n" + frame);
                    }
                    Log.d(TAG, "run: Frame " + frameCnt);

                    if(len == -1 ){
                        Log.d(TAG, "No more Samples in Media");
                    } else {
                        decoder.queueInputBuffer(inputBufIndex, 0, len, 0, 0); //Set data back to codec
                    }
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
        } catch (Exception e) {
            throw new RuntimeException("Error processing file!",e);
        }
    }


    /**
     * Initialize a MediaExctractor if needed
     * @throws IOException
     */
    private void initMediaExtractor() throws IOException{
        String filePath = Environment.getExternalStorageDirectory().getPath() + "/rpi960mal810.h264";
        //String filePath = "/storage/sdcard/rpi960mal810.h264";
        //String filePath = Environment.getExternalStorageDirectory().getPath() + "/somefile2.264"; //???
        //String filePath = "/storage/external_SD/movies/scut1.avi"; //???
        //String filePath = Environment.getExternalStorageDirectory().getPath() + "/vv/cat.h264";
        //String filePath = Environment.getExternalStorageDirectory().getPath() + "/vv/ravi2.264";
        //String filePath = Environment.getExternalStorageDirectory().getPath() + "/testfile.264";
        //String filePath = Environment.getExternalStorageDirectory().getPath() + "/vv/cat90.h264";

        File f = new File(filePath);
        boolean ext =  f.exists();


        mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(filePath);
        int trackCount = mediaExtractor.getTrackCount();
        if(trackCount != 1){
            // throw new RuntimeException("TrackCount = " + trackCount + ". Only Single Track is supported");
        }
        mediaExtractor.selectTrack(0);
    }

    /**
     * Fill input from MediaExtractor
     * @param inputBuffer
     * @return
     */
    private int fillInputBuffer(ByteBuffer inputBuffer) {
        return mediaExtractor.readSampleData(inputBuffer, 0);//Read form mediaExctractor
    }

    public MediaFormat getMediaFormat(int w, int h, int fps) {
        MediaFormat format = MediaFormat.createVideoFormat(H264_MIME, w, h);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
        return format;
    }


    static String getHex(byte[] raw) {
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }
}
