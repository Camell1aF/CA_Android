package com.example.ca_test.domain;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

public class CustomVideoView extends VideoView {

    private int videoWidth;
    private int videoHeight;

    public CustomVideoView(Context context) {
        super(context);
    }

    public CustomVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        int viewHeight = MeasureSpec.getSize(heightMeasureSpec);

        if (videoWidth > 0 && videoHeight > 0) {
            float videoAspectRatio = (float) videoHeight / (float) videoWidth;
            float viewAspectRatio = (float) viewHeight / (float) viewWidth;

            if (videoAspectRatio > viewAspectRatio) {
                viewHeight = (int) (viewWidth * videoAspectRatio);
            } else {
                viewWidth = (int) (viewHeight / videoAspectRatio);
            }
        }

        setMeasuredDimension(viewWidth, viewHeight);
    }

    public void setVideoSize(int width, int height) {
        this.videoWidth = width;
        this.videoHeight = height;
        requestLayout();
    }
}


