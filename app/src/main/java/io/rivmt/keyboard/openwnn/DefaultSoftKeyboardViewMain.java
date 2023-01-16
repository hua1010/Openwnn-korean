package io.rivmt.keyboard.openwnn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.view.KeyEvent;

import java.lang.reflect.Field;
import java.util.List;

import io.rivmt.keyboard.openwnn.KOKR.SoftKeyDisplay;
import io.rivmt.keyboard.openwnn.KOKR.SoftKeyboardDisplay;

public class DefaultSoftKeyboardViewMain extends KeyboardView {

	private Context context;
	private SoftKeyboardDisplay keyboardDisplay;
	private int focusIndex = 0;

	public DefaultSoftKeyboardViewMain(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}

	@Override
	public void onDraw(Canvas canvas) {

		Keyboard keyboard = this.getKeyboard();
		if(keyboard == null || keyboardDisplay == null) {
			super.onDraw(canvas);
			return;
		}
		if(keyboardDisplay.getBackground() == 0 || keyboardDisplay.getKeyBackground() == 0) {
			super.onDraw(canvas);
		} else {
			Drawable npd = context.getResources().getDrawable(keyboardDisplay.getBackground());
			Rect rect = new Rect();
			getLocalVisibleRect(rect);
			npd.setBounds(rect);
		}

		for(Key key : keyboard.getKeys()) {
			boolean focus = keyboard.getKeys().indexOf(key) == focusIndex;
			int keyCode = key.codes[0];
			SoftKeyDisplay keyDisplay = keyboardDisplay.get(keyCode);
			if(keyDisplay != null && keyDisplay.getKeyBackground() != 0) {
				onDrawBackground(keyDisplay.getKeyBackground(), keyDisplay.getFixWidth(), canvas, key,focus);
				onDrawForeground(keyDisplay.getKeyIcon(), keyDisplay.getColor(), canvas, key);
			} else if(keyboardDisplay.getKeyBackground() != 0) {
				onDrawBackground(keyboardDisplay.getKeyBackground(), false, canvas, key, focus);
				onDrawForeground(0, keyboardDisplay.getColor(), canvas, key);
			} else {	//keyDisplay == 0 && keyboardDisplay.getKeyBackground() == 0
				onDrawBackground(R.drawable.keybg_white_def, false, canvas, key, focus);
				onDrawForeground(0, keyboardDisplay.getColor(), canvas, key);
			}
		}
	}

	private void onDrawBackground(int drawableId, boolean fixWidth, Canvas canvas, Key key, boolean focus) {
		Drawable npd = null;
		if(focus)
			npd = context.getResources().getDrawable(R.drawable.keybg_focus_white_def);
		else
			npd = context.getResources().getDrawable(drawableId);
		int[] drawableState = key.getCurrentDrawableState();
		if(key.codes[0] != 0) npd.setState(drawableState);

		if(fixWidth) {
			int x = key.x + (key.width - key.height) / 2;
			npd.setBounds(x, key.y, x + key.height, key.y + key.height);
		}
		else npd.setBounds(key.x, key.y, key.x + key.width, key.y + key.height);
		npd.draw(canvas);
	}

	private void onDrawForeground(int drawableId, int textColor, Canvas canvas, Key key) {
		Rect bounds = new Rect();
		Paint paint = new Paint();
		paint.setTextAlign(Paint.Align.CENTER);
		paint.setAntiAlias(true);
		if(textColor != 0) paint.setColor(textColor);
		else paint.setColor(Color.BLACK);

		if(drawableId != 0) {
			Drawable icon = context.getResources().getDrawable(drawableId);
			int x = key.x + (key.width - key.icon.getIntrinsicWidth())/2;
			int y = key.y + (key.height - key.icon.getIntrinsicHeight())/2;
			icon.setBounds(
					x, y, x + key.icon.getIntrinsicWidth(), y + key.icon.getIntrinsicHeight());
			icon.draw(canvas);
		} else if(key.icon != null) {
			int x = key.x + (key.width - key.icon.getIntrinsicWidth())/2;
			int y = key.y + (key.height - key.icon.getIntrinsicHeight())/2;
			key.icon.setBounds(
					x, y, x + key.icon.getIntrinsicWidth(), y + key.icon.getIntrinsicHeight());
			key.icon.draw(canvas);
		} else if(key.label != null) {
			String label = key.label.toString();

			Field field;
			int minimumTextSize = 0;
			try {
				field = KeyboardView.class.getDeclaredField("mLabelTextSize");
				field.setAccessible(true);
				minimumTextSize = (int) field.get(this);
			} catch(NoSuchFieldException e) {
				e.printStackTrace();
			} catch(IllegalAccessException e) {
				e.printStackTrace();
			}
			StringBuilder boundsString = new StringBuilder("A");
			for(int i = 0 ; i < label.length() ; i++) boundsString.append('A');
			paint.getTextBounds(boundsString.toString(), 0, boundsString.length(), bounds);

			int labelTextSize = 4;
			float desiredTextSize;
			if(key.width < key.height) desiredTextSize = labelTextSize * key.width / bounds.height();
			else desiredTextSize = labelTextSize * key.height / bounds.height();
			if(desiredTextSize < minimumTextSize) desiredTextSize = minimumTextSize;
			paint.setTextSize(desiredTextSize);
			paint.setTypeface(Typeface.DEFAULT);

			canvas.drawText(label, key.x + key.width/2, key.y + key.height/2 + desiredTextSize/3, paint);
		}

	}

	public void moveToNextKey(KeyEvent event) {
		Keyboard keyboard = this.getKeyboard();
		List<Key> keys = keyboard.getKeys();
		if(0 > focusIndex || keys.size() < focusIndex) {
			focusIndex = 0;
			invalidate();
			return;
		}
		Key key = keys.get(focusIndex);
		switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_DPAD_UP:{
				if(event.getAction() == KeyEvent.ACTION_UP) break;
				int indices = 0 , target, min = 0;
				if(key.y - key.height >= 0)
					target = (key.y - key.height) * 100 + key.x;
				else
					target = (getHeight() - key.height) * 100 + key.x;
				for (int index = 0;index < keys.size(); index++) {
					Key tmp = keys.get(index);
					if(index == 0) min = Math.abs(tmp.y * 100 + tmp.x - target);
					else if (min > Math.abs(tmp.y * 100 + tmp.x - target)) {
						min = Math.abs(tmp.y * 100 + tmp.x - target);
						indices = index;
					}
				}
				focusIndex = indices;
				invalidate();
				break;
			}
			case KeyEvent.KEYCODE_DPAD_DOWN:{
				if(event.getAction() == KeyEvent.ACTION_UP) break;
				int indices = 0 , target, min = 0;
				if(key.y + key.height < keyboard.getHeight())
					target = (key.y + key.height) * 100 + key.x;
				else
					target = key.x;
				for (int index = 0;index < keys.size(); index++) {
					Key tmp = keys.get(index);
					if(index == 0) min = Math.abs(tmp.y * 100 + tmp.x - target);
					else if (min > Math.abs(tmp.y * 100 + tmp.x - target)) {
						min = Math.abs(tmp.y * 100 + tmp.x - target);
						indices = index;
					}
				}
				focusIndex = indices;
				invalidate();
				break;
			}
			case KeyEvent.KEYCODE_DPAD_LEFT:{
				if(event.getAction() == KeyEvent.ACTION_UP) break;
				focusIndex -= 1;
				if(focusIndex < 0) focusIndex = keys.size() -1;
				invalidate();
				break;
			}
			case KeyEvent.KEYCODE_DPAD_RIGHT:{
				if(event.getAction() == KeyEvent.ACTION_UP) break;
				focusIndex += 1;
				if(focusIndex >= keys.size()) focusIndex = 0;
				invalidate();
				break;
			}
			case KeyEvent.KEYCODE_DPAD_CENTER:
				if(event.getAction() == KeyEvent.ACTION_DOWN) {
					key.onPressed();
					invalidateAllKeys();
				}else
					key.onReleased(false);
				break;
			default:
				break;
		}
	}

	public int getCurrentKey(){
		Keyboard keyboard = this.getKeyboard();
		List<Key> keys = keyboard.getKeys();
		Key key = keys.get(focusIndex);
		return key.codes[0];
	}


	public SoftKeyboardDisplay getKeyboardDisplay() {
		return keyboardDisplay;
	}

	public void setKeyboardDisplay(SoftKeyboardDisplay keyboardDisplay) {
		this.keyboardDisplay = keyboardDisplay;
	}
}
