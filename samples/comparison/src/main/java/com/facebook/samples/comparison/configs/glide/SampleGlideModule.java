/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.samples.comparison.configs.glide;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;
import com.facebook.samples.comparison.configs.ConfigConstants;

/**
 * {@link com.bumptech.glide.module.GlideModule} implementation for the sample app.
 */
@GlideModule
public class SampleGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(final Context context, GlideBuilder builder) {
        builder.setDiskCache(
                new DiskCache.Factory() {
                    @Override
                    public DiskCache build() {
                        return DiskLruCacheWrapper.get(
                                Glide.getPhotoCacheDir(context),
                                ConfigConstants.MAX_DISK_CACHE_SIZE);
                    }
                });
        builder.setDecodeFormat(DecodeFormat.PREFER_ARGB_8888);//已经默认是ARGB_8888

        //控制图片的压缩质量
        //默认质量是90
        builder.setDefaultRequestOptions(new RequestOptions().downsample(DownsampleStrategy.NONE).encodeQuality(100)/*.encodeFormat()*/);//控制缩减采样率
//      builder.setDefaultRequestOptions()
        builder.setMemoryCache(new LruResourceCache(ConfigConstants.MAX_MEMORY_CACHE_SIZE));
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
