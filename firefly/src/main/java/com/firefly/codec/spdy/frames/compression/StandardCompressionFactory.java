package com.firefly.codec.spdy.frames.compression;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.ZipException;

import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class StandardCompressionFactory implements CompressionFactory {
	
	@Override
	public Compressor newCompressor() {
		return new StandardCompressor();
	}

	@Override
	public Decompressor newDecompressor() {
		return new StandardDecompressor();
	}

	public static class StandardCompressor implements Compressor {
		private final Deflater deflater = new Deflater();

		@Override
		public void setInput(byte[] input) {
			deflater.setInput(input);
		}

		@Override
		public void setDictionary(byte[] dictionary) {
			deflater.setDictionary(dictionary);
		}

		@Override
		public int compress(byte[] output) {
			return deflater.deflate(output, 0, output.length, Deflater.SYNC_FLUSH);
		}

		@Override
		public ByteBuffer compressToByteBuffer(byte[] bytes) {
			ByteArrayOutputStream compressed = new ByteArrayOutputStream(bytes.length);
			setInput(bytes);

			// Compressed bytes may be bigger than input bytes, so we need to loop and accumulate them
	        // Beware that the minimum amount of bytes generated by the compressor is few bytes, so we
	        // need to use an output buffer that is big enough to exit the compress loop
			byte[] output = new byte[Math.max(256, bytes.length)];
			while(true) {
				// SPDY uses the SYNC_FLUSH mode
				int count = compress(output);
				compressed.write(output, 0, count);
				if(count < output.length)
					break;
			}
			return ByteBuffer.wrap(compressed.toByteArray());
		}

		@Override
		public void close() throws IOException {
			deflater.end();
		}
	}

	public static class StandardDecompressor implements CompressionFactory.Decompressor {
		
		private static final Log log = LogFactory.getInstance().getLog("firefly-system");
		
		private final Inflater inflater = new Inflater();
		private byte[] defaultDictionary;

		@Override
		public void setDefaultDictionary(byte[] defaultDictionary) {
			this.defaultDictionary = defaultDictionary;
		}

		@Override
		public void setDictionary(byte[] dictionary) {
			inflater.setDictionary(dictionary);
		}

		@Override
		public void setInput(byte[] input) {
			inflater.setInput(input);
		}

		@Override
		public int decompress(byte[] output) throws ZipException {
			try {
				return inflater.inflate(output);
			} catch (DataFormatException x) {
				throw (ZipException) new ZipException().initCause(x);
			}
		}

		@Override
		public ByteBuffer decompressToByteBuffer(byte[] compressed) throws ZipException {
			ByteArrayOutputStream decompressed = new ByteArrayOutputStream(compressed.length * 2);
			setInput(compressed);
			byte[] buffer = new byte[compressed.length * 2];
			
			while(true) {
				int count = decompress(buffer);
				if(count == 0) {
					if(inflater.needsDictionary()) {
						setDictionary(defaultDictionary);
						log.debug("This decompressor needs dictionary!");
						continue;
					}
					
					if(decompressed.size() > 0)
						break;
					else
						throw new IllegalStateException();
				} 
				
				decompressed.write(buffer, 0, count);
				if(count < buffer.length)
					break;
				
				buffer = new byte[compressed.length * 2];
			}
			return ByteBuffer.wrap(decompressed.toByteArray());
		}

		@Override
		public void close() throws IOException {
			inflater.end();
		}
	}
}
