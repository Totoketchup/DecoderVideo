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

public class PlayerThread extends Thread {
	
	private static final String SAMPLE = Environment.getExternalStorageDirectory() + "/video2.mp4";
	
	private MediaExtractor extractorVideo;
	private MediaCodec decoderVideo;
	private Surface surface;
	
	public PlayerThread(Surface surface) {
		this.surface = surface;
	}

	@Override
	public void run() {
			
		extractorVideo = new MediaExtractor(); // Creation du media extractor		
		
		try {
			extractorVideo.setDataSource(SAMPLE); // On dit a l'extracteur quel fichier on va analyser
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		Log.d(getName(),"The number of tracks is : "+extractorVideo.getTrackCount());
		
		for (int i = 0; i < extractorVideo.getTrackCount(); i++) { // Compte le nombre de piste trouve dans le fichier
			
			MediaFormat format = extractorVideo.getTrackFormat(i); // Obtient le format de la piste courante
			String mime = format.getString(MediaFormat.KEY_MIME); // KEY-MIME donne le format du media.
			
			if (mime.startsWith("video/")) { // Si on a "video/" , c'est qu'on a affaire a la piste video
				Log.d(getName(),"The number of video's track is : "+i+"  avec MIME ="+mime);
				extractorVideo.selectTrack(i); // On selectionne alors cette piste
				try {
					decoderVideo = MediaCodec.createDecoderByType(mime); // On cree un decoder associ� � ce type de format video
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Log.d(this.getName(),"MIME = "+mime);
				decoderVideo.configure(format, surface, null, 0); // On donne le MediaFOrmat et la surface au d�codeur
				//break;
			}
		}

		if (decoderVideo == null ) {
			Log.e("DecodeActivity", "Can't find video info!");
			return;
		}
		

		decoderVideo.start(); // On lance le d�codage de la vid�o

		
		ByteBuffer[] inputBuffers = decoderVideo.getInputBuffers();
		ByteBuffer[] outputBuffers = decoderVideo.getOutputBuffers();

		BufferInfo info = new BufferInfo();
		
		boolean isEOS = false; // Is End of Stream � false
		long startMs = System.currentTimeMillis(); // Temps de d�part
		

		while (!Thread.interrupted()) { // Tant que le Thread n'est pas interrompu
			
			
			if (!isEOS) {
				
				
				// VIDEO PART
				int inIndex = decoderVideo.dequeueInputBuffer(10000); //Returns the index of an input buffer to be filled with valid data or -1 if no such buffer is currently available.
				
				
				if (inIndex >= 0) {
					
					ByteBuffer buffer = inputBuffers[inIndex];
					int sampleSize = extractorVideo.readSampleData(buffer, 0); //Retrieve the current encoded sample and store it in the byte buffer starting at the given offset.
					
					if (sampleSize < 0) {
						// We shouldn't stop the playback at this point, just pass the EOS
						// flag to decoder, we will get it again from the
						// dequeueOutputBuffer
						Log.d("DecodeActivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM");
						decoderVideo.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
						isEOS = true; // C'est la fin du stream
					} else {
						//Log.d("DecodeActivity", "Sample time " +extractor.getSampleTime()+" ms");
						decoderVideo.queueInputBuffer(inIndex, 0, sampleSize, extractorVideo.getSampleTime(), 0);//(int index, int offset, int size, long presentationTimeUs, int flags)
											//After filling a range of the input buffer at the specified index submit it to the component.
						extractorVideo.advance();//Advance to the next sample.
					}
					
				}
				else{ //No input buffer found
					Log.d(getName(),inIndex+"");
				}
				
			}

			// VIDEO PART
			int outIndex = decoderVideo.dequeueOutputBuffer(info, 10000);//Dequeue an output buffer, block at most "timeoutUs" microseconds.

			switch (outIndex) {
			
				case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
					
					Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
					outputBuffers = decoderVideo.getOutputBuffers();
					break;
					
				case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
					
					Log.d("DecodeActivity", "New format " + decoderVideo.getOutputFormat());
					break;
					
				case MediaCodec.INFO_TRY_AGAIN_LATER:
					
					Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
					break;
					
				default:
					
					ByteBuffer buffer = outputBuffers[outIndex];
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
					
					decoderVideo.releaseOutputBuffer(outIndex, true);
					break;
					
			}// on recommence


			// All decoded frames have been rendered, we can stop playing now
			if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
			//	Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
				break;
			}
			
			
			
		}
	   
		decoderVideo.stop();
		decoderVideo.release();
		extractorVideo.release();
	}
}
