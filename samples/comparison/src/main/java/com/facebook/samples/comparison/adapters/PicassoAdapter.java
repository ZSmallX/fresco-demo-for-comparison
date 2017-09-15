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

import com.facebook.samples.comparison.Drawables;
import com.facebook.samples.comparison.MyApplication;
import com.facebook.samples.comparison.instrumentation.InstrumentedImageView;
import com.facebook.samples.comparison.instrumentation.PerfListener;
import com.squareup.picasso.Picasso;

/**
 * Populate the list view with images using the Picasso library.
 */
public class PicassoAdapter extends ImageListAdapter<InstrumentedImageView> {
    private final Picasso mPicasso;

    public PicassoAdapter(Context context, int resourceId, PerfListener perfListener) {
        super(context, resourceId, perfListener);
        mPicasso = MyApplication.sPicasso;
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
        mPicasso
                .load(uri)
                .placeholder(Drawables.sPlaceholderDrawable)
                .error(Drawables.sErrorDrawable)
                .noFade()
//                .config()//默认使用Android BitmapFactory的缩减采样率，即不缩减采样
//                .fit()
//                .resize(view.getLayoutParams().width,view.getLayoutParams().height)
//                .memoryPolicy(MemoryPolicy.NO_CACHE,MemoryPolicy.NO_STORE)
//                .centerCrop()//需要和resize一起使用
                .into(view);
    }

    @Override
    public void shutDown() {
        for (int i = 0; i < getCount(); i++) {
            String uri = getItem(i);
            mPicasso.invalidate(uri);
        }
        super.clear();
    }
}
