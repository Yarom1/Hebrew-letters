package io.github.yarom1.hebrewletters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.nio.ByteBuffer;

/**
 * Shows one big letter as a dotted guide and lets a child paint over it with a finger.
 *
 * <p>Every time the finger is lifted, the view measures how much of the letter is covered and how
 * much paint fell clearly outside it (on small "mask" bitmaps, a quarter of the screen size). When
 * enough of the letter is covered without scribbling all over the card, {@link
 * Listener#onTraceCompleted()} is called once.
 */
public class TraceView extends View {

    public interface Listener {
        /** The child touched the canvas for the first time since it was cleared. */
        void onTraceStarted();

        /** The letter has been traced well enough. */
        void onTraceCompleted();
    }

    /** Fraction of the letter that has to be painted over. */
    static final float REQUIRED_COVERAGE = 0.75f;
    /**
     * Most paint allowed clearly outside the letter, measured against the letter's own area. This
     * stops "colour the whole card" from counting, but forgives a wobbly hand.
     */
    static final float MAX_EXCESS = 0.8f;

    private static final float REF_TEXT_SIZE = 1000f;
    /** Stroke thickness of Rubik Bold relative to its font size. */
    private static final float STEM = 0.135f;
    /** The crayon is a bit wider than the letter's strokes, so tracing their middle covers them. */
    private static final float BRUSH_PER_STEM = 1.2f;
    /** Small letters such as י may be drawn larger than the rest, but at most by this factor. */
    private static final float SMALL_LETTER_BOOST = 1.35f;
    /** How far outside the letter paint still counts as "on the letter", in brush widths. */
    private static final float TOLERANCE = 0.6f;
    /** The masks used for scoring are this many times smaller than the view. */
    private static final int MASK_SCALE = 4;

    private static RectF referenceBounds;
    private static Typeface referenceTypeface;

    private final Paint guideFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guideOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint inkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint maskInkPaint = new Paint();
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Path glyphPath = new Path();
    private final Path stroke = new Path();
    private final Matrix toMask = new Matrix();
    private final DashPathEffect dashes;

    private String letter = "א";
    private final Typeface typeface;
    private int guideColor = 0xFF7048E8;
    private float brushWidth;

    private Bitmap ink;
    private Canvas inkCanvas;
    private Bitmap glyphMask;
    private Bitmap zoneMask;
    private Bitmap inkMask;
    private Canvas inkMaskCanvas;
    private int glyphPixels;

    private int activePointer = -1;
    private float lastX;
    private float lastY;
    private boolean moved;
    private boolean hasInk;
    private boolean completed;
    private float coverage;
    private float excess;
    private Listener listener;

    public TraceView(Context context) {
        this(context, null);
    }

    public TraceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        typeface = Fonts.bold(context);
        float density = getResources().getDisplayMetrics().density;
        dashes = new DashPathEffect(new float[] {11 * density, 8 * density}, 0);

        guideFill.setStyle(Paint.Style.FILL);
        guideOutline.setStyle(Paint.Style.STROKE);
        guideOutline.setStrokeWidth(3 * density);
        guideOutline.setStrokeJoin(Paint.Join.ROUND);

        inkPaint.setStyle(Paint.Style.STROKE);
        inkPaint.setStrokeCap(Paint.Cap.ROUND);
        inkPaint.setStrokeJoin(Paint.Join.ROUND);
        setInkColor(0xFF1C7ED6);

        maskInkPaint.setStyle(Paint.Style.STROKE);
        maskInkPaint.setStrokeCap(Paint.Cap.ROUND);
        maskInkPaint.setStrokeJoin(Paint.Join.ROUND);
        maskInkPaint.setColor(Color.BLACK);

        applyGuideStyle();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /** Shows a new letter and wipes the canvas. */
    public void setLetter(String glyph) {
        letter = glyph;
        layoutLetter();
    }

    public void setInkColor(int color) {
        inkPaint.setColor(color);
        dotPaint.setColor(color);
    }

    public void setGuideColor(int color) {
        guideColor = color;
        applyGuideStyle();
        invalidate();
    }

    /** Wipes everything the child drew. */
    public void clear() {
        if (ink != null) {
            ink.eraseColor(Color.TRANSPARENT);
        }
        if (inkMask != null) {
            inkMask.eraseColor(Color.TRANSPARENT);
        }
        stroke.reset();
        activePointer = -1;
        hasInk = false;
        completed = false;
        coverage = 0;
        excess = 0;
        applyGuideStyle();
        invalidate();
    }

    public boolean isCompleted() {
        return completed;
    }

    float getCoverage() {
        return coverage;
    }

    float getExcess() {
        return excess;
    }

    float getBrushWidth() {
        return brushWidth;
    }

    /** The letter's outline in view coordinates. */
    Path getGlyphPath() {
        return glyphPath;
    }

    private void applyGuideStyle() {
        int color = completed ? getContext().getColor(R.color.success) : guideColor;
        guideFill.setColor((color & 0x00FFFFFF) | (completed ? 0x40000000 : 0x26000000));
        guideOutline.setColor(color);
        guideOutline.setPathEffect(completed ? null : dashes);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        layoutLetter();
    }

    /** Fits the letter into the view and prepares fresh drawing and scoring bitmaps. */
    private void layoutLetter() {
        int w = getWidth();
        int h = getHeight();
        glyphPath.reset();
        glyphPixels = 0;
        if (w <= 0 || h <= 0) {
            return;
        }
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(typeface);
        paint.setTextSize(REF_TEXT_SIZE);
        Path raw = new Path();
        paint.getTextPath(letter, 0, letter.length(), 0, 0, raw);
        RectF bounds = new RectF();
        raw.computeBounds(bounds, true);
        if (bounds.width() <= 0 || bounds.height() <= 0) {
            return;
        }

        // All letters share one scale (so their strokes are equally thick), except that short
        // letters may be enlarged a little so they are not tiny.
        RectF ref = referenceBounds(paint);
        float availableW = w * 0.80f;
        float availableH = h * 0.80f;
        float common = Math.min(availableW / ref.width(), availableH / ref.height());
        float own = Math.min(availableW / bounds.width(), availableH / bounds.height());
        float scale = Math.min(own, common * SMALL_LETTER_BOOST);

        Matrix matrix = new Matrix();
        matrix.setTranslate(-bounds.centerX(), -bounds.centerY());
        matrix.postScale(scale, scale);
        matrix.postTranslate(w / 2f, h / 2f);
        raw.transform(matrix);
        // Fonts build letters from overlapping strokes; merge them so the dotted guide shows
        // only the letter's outer edge.
        if (!glyphPath.op(raw, new Path(), Path.Op.UNION) || glyphPath.isEmpty()) {
            glyphPath.set(raw);
        }

        brushWidth = STEM * REF_TEXT_SIZE * scale * BRUSH_PER_STEM;
        inkPaint.setStrokeWidth(brushWidth);
        buildBitmaps(w, h);
        invalidate();
    }

    /** The box that holds every letter of the alphabet, at the reference text size. */
    private static synchronized RectF referenceBounds(Paint paint) {
        if (referenceBounds == null || referenceTypeface != paint.getTypeface()) {
            RectF union = new RectF();
            RectF bounds = new RectF();
            Path path = new Path();
            for (int i = 0; i < Alphabet.count(); i++) {
                String glyph = Alphabet.get(i).glyph;
                path.reset();
                paint.getTextPath(glyph, 0, glyph.length(), 0, 0, path);
                path.computeBounds(bounds, true);
                if (i == 0) {
                    union.set(bounds);
                } else {
                    union.union(bounds);
                }
            }
            referenceBounds = union;
            referenceTypeface = paint.getTypeface();
        }
        return referenceBounds;
    }

    private void buildBitmaps(int w, int h) {
        recycle(ink);
        recycle(glyphMask);
        recycle(zoneMask);
        recycle(inkMask);
        ink = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        inkCanvas = new Canvas(ink);

        int mw = Math.max(1, w / MASK_SCALE);
        int mh = Math.max(1, h / MASK_SCALE);
        toMask.setScale(mw / (float) w, mh / (float) h);
        float maskScale = mw / (float) w;
        Path small = new Path();
        glyphPath.transform(toMask, small);

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeJoin(Paint.Join.ROUND);

        // The letter itself.
        glyphMask = Bitmap.createBitmap(mw, mh, Bitmap.Config.ALPHA_8);
        paint.setStyle(Paint.Style.FILL);
        new Canvas(glyphMask).drawPath(small, paint);

        // The letter plus a margin around it: paint inside this zone is not "outside".
        zoneMask = Bitmap.createBitmap(mw, mh, Bitmap.Config.ALPHA_8);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(2 * TOLERANCE * brushWidth * maskScale);
        new Canvas(zoneMask).drawPath(small, paint);

        // Everything the child painted.
        inkMask = Bitmap.createBitmap(mw, mh, Bitmap.Config.ALPHA_8);
        inkMaskCanvas = new Canvas(inkMask);
        maskInkPaint.setStrokeWidth(brushWidth * maskScale);

        glyphPixels = countOpaque(glyphMask);
        stroke.reset();
        activePointer = -1;
        hasInk = false;
        completed = false;
        coverage = 0;
        excess = 0;
        applyGuideStyle();
    }

    private static void recycle(Bitmap bitmap) {
        if (bitmap != null) {
            bitmap.recycle();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!glyphPath.isEmpty()) {
            canvas.drawPath(glyphPath, guideFill);
            canvas.drawPath(glyphPath, guideOutline);
        }
        if (ink != null) {
            canvas.drawBitmap(ink, 0, 0, bitmapPaint);
        }
        if (activePointer != -1) {
            if (moved) {
                canvas.drawPath(stroke, inkPaint);
            } else {
                // Finger is down but has not moved yet: show a dot under it.
                canvas.drawCircle(lastX, lastY, brushWidth / 2f, dotPaint);
            }
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (ink == null || glyphPixels == 0) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                activePointer = event.getPointerId(0);
                beginStroke(event.getX(), event.getY());
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;
            case MotionEvent.ACTION_MOVE: {
                int i = event.findPointerIndex(activePointer);
                if (i < 0) {
                    return true; // only the first finger draws
                }
                for (int h = 0; h < event.getHistorySize(); h++) {
                    extendStroke(event.getHistoricalX(i, h), event.getHistoricalY(i, h));
                }
                extendStroke(event.getX(i), event.getY(i));
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerId(event.getActionIndex()) == activePointer) {
                    endStroke();
                    activePointer = -1;
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (activePointer != -1) {
                    endStroke();
                }
                activePointer = -1;
                return true;
            default:
                return true;
        }
    }

    private void beginStroke(float x, float y) {
        stroke.reset();
        stroke.moveTo(x, y);
        moved = false;
        lastX = x;
        lastY = y;
        if (!hasInk) {
            hasInk = true;
            if (listener != null) {
                listener.onTraceStarted();
            }
        }
        invalidate();
    }

    private void extendStroke(float x, float y) {
        if (x == lastX && y == lastY) {
            return;
        }
        // Smooth curve through the midpoints of the touch samples.
        stroke.quadTo(lastX, lastY, (lastX + x) / 2f, (lastY + y) / 2f);
        moved = true;
        lastX = x;
        lastY = y;
    }

    private void endStroke() {
        // A tap that never moved still leaves a round dot.
        stroke.lineTo(moved ? lastX : lastX + 0.01f, lastY);
        inkCanvas.drawPath(stroke, inkPaint);
        Path small = new Path();
        stroke.transform(toMask, small);
        inkMaskCanvas.drawPath(small, maskInkPaint);
        stroke.reset();
        invalidate();
        score();
    }

    /** Updates coverage and excess, and reports success once when the letter is traced. */
    private void score() {
        if (glyphPixels == 0) {
            return;
        }
        byte[] letterPixels = pixels(glyphMask);
        byte[] zonePixels = pixels(zoneMask);
        byte[] inkPixels = pixels(inkMask);
        int rowBytes = glyphMask.getRowBytes();
        int covered = 0;
        int outside = 0;
        for (int y = 0; y < glyphMask.getHeight(); y++) {
            int row = y * rowBytes;
            for (int x = 0; x < glyphMask.getWidth(); x++) {
                int i = row + x;
                if ((inkPixels[i] & 0xFF) < 128) {
                    continue;
                }
                if ((letterPixels[i] & 0xFF) >= 128) {
                    covered++;
                }
                if ((zonePixels[i] & 0xFF) < 128) {
                    outside++;
                }
            }
        }
        coverage = covered / (float) glyphPixels;
        excess = outside / (float) glyphPixels;
        if (!completed && coverage >= REQUIRED_COVERAGE && excess <= MAX_EXCESS) {
            completed = true;
            applyGuideStyle();
            invalidate();
            if (listener != null) {
                listener.onTraceCompleted();
            }
        }
    }

    private static byte[] pixels(Bitmap mask) {
        ByteBuffer buffer = ByteBuffer.allocate(mask.getRowBytes() * mask.getHeight());
        mask.copyPixelsToBuffer(buffer);
        return buffer.array();
    }

    private static int countOpaque(Bitmap mask) {
        byte[] data = pixels(mask);
        int rowBytes = mask.getRowBytes();
        int count = 0;
        for (int y = 0; y < mask.getHeight(); y++) {
            int row = y * rowBytes;
            for (int x = 0; x < mask.getWidth(); x++) {
                if ((data[row + x] & 0xFF) >= 128) {
                    count++;
                }
            }
        }
        return count;
    }
}
