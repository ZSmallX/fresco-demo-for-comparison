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
import android.net.Uri;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.samples.comparison.Drawables;
import com.facebook.samples.comparison.instrumentation.InstrumentedDraweeView;
import com.facebook.samples.comparison.instrumentation.PerfListener;

/**
 * Populate the list view with images using the Fresco image pipeline.
 */
public class FrescoAdapter extends ImageListAdapter<InstrumentedDraweeView> {

    public FrescoAdapter(
            Context context,
            int resourceId,
            PerfListener perfListener, ImagePipelineConfig imagePipelineConfig
    ) {
        super(context, resourceId, perfListener);
//        Fresco.initialize(context, imagePipelineConfig);//取消此处的初始化
    }

    @Override
    protected Class<InstrumentedDraweeView> getViewClass() {
        return InstrumentedDraweeView.class;
    }

    protected InstrumentedDraweeView createView() {
        GenericDraweeHierarchy gdh = new GenericDraweeHierarchyBuilder(getContext().getResources())
                .setPlaceholderImage(Drawables.sPlaceholderDrawable)
                .setFailureImage(Drawables.sErrorDrawable)
//                .setRoundingParams(RoundingParams.asCircle())
//                .setProgressBarImage(new ProgressBarDrawable())
                .setFadeDuration(0)//取消动画
                .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)//默认是centerCrop，调整为不做任何scale
                .build();
        return new InstrumentedDraweeView(getContext(), gdh);
    }

    protected void bind(final InstrumentedDraweeView view, String uri) {
        ImageRequest imageRequest =
                ImageRequestBuilder.newBuilderWithSource(Uri.parse(uri))
//                        .setResizeOptions(new ResizeOptions(view.getLayoutParams().width, view.getLayoutParams().height))
//                        .setProgressiveRenderingEnabled(true)//暂时关闭这个Demo
//                        .disableDiskCache()
                        .build();
        DraweeController draweeController = Fresco.newDraweeControllerBuilder()
                .setImageRequest(imageRequest)
                .setOldController(view.getController())
                .setControllerListener(view.getListener())
//                .setAutoPlayAnimations(true)//取消动画播放选项
                .build();
        view.setController(draweeController);
    }

    @Override
    public void shutDown() {
        super.clear();
        Fresco.shutDown();
    }
}
