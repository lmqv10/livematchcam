package it.lmqv.livematchcam.utils

import android.graphics.Bitmap
import android.opengl.GLES20
import com.pedro.encoder.input.gl.render.filters.`object`.BaseObjectFilterRender
import com.pedro.encoder.utils.gl.GifStreamObject
import com.pedro.encoder.utils.gl.StreamObjectBase
import java.io.IOException
import java.io.InputStream

class ImagesLoopObjectFilterRender : BaseObjectFilterRender() {
    init {
        streamObject = UrlImagesObject()
    }

    override fun drawFilter() {
        super.drawFilter()
        //val position = (streamObject as UrlImagesObject).updateFrame(streamObjectTextureId.size)
        val position = (streamObject as UrlImagesObject).updateFrame()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, streamObjectTextureId[position])
        //Set alpha. 0f if no image loaded.
        GLES20.glUniform1f(uAlphaHandle, if (streamObjectTextureId[position] == -1) 0f else alpha)
    }

    @Throws(IOException::class)
    fun setUrls(inputStream: InputStream?) {
        (streamObject as GifStreamObject).load(inputStream)
        shouldLoad = true
    }
}

class UrlImagesObject : StreamObjectBase() {
    override fun getWidth(): Int {
        TODO("Not yet implemented")
    }

    override fun getHeight(): Int {
        TODO("Not yet implemented")
    }

    override fun updateFrame(): Int {
        TODO("Not yet implemented")
    }

    override fun recycle() {
        TODO("Not yet implemented")
    }

    override fun getNumFrames(): Int {
        TODO("Not yet implemented")
    }

    override fun getBitmaps(): Array<out Bitmap?>? {
        TODO("Not yet implemented")
    }
}

/*
* public class GifStreamObject extends StreamObjectBase {

  private static final String TAG = "GifStreamObject";

  private int numFrames;
  private Bitmap[] gifBitmaps;
  private int[] gifDelayFrames;
  private long startDelayFrame;
  private int currentGifFrame;

  public GifStreamObject() {
  }

  @Override
  public int getWidth() {
    return gifBitmaps != null ? gifBitmaps[0].getWidth() : 0;
  }

  @Override
  public int getHeight() {
    return gifBitmaps != null ? gifBitmaps[0].getHeight() : 0;
  }

  public void load(InputStream inputStreamGif) throws IOException {
    GifDecoder gifDecoder = new GifDecoder();
    if (gifDecoder.read(inputStreamGif, inputStreamGif.available()) == 0) {
      Log.i(TAG, "read gif ok");
      numFrames = gifDecoder.getFrameCount();
      gifDelayFrames = new int[numFrames];
      gifBitmaps = new Bitmap[numFrames];
      for (int i = 0; i < numFrames; i++) {
        gifDecoder.advance();
        gifBitmaps[i] = gifDecoder.getNextFrame();
        gifDelayFrames[i] = gifDecoder.getNextDelay();
      }
      Log.i(TAG, "finish load gif frames");
    } else {
      throw new IOException("Read gif error");
    }
  }

  @Override
  public void recycle() {
    if (gifBitmaps != null) {
      for (int i = 0; i < numFrames; i++) {
        if (gifBitmaps[i] != null && !gifBitmaps[i].isRecycled()) gifBitmaps[i].recycle();
      }
    }
  }

  @Override
  public int getNumFrames() {
    return numFrames;
  }

  @Override
  public Bitmap[] getBitmaps() {
    return gifBitmaps;
  }

  public int[] getGifDelayFrames() {
    return gifDelayFrames;
  }

  public int updateFrame(int size) {
    return size <= 1 ? 0 : updateFrame();
  }

  @Override
  public int updateFrame() {
    if (startDelayFrame == 0) {
      startDelayFrame = TimeUtils.getCurrentTimeMillis();
    }
    if (TimeUtils.getCurrentTimeMillis() - startDelayFrame >= gifDelayFrames[currentGifFrame]) {
      if (currentGifFrame >= numFrames - 1) {
        currentGifFrame = 0;
      } else {
        currentGifFrame++;
      }
      startDelayFrame = 0;
    }
    return currentGifFrame;
  }
}
*/