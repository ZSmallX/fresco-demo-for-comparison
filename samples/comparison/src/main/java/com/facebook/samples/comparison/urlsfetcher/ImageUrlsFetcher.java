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

package com.facebook.samples.comparison.urlsfetcher;

import android.os.AsyncTask;
import android.util.Log;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import static com.facebook.samples.comparison.MainActivity.mAllowAnimations;
import static com.facebook.samples.comparison.MainActivity.mUseJPEG;
import static com.facebook.samples.comparison.MainActivity.mUsePNG;

/**
 * Helper that asynchronously fetches the list of image URIs from Imgur.
 */
public class ImageUrlsFetcher {

    /**
     * Imgur license key for use by the Fresco project.
     * <p>
     * The rest of this class may be used freely according to the licence file, with the sole
     * exception of this variable. Any fork of this code or use in any other application,
     * whether open- or closed-source, must use a different client ID obtained from Imgur.
     * See the <a href="https://api.imgur.com/#register">Imgur API documentation</a>.
     */
    private static final String IMGUR_CLIENT_ID = "Client-ID ccc6ca6a65ecdd8";

    private static final String TAG = "FrescoSample";

    public interface Callback {
        public void onFinish(List<String> results);
    }

    public static void getImageUrls(final ImageUrlsRequest request, final Callback callback) {
        new AsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... params) {
                return getImageUrls(request);
            }

            @Override
            protected void onPostExecute(List<String> result) {
                callback.onFinish(result);
            }
        }.execute();
    }

    private static List<String> getImageUrls(ImageUrlsRequest request) {
        List<String> urls = new ArrayList<String>();
//    urls.add("http://pooyak.com/p/progjpeg/jpegload.cgi?o=3");//渐进式 DEMO
//    urls.add("http://www.reasoft.com/tutorials/web/img/progress.jpg");
        try {
            String rawJson = downloadContentAsString(request.getEndpointUrl());

            ImageUrlsRequest imageUrlsRequest1 = getImageUrlsRequestByPager(1);
            ImageUrlsRequest imageUrlsRequest2 = getImageUrlsRequestByPager(2);
            ImageUrlsRequest imageUrlsRequest3 = getImageUrlsRequestByPager(3);
            String rawJson1 = downloadContentAsString(imageUrlsRequest1.getEndpointUrl());
            String rawJson2 = downloadContentAsString(imageUrlsRequest2.getEndpointUrl());
            String rawJson3 = downloadContentAsString(imageUrlsRequest3.getEndpointUrl());
            if (rawJson == null) {
                return urls;
            }
            addUrls(request, urls, rawJson);
            addUrls(request, urls, rawJson1);
            addUrls(request, urls, rawJson2);
            addUrls(request, urls, rawJson3);
        } catch (Exception e) {
            FLog.e(TAG, "Exception fetching album", e);
        }
        return urls;
    }

    private static ImageUrlsRequest getImageUrlsRequestByPager(int page) {
        String url = "http://api.imgur.com/3/gallery/hot/viral/"+page;//增加图片数量
        ImageUrlsRequestBuilder builder = new ImageUrlsRequestBuilder(url);
        if (mUseJPEG) {
            builder.addImageFormat(
                    ImageFormat.JPEG,
                    ImageSize.LARGE_THUMBNAIL);
        }
        if (mUsePNG) {
            builder.addImageFormat(
                    ImageFormat.PNG,
                    ImageSize.LARGE_THUMBNAIL);
        }

        if (mAllowAnimations) {
            builder.addImageFormat(
                    ImageFormat.GIF,
                    ImageSize.ORIGINAL_IMAGE);
        }
        return builder.build();
    }

    private static void addUrls(ImageUrlsRequest request, List<String> urls, String rawJson) throws JSONException {
        JSONObject json = new JSONObject(rawJson);
        JSONArray data = json.getJSONArray("data");
        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.getJSONObject(i);
            if (!item.has("type")) {
                continue;
            }
            ImageFormat imageFormat = ImageFormat.getImageFormatForMime(item.getString("type"));
            ImageSize imageSize = request.getImageSize(imageFormat);
            if (imageSize != null) {
                if (urls.size() < 60) {//控制在60张-65张
                    urls.add(getThumbnailLink(item, imageSize));
                    Log.e("Tag-----------","mImageUrls.add(\""+getThumbnailLink(item, ImageSize.ORIGINAL_IMAGE)+"\");");//自动写代码...

//                    urls.add(getThumbnailLink(item, ImageSize.BIG_SQUARE));//生成不同种类的URL
//                    urls.add(getThumbnailLink(item, ImageSize.HUGE_THUMBNAIL));
//                    urls.add(getThumbnailLink(item, ImageSize.LARGE_THUMBNAIL));
//                    urls.add(getThumbnailLink(item, ImageSize.SMALL_SQUARE));
//                    urls.add(getThumbnailLink(item, ImageSize.SMALL_THUMBNAIL));
//                    urls.add(getThumbnailLink(item, ImageSize.MEDIUM_THUMBNAIL));
                }
            }
        }
    }

    @Nullable
    private static String downloadContentAsString(String urlString) throws IOException {
        InputStream is = null;
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", IMGUR_CLIENT_ID);
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            if (response != HttpURLConnection.HTTP_OK) {
                FLog.e(TAG, "Album request returned %s", response);
                return null;
            }
            is = conn.getInputStream();
            return readAsString(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    /**
     * Reads an InputStream and converts it to a String.
     */
    private static String readAsString(InputStream stream) throws IOException {
        StringWriter writer = new StringWriter();
        Reader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        while (true) {
            int c = reader.read();
            if (c < 0) {
                break;
            }
            writer.write(c);
        }
        return writer.toString();
    }

    private static String getThumbnailLink(
            final JSONObject json,
            final ImageSize imageSize) throws JSONException {
        Preconditions.checkNotNull(imageSize);
        final String originalUrl = json.getString("link");
        if (imageSize == ImageSize.ORIGINAL_IMAGE) {
            return originalUrl;
        }

        final int dotPos = originalUrl.lastIndexOf('.');
        final StringBuilder linkBuilder = new StringBuilder(originalUrl.length() + 1);
        return linkBuilder
                .append(originalUrl)
                .insert(dotPos, imageSize.suffix)
                .toString();
    }
}
