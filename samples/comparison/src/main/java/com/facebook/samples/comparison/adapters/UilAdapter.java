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

package com.facebook.samples.comparison.adapters;

import android.content.Context;

import com.facebook.samples.comparison.MyApplication;
import com.facebook.samples.comparison.instrumentation.InstrumentedImageView;
import com.facebook.samples.comparison.instrumentation.PerfListener;
import com.nostra13.universalimageloader.core.ImageLoader;

/**
 * Populate the list view with images using the Universal Image Loader library.
 */
public class UilAdapter extends ImageListAdapter<InstrumentedImageView> {

    private final ImageLoader mImageLoader;

    public UilAdapter(Context context, int resourceId, PerfListener perfListener) {
        super(context, resourceId, perfListener);
        mImageLoader = MyApplication.sImageLoader;
    }

    @Override
    protected Class<InstrumentedImageView> getViewClass() {
        return InstrumentedImageView.class;
    }

    @Override
    protected InstrumentedImageView createView() {
        return new InstrumentedImageView(getContext());
    }

    @Override
    protected void bind(InstrumentedImageView view, String uri) {
        mImageLoader.displayImage(uri, view);
    }

    @Override
    public void shutDown() {
        super.clear();
        mImageLoader.clearMemoryCache();
    }
}
