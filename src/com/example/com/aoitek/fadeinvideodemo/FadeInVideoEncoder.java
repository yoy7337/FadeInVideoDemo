
package com.example.com.aoitek.fadeinvideodemo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.vision.face.FaceDetector;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/*
 * Encode several images to a video(mp4)
 * 1. Images to H.264 raw video by MediaCodec(API 16)
 * 2. H.264 raw video to mp4 by MediaMuxer(API 18)
 */

@SuppressLint("NewApi")
public class FadeInVideoEncoder {
    private static final String TAG = "FadeInVideoEncoder";

    private static final int TIMEOUT_USEC = 10000;
    private static final long ONE_SECOND_PER_USEC = 1000000;

    // H.264 Advanced Video Coding
    private static final String VIDEO_MIME_TYPE = "video/avc";
    private static final float IMAGE_CHANGE_INTERVAL = (float) 1.5;
    private static final int FRAME_RATE = 10;
    private static final int IFRAME_INTERVAL = (int) (IMAGE_CHANGE_INTERVAL * FRAME_RATE);
    private static final int BIT_RATE = 1024 * 1024;
    // audio coding
    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";
    public static final int COMPRESSED_AUDIO_FILE_BIT_RATE = 128000; // 320kbps
    public static final int SAMPLING_RATE = 44100;
    public static final int BUFFER_SIZE = SAMPLING_RATE * 2;

    // encode to H.264 raw file or mp4
    private static final boolean ENCODE_TO_RAW_FILE = false;

    private static final int MAX_ALPHA_VAL = 255;
    private static final int MIN_ALPHA_VAL = 0;

    private final int mVideoWidth;
    private final int mVideoHeight;
    private final String mOutputPath;

    private MediaCodec mVideoEncoder;
    private MediaCodec mAudioEncoder;

    private MediaMuxer mMuxer;
    private int mTrackVideoIndex;
    private int mTrackAudioIndex;

    // allocate one of these up front so we don't need to do it every time
    private MediaCodec.BufferInfo mBufferInfo;

    private final FaceFadeInBitmapGenerator mFaceFadeInBitmapGenerator;
    private final FaceDetector mFaceDetector;

    private MediaFormat mVideoMediaFormat;
    private MediaFormat mAudioMediaFormat;

    private Callback mCallback;

    public FadeInVideoEncoder(Context context, int videoWidth, int videoHeight, String outputPath, FaceDetector faceDetector,
            Callback callback) {

        mVideoWidth = videoWidth;
        mVideoHeight = videoHeight;
        mOutputPath = outputPath;
        mFaceDetector = faceDetector;
        mCallback = callback;

        mFaceFadeInBitmapGenerator = new FaceFadeInBitmapGenerator(context, videoWidth, videoHeight, faceDetector);

        prepareEncoder();
    }

    private void prepareEncoder() {
        File f = new File(mOutputPath);
        try {
            VideoEncodeUtils.touch(f);
        } catch (IOException e1) {
            Log.e(TAG, "can not touch file");
            e1.printStackTrace();
        }

        try {
            MediaCodecInfo videoCodecInfo = selectCodec(VIDEO_MIME_TYPE);
            if (videoCodecInfo == null) {
                // Don't fail CTS if they don't have an AVC codec (not here, anyway).
                Log.e(TAG, "Unable to find an appropriate codec for " + VIDEO_MIME_TYPE);
                return;
            }

            MediaCodecInfo audioCodecInfo = selectCodec(AUDIO_MIME_TYPE);
            if (audioCodecInfo == null) {
                // Don't fail CTS if they don't have an AVC codec (not here, anyway).
                Log.e(TAG, "Unable to find an appropriate codec for " + AUDIO_MIME_TYPE);
                return;
            }

            // for video
            mVideoMediaFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, mVideoWidth,
                    mVideoHeight);
            mVideoMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
            mVideoMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            mVideoMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
            mVideoMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

            mVideoEncoder = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
            mVideoEncoder.configure(mVideoMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mVideoEncoder.start();

            // for audio
            mAudioMediaFormat = MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, SAMPLING_RATE, 2);
            mAudioMediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            mAudioMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, COMPRESSED_AUDIO_FILE_BIT_RATE);
            mAudioMediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, BUFFER_SIZE);

            mAudioEncoder = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);
            mAudioEncoder.configure(mAudioMediaFormat, null, null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);
            mAudioEncoder.start();

            mBufferInfo = new MediaCodec.BufferInfo();

            mMuxer = new MediaMuxer(f.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        } catch (IOException e) {
            Log.e(TAG, "can not set media codec");
            e.printStackTrace();
        }
    }

    public void encodeToMp4(List<String> imagePathList, String audioPath) throws IOException {
        int frameIndex = 0;

        // return progress to caller
        final double progressStep = (100.0) / ((imagePathList.size()) * MAX_ALPHA_VAL / (FRAME_RATE * IMAGE_CHANGE_INTERVAL));
        double currentProgress = 0;

        byte[] inputFrame = null;

        initMuxer();

        // ++ audio
        offerAudioEncoder(audioPath, (long) (imagePathList.size() * IMAGE_CHANGE_INTERVAL * ONE_SECOND_PER_USEC));
        // -- audio

        final float alphaStep = (int) (MAX_ALPHA_VAL / (FRAME_RATE * IMAGE_CHANGE_INTERVAL));
        Log.d("yoy", "alphaStep = " + alphaStep);
        for (String imagePath : imagePathList) {
            mFaceFadeInBitmapGenerator.setFadeImage(imagePath);
            float alpha = MIN_ALPHA_VAL;
            while (alpha < MAX_ALPHA_VAL) {
                alpha += alphaStep;
                Log.d("yoy", "imagePath =" + imagePath +
                        ", alpha=" + alpha);
                if (alpha >= MAX_ALPHA_VAL) {
                    alpha = MAX_ALPHA_VAL;
                }
                Bitmap scaledBitmap = mFaceFadeInBitmapGenerator.genFadeInImage((int) alpha);
                if (scaledBitmap == null) {
                    Log.e(TAG, "Can not open image: " + imagePath);
                    continue;
                }
                // encode frame
                inputFrame = VideoEncodeUtils.getNV21(mVideoWidth, mVideoHeight, scaledBitmap);
                offerVideoEncoder(inputFrame, frameIndex, false);
                frameIndex++; // update progress
                currentProgress += progressStep;
                if (currentProgress > 100) {
                    currentProgress = 100;
                }
                if (mCallback != null) {
                    mCallback.onProgressUpdate((int) currentProgress);
                }
            }
        }
        // encode end frame
        offerVideoEncoder(null, frameIndex, true);

        // update progress
        currentProgress = 100;
        if (mCallback != null) {
            mCallback.onProgressUpdate((int) currentProgress);
        }

        release();
    }

    /*
     * first we should init muxer Add audio and video tracks then start track
     */
    private void initMuxer() {
        int outputBufferIndex = 0;
        int inputBufferIndex = 0;

        inputBufferIndex = mAudioEncoder.dequeueInputBuffer(-1);
        mAudioEncoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);

        while (outputBufferIndex != MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            outputBufferIndex = mAudioEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            Log.d("yoy", "@audio outputBufferIndex = " + outputBufferIndex);
        }

        MediaFormat newAudioFormat = mAudioEncoder.getOutputFormat();
        mTrackAudioIndex = mMuxer.addTrack(newAudioFormat);

        outputBufferIndex = 0;
        inputBufferIndex = mVideoEncoder.dequeueInputBuffer(-1);
        mVideoEncoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);

        while (outputBufferIndex != MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            outputBufferIndex = mVideoEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            Log.d("yoy", "@video outputBufferIndex = " + outputBufferIndex);
        }

        MediaFormat newVideoFormat = mVideoEncoder.getOutputFormat();
        mTrackVideoIndex = mMuxer.addTrack(newVideoFormat);

        mMuxer.start();
    }

    private void offerAudioEncoder(String audioPath, long totalTimeUs) throws IOException {
        int totalBytesRead = 0;
        byte[] tempBuffer = new byte[BUFFER_SIZE];
        long presentationTimeUs = 0;

        File audioFile = new File(audioPath);
        FileInputStream fis = new FileInputStream(audioFile);

        while (presentationTimeUs <= totalTimeUs) {

            Log.d("yoy", "presentationTimeUs = " + presentationTimeUs);
            // 1. write audio to input buffer
            ByteBuffer[] inputBuffers = mAudioEncoder.getInputBuffers();
            ByteBuffer[] outputBuffers = mAudioEncoder.getOutputBuffers();
            int inputBufferIndex = mAudioEncoder.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    inputBuffer = mAudioEncoder.getInputBuffer(inputBufferIndex);
                } else {
                    inputBuffer = inputBuffers[inputBufferIndex];
                }

                inputBuffer.clear();
                Log.d("yoy", "inputBuffer.limit() = " + inputBuffer.limit());
                int bytesRead = fis.read(tempBuffer, 0, inputBuffer.limit());

                if (bytesRead > 0) {
                    totalBytesRead += bytesRead;
                    inputBuffer.put(tempBuffer);
                    Log.d("yoy", "presentationTimeUs =" + presentationTimeUs + ", totalTimeUs=" + totalTimeUs);

                    if (presentationTimeUs >= totalTimeUs) {
                        mAudioEncoder
                                .queueInputBuffer(inputBufferIndex, 0, bytesRead, presentationTimeUs, MediaCodec.BUFFER_FLAG_KEY_FRAME);
                    } else {
                        mAudioEncoder
                                .queueInputBuffer(inputBufferIndex, 0, bytesRead, presentationTimeUs, MediaCodec.BUFFER_FLAG_KEY_FRAME);
                    }
                    presentationTimeUs = ONE_SECOND_PER_USEC * (totalBytesRead / 4) / SAMPLING_RATE;
                } else {
                    mAudioEncoder.queueInputBuffer(inputBufferIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    // file end...
                    break;
                }

                ByteBuffer outputBuffer = null;

                while (true) {
                    int outputBufferIndex = mAudioEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);

                    if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break;
                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED
                            && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        outputBuffers = mAudioEncoder.getOutputBuffers();
                        outputBufferIndex = mAudioEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                        Log.d(TAG, "@audio outputBufferIndex changed");
                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat newFormat = mAudioEncoder.getOutputFormat();
                        Log.d(TAG, "@audio encoder output format changed: " + newFormat);
                    } else if (outputBufferIndex < 0) {
                        Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: "
                                + outputBufferIndex);
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            outputBuffer = mAudioEncoder.getOutputBuffer(outputBufferIndex);
                        } else {
                            outputBuffer = outputBuffers[outputBufferIndex];
                        }

                        outputBuffer.position(mBufferInfo.offset);
                        outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);

                        // write to mp4
                        mMuxer.writeSampleData(mTrackAudioIndex, outputBuffer, mBufferInfo);

                        mAudioEncoder.releaseOutputBuffer(outputBufferIndex, false);

                        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break;
                        }
                    }
                }
            }
        }
    }

    private void offerVideoEncoder(byte[] input, int frameIndex, boolean finished) {

        try {

            long presentationTimeNsec = computePresentationTimeNsec(frameIndex);
            Log.d("yoy", "write video frame " + frameIndex + ", presentationTimeNsec=" + presentationTimeNsec);

            // 1. write bitmap to input buffer
            ByteBuffer[] inputBuffers = mVideoEncoder.getInputBuffers();
            ByteBuffer[] outputBuffers = mVideoEncoder.getOutputBuffers();
            int inputBufferIndex = mVideoEncoder.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0) {

                if (input != null) {
                    ByteBuffer inputBuffer = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        inputBuffer = mVideoEncoder.getInputBuffer(inputBufferIndex);
                    } else {
                        inputBuffer = inputBuffers[inputBufferIndex];
                    }
                    inputBuffer.clear();
                    inputBuffer.put(input);
                }

                mVideoEncoder.queueInputBuffer(inputBufferIndex
                        , 0
                        , input == null ? 0 : input.length,
                        presentationTimeNsec,
                        finished ? MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                : MediaCodec.BUFFER_FLAG_KEY_FRAME);
            }

            // 2. write data to mp4 or H.264 raw
            ByteBuffer outputBuffer = null;

            while (true) {
                int outputBufferIndex = mVideoEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);

                if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (!finished) {
                        break;
                    } else {
                        Log.d(TAG, "no output available, finished");
                    }
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED
                        && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    outputBuffers = mVideoEncoder.getOutputBuffers();
                    outputBufferIndex = mVideoEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                    Log.d("yoy", "@video INFO_OUTPUT_BUFFERS_CHANGED");
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = mVideoEncoder.getOutputFormat();
                    Log.d(TAG, "@video encoder output format changed: " + newFormat);
                } else if (outputBufferIndex < 0) {
                    Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: "
                            + outputBufferIndex);
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        outputBuffer = mVideoEncoder.getOutputBuffer(outputBufferIndex);
                    } else {
                        outputBuffer = outputBuffers[outputBufferIndex];
                    }

                    outputBuffer.position(mBufferInfo.offset);
                    outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);

                    // write to mp4
                    mMuxer.writeSampleData(mTrackVideoIndex, outputBuffer, mBufferInfo);

                    mVideoEncoder.releaseOutputBuffer(outputBufferIndex, false);

                    if (finished && (mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d("yoy", "write end to video");
                        break;
                    }
                }
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void release() {
        try {
            if (mAudioEncoder != null) {
                mAudioEncoder.flush();
                mAudioEncoder.stop();
                mAudioEncoder.release();
            }

            if (mVideoEncoder != null) {
                mVideoEncoder.flush();
                mVideoEncoder.stop();
                mVideoEncoder.release();
            }

            if (mMuxer != null) {
                mMuxer.stop();
                mMuxer.release();
                mMuxer = null;
            }

            if (mFaceFadeInBitmapGenerator != null) {
                mFaceFadeInBitmapGenerator.release(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates the presentation time for frame N, in nanoseconds.
     */
    private static long computePresentationTimeNsec(int frameIndex) {
        return (frameIndex + 0) * ONE_SECOND_PER_USEC / FRAME_RATE;
    }

    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    private static int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }

        Log.e(TAG, "couldn't find a good color format for " + codecInfo.getName() + " / "
                + mimeType);
        return 0; // not reached
    }

    /**
     * Returns true if this is a color format that this test code understands (i.e. we know how to
     * read and generate frames in this format).
     */
    private static boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
        // these are the formats we know how to handle for this test
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                return true;
            default:
                return false;
        }
    }

    public interface Callback {
        void onProgressUpdate(int progress);
    }
}
