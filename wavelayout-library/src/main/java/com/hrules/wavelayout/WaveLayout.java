package com.hrules.wavelayout;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.RelativeLayout;
import java.util.ArrayList;

public class WaveLayout extends RelativeLayout {
  public static final int DIRECTION_INWARDS = 0;
  public static final int DIRECTION_OUTWARDS = 1;
  public static final int STYLE_FILL = 0;
  public static final int STYLE_STROKE = 1;

  @IntDef({ DIRECTION_INWARDS, DIRECTION_OUTWARDS }) public @interface Direction {
  }

  @IntDef({ STYLE_FILL, STYLE_STROKE }) public @interface Style {
  }

  private static final int DEFAULT_DIRECTION = DIRECTION_OUTWARDS;
  private static final int DEFAULT_STYLE = STYLE_FILL;

  private Context context;

  private int color;
  private float strokeWidth;
  private float radius;
  private int duration;
  private int amount;
  private int delay;
  private float scale;
  private int style;
  private int direction;
  private float correctionY;
  private float correctionX;

  private Paint paint;

  private Animator animator;
  private Interpolator interpolator;

  private final ArrayList<WaveView> waveViews = new ArrayList<>();
  private ArrayList<WaveLayoutListener> listeners;

  private boolean stopping;

  public WaveLayout(Context context) {
    this(context, null);
  }

  public WaveLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public WaveLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  @SuppressWarnings("ResourceType")
  private void init(final Context context, final AttributeSet attrs) {
    if (isInEditMode()) {
      return;
    }

    this.context = context;

    final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.WaveLayout);
    Resources resources = context.getResources();

    color = typedArray.getColor(R.styleable.WaveLayout_wl_color,
        getResources().getColor(R.color.default_waveColor));
    strokeWidth = typedArray.getDimension(R.styleable.WaveLayout_wl_strokeWidth,
        getResources().getDimension(R.dimen.default_strokeWidth));
    radius = typedArray.getDimension(R.styleable.WaveLayout_wl_radius,
        getResources().getDimension(R.dimen.default_radius));
    duration = typedArray.getInt(R.styleable.WaveLayout_wl_duration,
        resources.getInteger(R.integer.default_durationTime));
    amount = typedArray.getInt(R.styleable.WaveLayout_wl_amount,
        resources.getInteger(R.integer.default_wavesCount));
    scale = typedArray.getFloat(R.styleable.WaveLayout_wl_scale,
        getFloatFromResources(R.dimen.default_scale));
    style = typedArray.getInt(R.styleable.WaveLayout_wl_style, DEFAULT_STYLE);
    direction = typedArray.getInt(R.styleable.WaveLayout_wl_direction, DEFAULT_DIRECTION);
    correctionX = typedArray.getDimension(R.styleable.WaveLayout_wl_correctionX, 0f);
    correctionY = typedArray.getDimension(R.styleable.WaveLayout_wl_correctionY, 0f);
    boolean autoStart = typedArray.getBoolean(R.styleable.WaveLayout_wl_autoStart, false);
    typedArray.recycle();

    interpolator = new AccelerateDecelerateInterpolator();
    delay = duration / amount;

    paint = new Paint();
    paint.setAntiAlias(true);
    setStyle(style == STYLE_FILL ? STYLE_FILL : STYLE_STROKE);
    setColor(color);

    addWaveViews(context);
    setCorrectionXY(correctionX, correctionY);

    stopping = false;
    if (autoStart) {
      start();
    }
  }

  public void addWaveLayoutListener(WaveLayoutListener listener) {
    if (listeners == null) {
      listeners = new ArrayList<>();
    }
    listeners.add(listener);
  }

  public void removeWaveLayoutListener(WaveLayoutListener listener) {
    if (listeners != null) {
      int position = listeners.indexOf(listener);
      if (position >= 0) {
        listeners.remove(position);
      }
    }
  }

  public void clearWaveLayoutListener() {
    if (listeners != null) {
      for (WaveLayoutListener listener : listeners) {
        removeWaveLayoutListener(listener);
      }
      listeners.clear();
      listeners = null;
    }
  }

  private float getFloatFromResources(int resId) {
    TypedValue typedValue = new TypedValue();
    getResources().getValue(resId, typedValue, true);
    return typedValue.getFloat();
  }

  public void resetCorrectionXY() {
    setCorrectionXY(0, 0);
  }

  public void resetCorrectionX() {
    setCorrectionX(0);
  }

  public void resetCorrectionY() {
    setCorrectionY(0);
  }

  public void setCorrectionXY(float correctionX, float correctionY) {
    for (WaveView waveView : waveViews) {
      ObjectAnimator.ofFloat(waveView, "translationY", correctionY).setDuration(0).start();
      ObjectAnimator.ofFloat(waveView, "translationX", correctionX).setDuration(0).start();
    }
  }

  public float getCorrectionY() {
    return correctionY;
  }

  public void setCorrectionY(float correctionY) {
    this.correctionY = correctionY;

    for (WaveView waveView : waveViews) {
      ObjectAnimator.ofFloat(waveView, "translationY", correctionY).setDuration(0).start();
    }
  }

  public float getCorrectionX() {
    return correctionX;
  }

  public void setCorrectionX(float correctionX) {
    this.correctionX = correctionX;

    for (WaveView waveView : waveViews) {
      ObjectAnimator.ofFloat(waveView, "translationX", correctionX).setDuration(0).start();
    }
  }

  public void setColor(@ColorInt int color) {
    this.color = color;

    paint.setColor(color);
    for (WaveView waveView : waveViews) {
      waveView.invalidate();
    }
  }

  public void setStyle(@Style int style) {
    this.style = style;

    if (style == STYLE_FILL) {
      strokeWidth = 0;
      paint.setStyle(Paint.Style.FILL);
    } else {
      paint.setStyle(Paint.Style.STROKE);
    }
    paint.setStrokeWidth(strokeWidth);

    setWaveViewLayoutParams();
  }

  private void setWaveViewLayoutParams() {
    for (WaveView waveView : waveViews) {
      LayoutParams params =
          new LayoutParams((int) (2 * (radius + strokeWidth)), (int) (2 * (radius + strokeWidth)));
      params.addRule(CENTER_IN_PARENT, TRUE);
      waveView.setLayoutParams(params);
    }
  }

  public float getStrokeWidth() {
    return strokeWidth;
  }

  public void setStrokeWidth(float strokeWidth) {
    this.strokeWidth = strokeWidth;
    setStyle(STYLE_STROKE);
  }

  public float getRadius() {
    return radius;
  }

  public void setRadius(float radius) {
    this.radius = radius;
    setWaveViewLayoutParams();
  }

  public int getDuration() {
    return duration;
  }

  public void setDuration(int duration) {
    this.duration = duration;
    restart();
  }

  public int getAmount() {
    return amount;
  }

  public void setAmount(int amount) {
    this.amount = amount;
    removeWaveViews();
    addWaveViews(context);
    restart();
  }

  public float getScale() {
    return scale;
  }

  public void setScale(float scale) {
    this.scale = scale;
    removeWaveViews();
    addWaveViews(context);
    restart();
  }

  public @Direction int getDirection() {
    return direction;
  }

  public void setDirection(@Direction int direction) {
    this.direction = direction;
    restart();
  }

  public Interpolator getInterpolator() {
    return interpolator;
  }

  public void setInterpolator(Interpolator interpolator) {
    this.interpolator = interpolator;
    restart();
  }

  private void addWaveViews(Context context) {
    LayoutParams params =
        new LayoutParams((int) (2 * (radius + strokeWidth)), (int) (2 * (radius + strokeWidth)));
    params.addRule(CENTER_IN_PARENT, TRUE);

    for (int i = 0; i < amount; i++) {
      WaveView waveView = new WaveView(context);
      addView(waveView, i, params);
      waveViews.add(waveView);
    }
  }

  private void removeWaveViews() {
    for (int i = getChildCount(); i >= 0; i--) {
      if (getChildAt(i) instanceof WaveView) {
        removeViewAt(i);
      }
    }
    waveViews.clear();
  }

  private Animator generateAnimation() {
    ArrayList<Animator> animatorList = new ArrayList<>();

    AnimatorSet animatorSet = new AnimatorSet();
    animatorSet.setInterpolator(interpolator);

    for (int i = 0; i < waveViews.size(); i++) {
      final ObjectAnimator scaleXAnimator;
      if (direction == DIRECTION_OUTWARDS) {
        scaleXAnimator = ObjectAnimator.ofFloat(waveViews.get(i), "ScaleX", 1.0f, scale);
      } else {
        scaleXAnimator = ObjectAnimator.ofFloat(waveViews.get(i), "ScaleX", scale, 1.0f);
      }
      scaleXAnimator.setRepeatCount(ObjectAnimator.INFINITE);
      scaleXAnimator.setRepeatMode(ObjectAnimator.RESTART);
      scaleXAnimator.setStartDelay(i * delay);
      scaleXAnimator.setDuration(duration);
      animatorList.add(scaleXAnimator);

      final ObjectAnimator scaleYAnimator;
      if (direction == DIRECTION_OUTWARDS) {
        scaleYAnimator = ObjectAnimator.ofFloat(waveViews.get(i), "ScaleY", 1.0f, scale);
      } else {
        scaleYAnimator = ObjectAnimator.ofFloat(waveViews.get(i), "ScaleY", scale, 1.0f);
      }
      scaleYAnimator.setRepeatCount(ObjectAnimator.INFINITE);
      scaleYAnimator.setRepeatMode(ObjectAnimator.RESTART);
      scaleYAnimator.setStartDelay(i * delay);
      scaleYAnimator.setDuration(duration);
      animatorList.add(scaleYAnimator);

      final ObjectAnimator alphaAnimator;
      if (direction == DIRECTION_OUTWARDS) {
        alphaAnimator = ObjectAnimator.ofFloat(waveViews.get(i), "Alpha", 1.0f, 0f);
      } else {
        alphaAnimator = ObjectAnimator.ofFloat(waveViews.get(i), "Alpha", 0f, 1.0f);
      }
      alphaAnimator.setRepeatCount(ObjectAnimator.INFINITE);
      alphaAnimator.setRepeatMode(ObjectAnimator.RESTART);
      alphaAnimator.setStartDelay(i * delay);
      alphaAnimator.setDuration(duration);
      animatorList.add(alphaAnimator);
    }

    animatorSet.playTogether(animatorList);
    return animatorSet;
  }

  public void start() {
    if (!isRunning()) {
      setWaveVisibility(View.VISIBLE);
    }

    animator = generateAnimation();
    animator.removeAllListeners();
    animator.addListener(new Animator.AnimatorListener() {
      @Override public void onAnimationStart(Animator animation) {
      }

      @Override public void onAnimationEnd(Animator animation) {
        notifyStop();
        setWaveVisibility(View.INVISIBLE);
      }

      @Override public void onAnimationCancel(Animator animation) {
      }

      @Override public void onAnimationRepeat(Animator animation) {
      }
    });

    notifyStart();
    animator.start();
  }

  public void stop() {
    internalStop(false);
  }

  public void stop(boolean kindly) {
    internalStop(kindly);
  }

  private void internalStop(boolean kindly) {
    if (isRunning()) {
      if (kindly) {
        notifyStopping();
        ArrayList<Animator> animatorList = ((AnimatorSet) animator).getChildAnimations();
        for (Animator animatorInList : animatorList) {
          ((ObjectAnimator) animatorInList).setRepeatCount(0);
        }
      } else {
        animator.end();
      }
    }
  }

  public void toggle() {
    if (isRunning()) {
      stop();
    } else {
      start();
    }
  }

  public void restart() {
    if (isRunning()) {
      notifyRestart();

      stop();
      start();
    }
  }

  private void notifyRestart() {
    if (listeners != null) {
      for (WaveLayoutListener listener : listeners) {
        listener.onRestart();
      }
    }
  }

  private void notifyStop() {
    if (listeners != null) {
      stopping = false;
      for (WaveLayoutListener listener : listeners) {
        listener.onStop();
      }
    }
  }

  private void notifyStopping() {
    if (listeners != null && !stopping) {
      stopping = true;
      for (WaveLayoutListener listener : listeners) {
        listener.onStopping();
      }
    }
  }

  private void notifyStart() {
    if (listeners != null) {
      for (WaveLayoutListener listener : listeners) {
        listener.onStart();
      }
    }
  }

  public boolean isRunning() {
    return animator != null && animator.isRunning();
  }

  private void setWaveVisibility(int visibility) {
    for (WaveView waveView : waveViews) {
      waveView.setVisibility(visibility);
    }
  }

  private class WaveView extends View {
    public WaveView(Context context) {
      super(context);
      this.setVisibility(INVISIBLE);
    }

    @Override protected void onDraw(Canvas canvas) {
      int radius = (Math.min(getWidth(), getHeight())) / 2;
      canvas.drawCircle(radius, radius, radius - strokeWidth, paint);
    }
  }
}