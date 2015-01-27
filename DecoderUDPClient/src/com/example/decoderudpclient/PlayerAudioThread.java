package com.example.decoderudpclient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.Format;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

public class PlayerAudioThread extends Thread {

	private static final String SAMPLE = Environment.getExternalStorageDirectory() + "/video2.mp4";
	private MediaExtractor extractorAudio;
	private MediaCodec decoderAudio;
	private AudioTrack audioTrack;
	private MediaFormat formatAudio; 

	@Override
	public void run() {
		
		extractorAudio = new MediaExtractor();
		
		try {
			extractorAudio.setDataSource(SAMPLE);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
				
		for (int i = 0; i < extractorAudio.getTrackCount(); i++) { // Compte le nombre de piste trouve dans le fichier
			
			MediaFormat format = extractorAudio.getTrackFormat(i); // Obtient le format de la piste courante
			String mime = format.getString(MediaFormat.KEY_MIME); // KEY-MIME donne le format du media.
			
			
			if(mime.startsWith("audio/")){
				Log.d(getName(),"The number of audio's track is : "+i+"  avec MIME ="+mime);
				formatAudio = extractorAudio.getTrackFormat(i);
				extractorAudio.selectTrack(i); // On s�lectionne alors cette piste
				try {
					decoderAudio = MediaCodec.createDecoderByType(mime); // On cr�e un d�coder associ� � ce type de format audio
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Log.d(this.getName(),"MIME = "+mime);
				decoderAudio.configure(formatAudio, null, null, 0); // On donne le MediaFOrmat et la surface au d�codeur
			}
			
		}
		
		int sampleRate = formatAudio.getInteger(MediaFormat.KEY_SAMPLE_RATE);
		audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM);
		audioTrack.play();

		decoderAudio.start(); // On lance le d�codage de l'audio
		
		ByteBuffer[] inputBuffersAudio = decoderAudio.getInputBuffers();
		ByteBuffer[] outputBuffersAudio = decoderAudio.getOutputBuffers();
		
		BufferInfo info = new BufferInfo();
		
		boolean isEOS = false; // Is End of Stream � false
		long startMs = System.currentTimeMillis(); // Temps de d�part
		

		while (!Thread.interrupted()) { // Tant que le Thread n'est pas interrompu
			
			
			if (!isEOS) {
				
				
				
				//AUDIO PART
				int inIndexAudio = decoderAudio.dequeueInputBuffer(10000); //Returns the index of an input buffer to be filled with valid data or -1 if no such buffer is currently available.
				
				if (inIndexAudio >= 0) {
					
					ByteBuffer buffer = inputBuffersAudio[inIndexAudio];
					int sampleSize = extractorAudio.readSampleData(buffer, 0); //Retrieve the current encoded sample and store it in the byte buffer starting at the given offset.
					
					if (sampleSize < 0) {
						// We shouldn't stop the playback at this point, just pass the EOS
						// flag to decoder, we will get it again from the
						// dequeueOutputBuffer
						Log.d("DecodeActivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM");
						decoderAudio.queueInputBuffer(inIndexAudio, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
						isEOS = true; // C'est la fin du stream
					} else {
				//		Log.d("DecodeActivity", "Sample time " +extractor.getSampleTime()+" ms");
						decoderAudio.queueInputBuffer(inIndexAudio, 0, sampleSize, extractorAudio.getSampleTime(), 0);//(int index, int offset, int size, long presentationTimeUs, int flags)
											//After filling a range of the input buffer at the specified index submit it to the component.
						extractorAudio.advance();//Advance to the next sample.
					}
					
				}
				
			}


			// AUDIO PART
			int outIndexAudio = decoderAudio.dequeueOutputBuffer(info, 10000);//Dequeue an output buffer, block at most "timeoutUs" microseconds.

			switch (outIndexAudio) {
			
				case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
					
					Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
					outputBuffersAudio = decoderAudio.getOutputBuffers();
					break;
					
				case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
					
					Log.d("DecodeActivity", "New format " + decoderAudio.getOutputFormat());
					break;
					
				case MediaCodec.INFO_TRY_AGAIN_LATER:
					
					Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
					break;
					
				default:
					
					ByteBuffer buffer = outputBuffersAudio[outIndexAudio];

			            final byte[] chunk = new byte[info.size]; 
			            buffer.get(chunk);
			            buffer.clear();

			            if (chunk.length > 0) {
			                audioTrack.write(chunk, 0, chunk.length); // Play the sound
			            }
					
					
			            //	Log.v("DecodeActivity", "We can't use this buffer but render it due to the API limit, " + buffer);
	
					// We use a very simple clock to keep the video FPS, or the video
					// playback will be too fast
			            
					while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) { //time in microsecond for info.presentationTimeUs
						try {
							sleep(10);
						} catch (InterruptedException e) {
							e.printStackTrace();
							break;
						}
					}
					
					decoderAudio.releaseOutputBuffer(outIndexAudio, true);
					break;
					
			}// on recommence
			
			
			// All decoded frames have been rendered, we can stop playing now
			if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
			//	Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
				break;
			}
			
			
			
		}
		
	    audioTrack.flush();
	    audioTrack.release();
	    audioTrack = null;	    
		decoderAudio.stop();
		decoderAudio.release();
		extractorAudio.release();
	}
}
