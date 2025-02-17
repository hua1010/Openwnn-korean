/*
 * Copyright (C) 2008,2009  OMRON SOFTWARE Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.rivmt.keyboard.openwnn.JAJP;

import io.rivmt.keyboard.openwnn.*;
import io.rivmt.keyboard.openwnn.KOKR.DefaultSoftKeyboardKOKR;
import io.rivmt.keyboard.openwnn.KOKR.DefaultSoftKeyboardViewKOKR;
import io.rivmt.keyboard.openwnn.KOKR.HangulEngine;
import io.rivmt.keyboard.openwnn.event.InputCharEvent;
import io.rivmt.keyboard.openwnn.event.InputJAJPEvent;
import io.rivmt.keyboard.openwnn.event.InputSoftKeyEvent;
import io.rivmt.keyboard.openwnn.event.InputTimeoutEvent;
import io.rivmt.keyboard.openwnn.event.SoftKeyFlickEvent;
import io.rivmt.keyboard.openwnn.event.SoftKeyGestureEvent;
import io.rivmt.keyboard.openwnn.event.SoftKeyLongPressEvent;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.content.Context;
import android.content.SharedPreferences;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * The default Software Keyboard class for Japanese IME.
 *
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class DefaultSoftKeyboardJAJP extends DefaultSoftKeyboard {

    private static final String TAG = "DefaultSoftKeyboardJAJP";
    /** Enable English word prediction on half-width alphabet mode */
    private static final boolean USE_ENGLISH_PREDICT = true;

    /** Key code for switching to full-width HIRAGANA mode */
    private static final int KEYCODE_SWITCH_FULL_HIRAGANA = -301;

    /** Key code for switching to full-width KATAKANA mode */
    private static final int KEYCODE_SWITCH_FULL_KATAKANA = -302;

    /** Key code for switching to full-width alphabet mode */
    private static final int KEYCODE_SWITCH_FULL_ALPHABET = -303;

    /** Key code for switching to full-width number mode */
    private static final int KEYCODE_SWITCH_FULL_NUMBER = -304;

    /** Key code for switching to half-width KATAKANA mode */
    private static final int KEYCODE_SWITCH_HALF_KATAKANA = -306;

    /** Key code for switching to half-width alphabet mode */
    private static final int KEYCODE_SWITCH_HALF_ALPHABET = -307;

    /** Key code for switching to half-width number mode */
    private static final int KEYCODE_SWITCH_HALF_NUMBER = -308;

    /** Key code for case toggle key */
    private static final int KEYCODE_SELECT_CASE = -309;

    /** Key code for EISU-KANA conversion */
    private static final int KEYCODE_EISU_KANA = -305;

    /** Key code for NOP (no-operation) */
    private static final int KEYCODE_NOP = -310;

    private static final int KEYCODE_RIGHT = -217;
    private static final int KEYCODE_LEFT = -218;
    private static final int KEYCODE_DOWN = -219;
    private static final int KEYCODE_UP = -220;

    private static final int KEYCODE_NON_SHIN_DEL = -510;
    private static final int KEYCODE_TOGGLE_ONE_HAND_SIDE = -520;
    private static final int mFlickSensitivity = 100;
    private static final int mSpaceSlideSensitivity = 100;

    private static final int mTimeoutDelay = 0;
    private static final int SPACE_SLIDE_UNIT = 30;
    private static final int BACKSPACE_SLIDE_UNIT = 250;

    /** Input mode toggle cycle table */
    private static final int[] JP_MODE_CYCLE_TABLE = {
        KEYMODE_JA_FULL_HIRAGANA, KEYMODE_JA_HALF_ALPHABET, KEYMODE_JA_HALF_NUMBER
    };

    /** Definition for {@code mInputType} (toggle) */
    private static final int INPUT_TYPE_TOGGLE = 1;

    /** Definition for {@code mInputType} (commit instantly) */
    private static final int INPUT_TYPE_INSTANT = 2;

    /** Toggle cycle table for full-width HIRAGANA */
    private static final String[][] JP_FULL_HIRAGANA_CYCLE_TABLE = {
        {"\u3042", "\u3044", "\u3046", "\u3048", "\u304a", "\u3041", "\u3043", "\u3045", "\u3047", "\u3049"},
        {"\u304b", "\u304d", "\u304f", "\u3051", "\u3053"},
        {"\u3055", "\u3057", "\u3059", "\u305b", "\u305d"},
        {"\u305f", "\u3061", "\u3064", "\u3066", "\u3068", "\u3063"},
        {"\u306a", "\u306b", "\u306c", "\u306d", "\u306e"},
        {"\u306f", "\u3072", "\u3075", "\u3078", "\u307b"},
        {"\u307e", "\u307f", "\u3080", "\u3081", "\u3082"},
        {"\u3084", "\u3086", "\u3088", "\u3083", "\u3085", "\u3087"},
        {"\u3089", "\u308a", "\u308b", "\u308c", "\u308d"},
        {"\u308f", "\u3092", "\u3093", "\u308e", "\u30fc"},
        {"\u3001", "\u3002", "\uff1f", "\uff01", "\u30fb", "\u3000"},
    };

    /** Replace table for full-width HIRAGANA */
    private static final HashMap<String, String> JP_FULL_HIRAGANA_REPLACE_TABLE = new HashMap<String, String>() {{
          put("\u3042", "\u3041"); put("\u3044", "\u3043"); put("\u3046", "\u3045"); put("\u3048", "\u3047"); put("\u304a", "\u3049");
          put("\u3041", "\u3042"); put("\u3043", "\u3044"); put("\u3045", "\u30f4"); put("\u3047", "\u3048"); put("\u3049", "\u304a");
          put("\u304b", "\u304c"); put("\u304d", "\u304e"); put("\u304f", "\u3050"); put("\u3051", "\u3052"); put("\u3053", "\u3054");
          put("\u304c", "\u304b"); put("\u304e", "\u304d"); put("\u3050", "\u304f"); put("\u3052", "\u3051"); put("\u3054", "\u3053");
          put("\u3055", "\u3056"); put("\u3057", "\u3058"); put("\u3059", "\u305a"); put("\u305b", "\u305c"); put("\u305d", "\u305e");
          put("\u3056", "\u3055"); put("\u3058", "\u3057"); put("\u305a", "\u3059"); put("\u305c", "\u305b"); put("\u305e", "\u305d");
          put("\u305f", "\u3060"); put("\u3061", "\u3062"); put("\u3064", "\u3063"); put("\u3066", "\u3067"); put("\u3068", "\u3069");
          put("\u3060", "\u305f"); put("\u3062", "\u3061"); put("\u3063", "\u3065"); put("\u3067", "\u3066"); put("\u3069", "\u3068");
          put("\u3065", "\u3064"); put("\u30f4", "\u3046");
          put("\u306f", "\u3070"); put("\u3072", "\u3073"); put("\u3075", "\u3076"); put("\u3078", "\u3079"); put("\u307b", "\u307c");
          put("\u3070", "\u3071"); put("\u3073", "\u3074"); put("\u3076", "\u3077"); put("\u3079", "\u307a"); put("\u307c", "\u307d");
          put("\u3071", "\u306f"); put("\u3074", "\u3072"); put("\u3077", "\u3075"); put("\u307a", "\u3078"); put("\u307d", "\u307b");
          put("\u3084", "\u3083"); put("\u3086", "\u3085"); put("\u3088", "\u3087");
          put("\u3083", "\u3084"); put("\u3085", "\u3086"); put("\u3087", "\u3088");
          put("\u308f", "\u308e");
          put("\u308e", "\u308f");
          put("\u309b", "\u309c");
          put("\u309c", "\u309b");
    }};

    /** Toggle cycle table for full-width KATAKANA */
    private static final String[][] JP_FULL_KATAKANA_CYCLE_TABLE = {
        {"\u30a2", "\u30a4", "\u30a6", "\u30a8", "\u30aa", "\u30a1", "\u30a3",
         "\u30a5", "\u30a7", "\u30a9"},
        {"\u30ab", "\u30ad", "\u30af", "\u30b1", "\u30b3"},
        {"\u30b5", "\u30b7", "\u30b9", "\u30bb", "\u30bd"},
        {"\u30bf", "\u30c1", "\u30c4", "\u30c6", "\u30c8", "\u30c3"},
        {"\u30ca", "\u30cb", "\u30cc", "\u30cd", "\u30ce"},
        {"\u30cf", "\u30d2", "\u30d5", "\u30d8", "\u30db"},
        {"\u30de", "\u30df", "\u30e0", "\u30e1", "\u30e2"},
        {"\u30e4", "\u30e6", "\u30e8", "\u30e3", "\u30e5", "\u30e7"},
        {"\u30e9", "\u30ea", "\u30eb", "\u30ec", "\u30ed"},
        {"\u30ef", "\u30f2", "\u30f3", "\u30ee", "\u30fc"},
        {"\u3001", "\u3002", "\uff1f", "\uff01", "\u30fb", "\u3000"}
    };

    /** Replace table for full-width KATAKANA */
    private static final HashMap<String,String> JP_FULL_KATAKANA_REPLACE_TABLE = new HashMap<String,String>() {{
        put("\u30a2", "\u30a1"); put("\u30a4", "\u30a3"); put("\u30a6", "\u30a5"); put("\u30a8", "\u30a7"); put("\u30aa", "\u30a9");
        put("\u30a1", "\u30a2"); put("\u30a3", "\u30a4"); put("\u30a5", "\u30f4"); put("\u30a7", "\u30a8"); put("\u30a9", "\u30aa");
        put("\u30ab", "\u30ac"); put("\u30ad", "\u30ae"); put("\u30af", "\u30b0"); put("\u30b1", "\u30b2"); put("\u30b3", "\u30b4");
        put("\u30ac", "\u30ab"); put("\u30ae", "\u30ad"); put("\u30b0", "\u30af"); put("\u30b2", "\u30b1"); put("\u30b4", "\u30b3");
        put("\u30b5", "\u30b6"); put("\u30b7", "\u30b8"); put("\u30b9", "\u30ba"); put("\u30bb", "\u30bc"); put("\u30bd", "\u30be");
        put("\u30b6", "\u30b5"); put("\u30b8", "\u30b7"); put("\u30ba", "\u30b9"); put("\u30bc", "\u30bb"); put("\u30be", "\u30bd");
        put("\u30bf", "\u30c0"); put("\u30c1", "\u30c2"); put("\u30c4", "\u30c3"); put("\u30c6", "\u30c7"); put("\u30c8", "\u30c9");
        put("\u30c0", "\u30bf"); put("\u30c2", "\u30c1"); put("\u30c3", "\u30c5"); put("\u30c7", "\u30c6"); put("\u30c9", "\u30c8");
        put("\u30c5", "\u30c4"); put("\u30f4", "\u30a6");
        put("\u30cf", "\u30d0"); put("\u30d2", "\u30d3"); put("\u30d5", "\u30d6"); put("\u30d8", "\u30d9"); put("\u30db", "\u30dc");
        put("\u30d0", "\u30d1"); put("\u30d3", "\u30d4"); put("\u30d6", "\u30d7"); put("\u30d9", "\u30da"); put("\u30dc", "\u30dd");
        put("\u30d1", "\u30cf"); put("\u30d4", "\u30d2"); put("\u30d7", "\u30d5"); put("\u30da", "\u30d8"); put("\u30dd", "\u30db");
        put("\u30e4", "\u30e3"); put("\u30e6", "\u30e5"); put("\u30e8", "\u30e7");
        put("\u30e3", "\u30e4"); put("\u30e5", "\u30e6"); put("\u30e7", "\u30e8");
        put("\u30ef", "\u30ee");
        put("\u30ee", "\u30ef");
    }};

    /** Toggle cycle table for half-width KATAKANA */
    private static final String[][] JP_HALF_KATAKANA_CYCLE_TABLE = {
        {"\uff71", "\uff72", "\uff73", "\uff74", "\uff75", "\uff67", "\uff68", "\uff69", "\uff6a", "\uff6b"},
        {"\uff76", "\uff77", "\uff78", "\uff79", "\uff7a"},
        {"\uff7b", "\uff7c", "\uff7d", "\uff7e", "\uff7f"},
        {"\uff80", "\uff81", "\uff82", "\uff83", "\uff84", "\uff6f"},
        {"\uff85", "\uff86", "\uff87", "\uff88", "\uff89"},
        {"\uff8a", "\uff8b", "\uff8c", "\uff8d", "\uff8e"},
        {"\uff8f", "\uff90", "\uff91", "\uff92", "\uff93"},
        {"\uff94", "\uff95", "\uff96", "\uff6c", "\uff6d", "\uff6e"},
        {"\uff97", "\uff98", "\uff99", "\uff9a", "\uff9b"},
        {"\uff9c", "\uff66", "\uff9d", "\uff70"},
        {"\uff64", "\uff61", "?", "!", "\uff65", " "},
    };

    /** Replace table for half-width KATAKANA */
    private static final HashMap<String,String> JP_HALF_KATAKANA_REPLACE_TABLE = new HashMap<String,String>() {{
        put("\uff71", "\uff67");  put("\uff72", "\uff68");  put("\uff73", "\uff69");  put("\uff74", "\uff6a");  put("\uff75", "\uff6b");
        put("\uff67", "\uff71");  put("\uff68", "\uff72");  put("\uff69", "\uff73\uff9e");  put("\uff6a", "\uff74");  put("\uff6b", "\uff75");
        put("\uff76", "\uff76\uff9e"); put("\uff77", "\uff77\uff9e"); put("\uff78", "\uff78\uff9e"); put("\uff79", "\uff79\uff9e"); put("\uff7a", "\uff7a\uff9e");
        put("\uff76\uff9e", "\uff76"); put("\uff77\uff9e", "\uff77"); put("\uff78\uff9e", "\uff78"); put("\uff79\uff9e", "\uff79"); put("\uff7a\uff9e", "\uff7a");
        put("\uff7b", "\uff7b\uff9e"); put("\uff7c", "\uff7c\uff9e"); put("\uff7d", "\uff7d\uff9e"); put("\uff7e", "\uff7e\uff9e"); put("\uff7f", "\uff7f\uff9e");
        put("\uff7b\uff9e", "\uff7b"); put("\uff7c\uff9e", "\uff7c"); put("\uff7d\uff9e", "\uff7d"); put("\uff7e\uff9e", "\uff7e"); put("\uff7f\uff9e", "\uff7f");
        put("\uff80", "\uff80\uff9e"); put("\uff81", "\uff81\uff9e"); put("\uff82", "\uff6f");  put("\uff83", "\uff83\uff9e"); put("\uff84", "\uff84\uff9e");
        put("\uff80\uff9e", "\uff80"); put("\uff81\uff9e", "\uff81"); put("\uff6f", "\uff82\uff9e"); put("\uff83\uff9e", "\uff83"); put("\uff84\uff9e", "\uff84");
        put("\uff82\uff9e", "\uff82");
        put("\uff8a", "\uff8a\uff9e"); put("\uff8b", "\uff8b\uff9e"); put("\uff8c", "\uff8c\uff9e"); put("\uff8d", "\uff8d\uff9e"); put("\uff8e", "\uff8e\uff9e");
        put("\uff8a\uff9e", "\uff8a\uff9f");put("\uff8b\uff9e", "\uff8b\uff9f");put("\uff8c\uff9e", "\uff8c\uff9f");put("\uff8d\uff9e", "\uff8d\uff9f");put("\uff8e\uff9e", "\uff8e\uff9f");
        put("\uff8a\uff9f", "\uff8a"); put("\uff8b\uff9f", "\uff8b"); put("\uff8c\uff9f", "\uff8c"); put("\uff8d\uff9f", "\uff8d"); put("\uff8e\uff9f", "\uff8e");
        put("\uff94", "\uff6c");  put("\uff95", "\uff6d");  put("\uff96", "\uff6e");
        put("\uff6c", "\uff94");  put("\uff6d", "\uff95");  put("\uff6e", "\uff96");
        put("\uff9c", "\uff9c"); put("\uff73\uff9e", "\uff73");
    }};

    /** Toggle cycle table for full-width alphabet */
    private static final String[][] JP_FULL_ALPHABET_CYCLE_TABLE = {
        {"\uff0e", "\uff20", "\uff0d", "\uff3f", "\uff0f", "\uff1a", "\uff5e", "\uff11"},
        {"\uff41", "\uff42", "\uff43", "\uff21", "\uff22", "\uff23", "\uff12"},
        {"\uff44", "\uff45", "\uff46", "\uff24", "\uff25", "\uff26", "\uff13"},
        {"\uff47", "\uff48", "\uff49", "\uff27", "\uff28", "\uff29", "\uff14"},
        {"\uff4a", "\uff4b", "\uff4c", "\uff2a", "\uff2b", "\uff2c", "\uff15"},
        {"\uff4d", "\uff4e", "\uff4f", "\uff2d", "\uff2e", "\uff2f", "\uff16"},
        {"\uff50", "\uff51", "\uff52", "\uff53", "\uff30", "\uff31", "\uff32", "\uff33", "\uff17"},
        {"\uff54", "\uff55", "\uff56", "\uff34", "\uff35", "\uff36", "\uff18"},
        {"\uff57", "\uff58", "\uff59", "\uff5a", "\uff37", "\uff38", "\uff39", "\uff3a", "\uff19"},
        {"\uff0d", "\uff10"},
        {"\uff0c", "\uff0e", "\uff1f", "\uff01", "\u30fb", "\u3000"}
    };

    /** Replace table for full-width alphabet */
    private static final HashMap<String,String> JP_FULL_ALPHABET_REPLACE_TABLE = new HashMap<String,String>() {{
        put("\uff21", "\uff41"); put("\uff22", "\uff42"); put("\uff23", "\uff43"); put("\uff24", "\uff44"); put("\uff25", "\uff45"); 
        put("\uff41", "\uff21"); put("\uff42", "\uff22"); put("\uff43", "\uff23"); put("\uff44", "\uff24"); put("\uff45", "\uff25"); 
        put("\uff26", "\uff46"); put("\uff27", "\uff47"); put("\uff28", "\uff48"); put("\uff29", "\uff49"); put("\uff2a", "\uff4a"); 
        put("\uff46", "\uff26"); put("\uff47", "\uff27"); put("\uff48", "\uff28"); put("\uff49", "\uff29"); put("\uff4a", "\uff2a"); 
        put("\uff2b", "\uff4b"); put("\uff2c", "\uff4c"); put("\uff2d", "\uff4d"); put("\uff2e", "\uff4e"); put("\uff2f", "\uff4f"); 
        put("\uff4b", "\uff2b"); put("\uff4c", "\uff2c"); put("\uff4d", "\uff2d"); put("\uff4e", "\uff2e"); put("\uff4f", "\uff2f"); 
        put("\uff30", "\uff50"); put("\uff31", "\uff51"); put("\uff32", "\uff52"); put("\uff33", "\uff53"); put("\uff34", "\uff54"); 
        put("\uff50", "\uff30"); put("\uff51", "\uff31"); put("\uff52", "\uff32"); put("\uff53", "\uff33"); put("\uff54", "\uff34"); 
        put("\uff35", "\uff55"); put("\uff36", "\uff56"); put("\uff37", "\uff57"); put("\uff38", "\uff58"); put("\uff39", "\uff59"); 
        put("\uff55", "\uff35"); put("\uff56", "\uff36"); put("\uff57", "\uff37"); put("\uff58", "\uff38"); put("\uff59", "\uff39"); 
        put("\uff3a", "\uff5a"); 
        put("\uff5a", "\uff3a"); 
    }};

    /** Toggle cycle table for half-width alphabet */
    private static final String[][] JP_HALF_ALPHABET_CYCLE_TABLE = {
        {".", "@", "-", "_", "/", ":", "~", "1"},
        {"a", "b", "c", "A", "B", "C", "2"},
        {"d", "e", "f", "D", "E", "F", "3"},
        {"g", "h", "i", "G", "H", "I", "4"},
        {"j", "k", "l", "J", "K", "L", "5"},
        {"m", "n", "o", "M", "N", "O", "6"},
        {"p", "q", "r", "s", "P", "Q", "R", "S", "7"},
        {"t", "u", "v", "T", "U", "V", "8"},
        {"w", "x", "y", "z", "W", "X", "Y", "Z", "9"},
        {"-", "0"},
        {",", ".", "?", "!", ";", " "}
    };

    /** Replace table for half-width alphabet */
    private static final HashMap<String,String> JP_HALF_ALPHABET_REPLACE_TABLE = new HashMap<String,String>() {{
        put("A", "a"); put("B", "b"); put("C", "c"); put("D", "d"); put("E", "e"); 
        put("a", "A"); put("b", "B"); put("c", "C"); put("d", "D"); put("e", "E"); 
        put("F", "f"); put("G", "g"); put("H", "h"); put("I", "i"); put("J", "j"); 
        put("f", "F"); put("g", "G"); put("h", "H"); put("i", "I"); put("j", "J"); 
        put("K", "k"); put("L", "l"); put("M", "m"); put("N", "n"); put("O", "o"); 
        put("k", "K"); put("l", "L"); put("m", "M"); put("n", "N"); put("o", "O"); 
        put("P", "p"); put("Q", "q"); put("R", "r"); put("S", "s"); put("T", "t"); 
        put("p", "P"); put("q", "Q"); put("r", "R"); put("s", "S"); put("t", "T"); 
        put("U", "u"); put("V", "v"); put("W", "w"); put("X", "x"); put("Y", "y"); 
        put("u", "U"); put("v", "V"); put("w", "W"); put("x", "X"); put("y", "Y"); 
        put("Z", "z"); 
        put("z", "Z"); 
    }};

    /** Character table for full-width number */
    private static final char[] INSTANT_CHAR_CODE_FULL_NUMBER = 
        "\uff11\uff12\uff13\uff14\uff15\uff16\uff17\uff18\uff19\uff10\uff03\uff0a".toCharArray();

    /** Character table for half-width number */
    private static final char[] INSTANT_CHAR_CODE_HALF_NUMBER = 
        "1234567890#*".toCharArray();

    /** The constant for mFixedKeyMode. It means that input mode is not fixed. */
    private static final int INVALID_KEYMODE = -1;

    /** Type of input mode */
    private int mInputType = INPUT_TYPE_TOGGLE;

    /** Previous input character code */
    private int mPrevInputKeyCode = 0;

    /**
     * Character table to input when mInputType becomes INPUT_TYPE_INSTANT.
     * (Either INSTANT_CHAR_CODE_FULL_NUMBER or INSTANT_CHAR_CODE_HALF_NUMBER)
     */
    private char[] mCurrentInstantTable = null;

    /** Input mode that is not able to be changed. If ENABLE_CHANGE_KEYMODE is set, input mode can change. */
    private int mFixedKeyMode = INVALID_KEYMODE;

    /** Input mode that is given the first priority. If ENABLE_CHANGE_KEYMODE is set, input mode can change. */
    private int mPreferenceKeyMode = INVALID_KEYMODE;

    /** The last input type */
    private int mLastInputType = 0;

    /** Auto caps mode */
    private boolean mEnableAutoCaps = true;

    protected boolean mShowKeyPreview = false;
    protected int mVibrateDuration = 30;

    /** Default constructor */
    public DefaultSoftKeyboardJAJP() {
        mCurrentLanguage     = LANG_JA;
        mCurrentKeyboardType = KEYBOARD_QWERTY;
        mShiftOn             = KEYBOARD_SHIFT_OFF;
        mCurrentKeyMode      = KEYMODE_JA_FULL_HIRAGANA;
    }

    /** @see io.rivmt.keyboard.openwnn.DefaultSoftKeyboard#createKeyboards */
    @Override protected void createKeyboards(OpenWnn parent) {

        /* Keyboard[# of Languages][portrait/landscape][# of keyboard type][shift off/on][max # of key-modes][noinput/input] */
        mKeyboard = new Keyboard[3][2][4][2][8][2];

        if (mHardKeyboardHidden) {
        	/* Create the suitable keyboard object */
            if (mDisplayMode == DefaultSoftKeyboard.PORTRAIT) {
                createKeyboardsPortrait(parent);
            } else {
                createKeyboardsLandscape(parent);
            }
            
            if (mCurrentKeyboardType == KEYBOARD_12KEY) {
                mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.CHANGE_MODE,
                                              OpenWnnJAJP.ENGINE_MODE_OPT_TYPE_12KEY));
            } else {
                mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.CHANGE_MODE,
                                              OpenWnnJAJP.ENGINE_MODE_OPT_TYPE_QWERTY));
            }
        } else {
            mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.CHANGE_MODE,
                                          OpenWnnJAJP.ENGINE_MODE_OPT_TYPE_QWERTY));
        }
    }

    /**
     * Commit the pre-edit string for committing operation that is not explicit (ex. when a candidate is selected)
     */
    private void commitText() {
        if (!mNoInput) {
            mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.COMMIT_COMPOSING_TEXT));
        }
    }

    public void setPreviewEnabled(int x) {
        switch(x) {
            case KEYCODE_QWERTY_SHIFT:
            case KEYCODE_QWERTY_ENTER:
            case KEYCODE_JP12_ENTER:
            case KEYCODE_QWERTY_BACKSPACE:
            case KEYCODE_JP12_BACKSPACE:
            case -10:
            case KEYCODE_JP12_SPACE:
                break;
            default:
                mKeyboardView.setPreviewEnabled(mShowKeyPreview);
        }
    }

    public void updateKeyLabels() {
        mKeyboardView.invalidateAllKeys();
        mKeyboardView.requestLayout();
    }


    class LongClickHandler implements Runnable {
        int keyCode;
        boolean performed = false;
        public LongClickHandler(int keyCode) {
            this.keyCode = keyCode;
        }
        public void run() {
            setPreviewEnabled(keyCode);
            switch(keyCode) {
                case KEYCODE_QWERTY_SHIFT:
                    if(mShiftOn > 0) return;
                    toggleShiftLock();
                    EventBus.getDefault().post(new InputSoftKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT)));
                    mCapsLock = true;
                    performed = true;
                    updateKeyLabels();
                    return;

                case KEYCODE_JP12_BACKSPACE:
                case KEYCODE_QWERTY_BACKSPACE:
                    mBackspaceLongClickHandler.postDelayed(new BackspaceLongClickHandler(), 50);
                    return;
            }
            EventBus.getDefault().post(new SoftKeyLongPressEvent(keyCode));
            try { mVibrator.vibrate(mVibrateDuration*2); } catch (Exception ex) { }
            performed = true;
        }
    }

    Handler mBackspaceLongClickHandler = new Handler();
    class BackspaceLongClickHandler implements Runnable {
        @Override
        public void run() {
            EventBus.getDefault().post(new InputSoftKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_NON_SHIN_DEL)));
            mBackspaceLongClickHandler.postDelayed(new BackspaceLongClickHandler(), 50);
        }
    }

    int mLongPressTimeout = 500;

    private SparseArray<TouchPoint> mTouchPoints = new SparseArray<>();
    class TouchPoint {
        Keyboard.Key key;
        int keyCode;

        float downX, downY;
        float dx, dy;
        float beforeX, beforeY;
        int space = -1;
        int spaceDistance;
        int backspace = -1;
        int backspaceDistance;

        LongClickHandler longClickHandler;
        Handler handler;

        public TouchPoint(Keyboard.Key key, float downX, float downY) {
            this.key = key;
            this.keyCode = key.codes[0];
            this.downX = downX;
            this.downY = downY;

            key.onPressed();
            mKeyboardView.invalidateAllKeys();

            setPreviewEnabled(keyCode);

            handler = new Handler();
            handler.postDelayed(longClickHandler = new LongClickHandler(keyCode), mLongPressTimeout);

            /* key click sound & vibration */
            if (mVibrator != null) {
                try { mVibrator.vibrate(mVibrateDuration); } catch (Exception ex) { }
            }
            if (mSound != null) {
                try { mSound.seekTo(0); mSound.start(); } catch (Exception ex) { }
            }
        }

        public boolean onMove(float x, float y) {
            dx = x - downX;
            dy = y - downY;
            switch(keyCode) {
                case KEYCODE_JP12_SPACE:
                case -10:
                    if(Math.abs(dx) >= mSpaceSlideSensitivity) space = keyCode;
                    break;

                case KEYCODE_JP12_BACKSPACE:
                case KEYCODE_QWERTY_BACKSPACE:
                    if(Math.abs(dx) >= BACKSPACE_SLIDE_UNIT) {
                        backspace = keyCode;
                        mBackspaceLongClickHandler.removeCallbacksAndMessages(null);
                    }
                    break;

                default:
                    space = -1;
                    backspace = -1;
                    break;
            }
            if(dy > mFlickSensitivity || dy < -mFlickSensitivity
                    || dx < -mFlickSensitivity || dx > mFlickSensitivity || space != -1) {
                handler.removeCallbacksAndMessages(null);
            }
            if(space != -1) {
                spaceDistance += x - beforeX;
                if(spaceDistance < -SPACE_SLIDE_UNIT) {
                    spaceDistance = 0;
                    EventBus.getDefault().post(new InputSoftKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT)));
                    EventBus.getDefault().post(new InputSoftKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT)));
                }
                if(spaceDistance > +SPACE_SLIDE_UNIT) {
                    spaceDistance = 0;
                    EventBus.getDefault().post(new InputSoftKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT)));
                    EventBus.getDefault().post(new InputSoftKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT)));
                }
            }
            if(backspace != -1) {
                backspaceDistance += x - beforeX;
                if(backspaceDistance < -BACKSPACE_SLIDE_UNIT) {
                    backspaceDistance = 0;
                    EventBus.getDefault().post(new SoftKeyGestureEvent(KeyEvent.KEYCODE_DEL, SoftKeyGestureEvent.Type.SLIDE_LEFT));
                }
                if(backspaceDistance > +BACKSPACE_SLIDE_UNIT) {
                    backspaceDistance = 0;
                    EventBus.getDefault().post(new SoftKeyGestureEvent(KeyEvent.KEYCODE_DEL, SoftKeyGestureEvent.Type.SLIDE_RIGHT));
                }
            }
            beforeX = x;
            beforeY = y;
            return true;
        }

        public boolean onUp() {
            key.onReleased(true);
            mKeyboardView.setPreviewEnabled(false);
            mBackspaceLongClickHandler.removeCallbacksAndMessages(null);
            mKeyboardView.invalidateAllKeys();
            handler.removeCallbacksAndMessages(null);
            if(space != -1) {
                space = -1;
                return false;
            }
            if(backspace != -1) {
                EventBus.getDefault().post(new SoftKeyGestureEvent(KeyEvent.KEYCODE_DEL, SoftKeyGestureEvent.Type.RELEASE));
                backspace = -1;
                return false;
            }
            // Swipe Detection
            if(dx < -mFlickSensitivity*5) {
                if(Math.abs(dx) > Math.abs(dy)) {
                    swipeLeft();
                }
                return false;
            }
            if(dx > mFlickSensitivity*5) {
                if(Math.abs(dx) > Math.abs(dy)) {
                    swipeRight();
                }
                return false;
            }

            //Flick detection
            if(dy > mFlickSensitivity) {
                if(Math.abs(dy) > Math.abs(dx)) {
                    EventBus.getDefault().post(new SoftKeyFlickEvent(keyCode, SoftKeyFlickEvent.Direction.DOWN));
                }
                return false;
            }
            if(dy < -mFlickSensitivity) {
                if(Math.abs(dy) > Math.abs(dx)) {
                    EventBus.getDefault().post(new SoftKeyFlickEvent(keyCode, SoftKeyFlickEvent.Direction.UP));
                }
                return false;
            }
            if(dx < -mFlickSensitivity) {
                if(Math.abs(dx) > Math.abs(dy)) {
                    EventBus.getDefault().post(new SoftKeyFlickEvent(keyCode, SoftKeyFlickEvent.Direction.LEFT));
                }
                return false;
            }
            if(dx > mFlickSensitivity) {
                if(Math.abs(dx) > Math.abs(dy)) {
                    EventBus.getDefault().post(new SoftKeyFlickEvent(keyCode, SoftKeyFlickEvent.Direction.RIGHT));
                }
                return false;
            }
            if(!longClickHandler.performed) onKey(keyCode);
            return false;
        }

    }
    class onKeyboardViewKeyListenor implements View.OnKeyListener{

        @Override
        public boolean onKey(View view, int i, KeyEvent keyEvent) {
            if(keyEvent.getAction() != KeyEvent.ACTION_UP)
                return false;
            switch (keyEvent.getKeyCode()){
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    Log.d(TAG, "onKey: "+ keyEvent.getKeyCode());
                    return true;
            }
            return false;
        }
    }


    class OnKeyboardViewTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(Build.VERSION.SDK_INT >= 8) {
                int pointerIndex = event.getActionIndex();
                int pointerId = event.getPointerId(pointerIndex);
                int action = event.getActionMasked();
                float x = event.getX(pointerIndex), y = event.getY(pointerIndex);
                switch(action) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                        TouchPoint point = new TouchPoint(findKey(mCurrentKeyboard, (int) x, (int) y), x, y);
                        mTouchPoints.put(pointerId, point);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        return mTouchPoints.get(pointerId).onMove(x, y);

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        mTouchPoints.get(pointerId).onUp();
                        mTouchPoints.remove(pointerId);
                        return true;

                }
            } else {
                float x = event.getX(), y = event.getY();
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        TouchPoint point = new TouchPoint(findKey(mCurrentKeyboard, (int) x, (int) y), x, y);
                        mTouchPoints.put(0, point);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        return mTouchPoints.get(0).onMove(x, y);

                    case MotionEvent.ACTION_UP:
                        mTouchPoints.get(0).onUp();
                        mTouchPoints.remove(0);
                        return true;

                }
            }
            return false;
        }

        private Keyboard.Key findKey(Keyboard keyboard, int x, int y) {
            for(Keyboard.Key key : keyboard.getKeys()) {
                if(key.isInside(x, y)) return key;
            }
            return null;
        }

    }


    /**
     * Change input mode
     * <br>
     * @param keyMode   The type of input mode
     */
    public void changeKeyMode(int keyMode) {
        int targetMode = keyMode;
        commitText();

        if (mCapsLock) {
			mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.INPUT_SOFT_KEY,
                                          new KeyEvent(KeyEvent.ACTION_UP,
                                                       KeyEvent.KEYCODE_SHIFT_LEFT)));
            mCapsLock = false;
        }
        mShiftOn = KEYBOARD_SHIFT_OFF;

        if (mFixedKeyMode != INVALID_KEYMODE) {
            targetMode = mFixedKeyMode;
        }

        if (!mHardKeyboardHidden) {
            if ((targetMode != KEYMODE_JA_FULL_HIRAGANA)
                && (targetMode != KEYMODE_JA_HALF_ALPHABET)) {

                Locale locale = Locale.getDefault();
                int keymode = KEYMODE_JA_HALF_ALPHABET;
                if (locale.getLanguage().equals(Locale.JAPANESE.getLanguage())) {
                    switch (targetMode) {
                    case KEYMODE_JA_FULL_HIRAGANA:
                    case KEYMODE_JA_FULL_KATAKANA:
                    case KEYMODE_JA_HALF_KATAKANA:
                        keymode = KEYMODE_JA_FULL_HIRAGANA;
                        break;
                    default:
                        break;
                    }
                }
                targetMode = keymode;
            }
        }
        Keyboard kbd = getModeChangeKeyboard(targetMode);
        mCurrentKeyMode = targetMode;
        mPrevInputKeyCode = 0;
        
        int mode = InputJAJPEvent.Mode.DIRECT;
        
        switch (targetMode) {
        case KEYMODE_JA_FULL_HIRAGANA:
            mInputType = INPUT_TYPE_TOGGLE;
            mode = InputJAJPEvent.Mode.DEFAULT;
            break;
            
        case KEYMODE_JA_HALF_ALPHABET:
            if (USE_ENGLISH_PREDICT) {
                mInputType = INPUT_TYPE_TOGGLE;
                mode = InputJAJPEvent.Mode.NO_LV1_CONV;
            } else {
                mInputType = INPUT_TYPE_TOGGLE;
                mode = InputJAJPEvent.Mode.DIRECT;
            }
            break;
            
        case KEYMODE_JA_FULL_NUMBER:
            mInputType = INPUT_TYPE_INSTANT;
            mode = InputJAJPEvent.Mode.DIRECT;
            mCurrentInstantTable = INSTANT_CHAR_CODE_FULL_NUMBER;
            break;
            
        case KEYMODE_JA_HALF_NUMBER:
            mInputType = INPUT_TYPE_INSTANT;
            mode = InputJAJPEvent.Mode.DIRECT;
            mCurrentInstantTable = INSTANT_CHAR_CODE_HALF_NUMBER;
            break;
            
        case KEYMODE_JA_FULL_KATAKANA:
            mInputType = INPUT_TYPE_TOGGLE;
            mode = OpenWnnJAJP.ENGINE_MODE_FULL_KATAKANA;
            break;
            
        case KEYMODE_JA_FULL_ALPHABET:
            mInputType = INPUT_TYPE_TOGGLE;
            mode = InputJAJPEvent.Mode.DIRECT;
            break;
            
        case KEYMODE_JA_HALF_KATAKANA:
            mInputType = INPUT_TYPE_TOGGLE;
            mode = OpenWnnJAJP.ENGINE_MODE_HALF_KATAKANA;
            break;
            
        default:
            break;
        }
        
        setStatusIcon();
        changeKeyboard(kbd);
        mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.CHANGE_MODE, mode));
    }

     /** @see io.rivmt.keyboard.openwnn.DefaultSoftKeyboard#initView */
     @Override public View initView(OpenWnn parent, int width, int height) {
         mWnn = parent;
         mDisplayMode =
                 (parent.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                         ? LANDSCAPE : PORTRAIT;
         createKeyboards(parent);
         SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(parent);
         String skin = pref.getString("keyboard_skin", mWnn.getResources().getString(R.string.keyboard_skin_id_default));
         int id = parent.getResources().getIdentifier("keyboard_ja_" + skin, "layout", parent.getPackageName());
         if(id == 0) id = R.layout.keyboard_ja_white;

         mKeyboardView = (KeyboardView) mWnn.getLayoutInflater().inflate(id, null);
         mKeyboardView.setOnKeyboardActionListener(this);
         mCurrentKeyboard = null;

         mMainView = (ViewGroup) parent.getLayoutInflater().inflate(R.layout.keyboard_default_main, null);
         mSubView = (ViewGroup) parent.getLayoutInflater().inflate(R.layout.keyboard_default_sub, null);
         if (!mHardKeyboardHidden) {
             mMainView.addView(mSubView);
         } else if (mKeyboardView != null) {
             mKeyboardView.setFocusable(true);
             mKeyboardView.setFocusableInTouchMode(true);
             mMainView.addView(mKeyboardView);
         }
        changeKeyboard(mKeyboard[mCurrentLanguage][mDisplayMode][mCurrentKeyboardType][mShiftOn][mCurrentKeyMode][0]);

        mKeyboardView.setOnTouchListener(new OnKeyboardViewTouchListener());
        mKeyboardView.setOnKeyListener(new onKeyboardViewKeyListenor());

        return mMainView;
    }

    /** @see io.rivmt.keyboard.openwnn.DefaultSoftKeyboard#changeKeyboardType */
    @Override public void changeKeyboardType(int type) {
        commitText();
        Keyboard kbd = getTypeChangeKeyboard(type);
        if (kbd != null) {
            mCurrentKeyboardType = type;
            changeKeyboard(kbd);
        }
        if (type == KEYBOARD_12KEY) {
            mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.CHANGE_MODE, OpenWnnJAJP.ENGINE_MODE_OPT_TYPE_12KEY));
        } else {
            mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.CHANGE_MODE, OpenWnnJAJP.ENGINE_MODE_OPT_TYPE_QWERTY));
        }
    }

    /** @see io.rivmt.keyboard.openwnn.DefaultSoftKeyboard#onKey */
    @Override public void onKey(int primaryCode, int[] keyCodes) {
        super.onKey(primaryCode,keyCodes);
    }

    Handler mTimeoutHandler;
    class TimeOutHandler implements Runnable {
        @Override
        public void run() {
            EventBus.getDefault().post(new InputTimeoutEvent());
        }
    }

    public void dispatchKeyEvent(KeyEvent event){
        if(mKeyboardView != null) {
            //mKeyboardView.dispatchKeyShortcutEvent(event);
            //mKeyboardView.dispatchKeyEvent(event);
            if(mKeyboardView instanceof  DefaultSoftKeyboardViewJAJP) {
                Log.d(TAG, "[hangup]dispatchKeyEvent........." + event.getAction());
                DefaultSoftKeyboardViewJAJP keyboardView = ((DefaultSoftKeyboardViewJAJP) mKeyboardView);
                keyboardView.moveToNextKey(event);
                if(event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER && event.getAction() == KeyEvent.ACTION_UP){
                    onKey(keyboardView.getCurrentKey());
                }
            }
            //mMainView.dispatchKeyEvent(event);
        }
    }

    public void onKey(int primaryCode) {

        Log.d(TAG, "onKey: primaryCode:" + primaryCode);

        if(mTimeoutHandler != null) {
            mTimeoutHandler.removeCallbacksAndMessages(null);
            mTimeoutHandler = null;
        }

        switch (primaryCode) {
            case KEYCODE_JP12_TOGGLE_MODE:
            case KEYCODE_QWERTY_TOGGLE_MODE:
                nextKeyMode();
                break;

            case DefaultSoftKeyboard.KEYCODE_QWERTY_BACKSPACE:
            case KEYCODE_JP12_BACKSPACE:
                mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.INPUT_SOFT_KEY,
                        new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)));
                break;

            case DefaultSoftKeyboard.KEYCODE_QWERTY_SHIFT:
                toggleShiftLock();
                break;

            case DefaultSoftKeyboard.KEYCODE_QWERTY_ALT:
                processAltKey();
                break;

            case KEYCODE_QWERTY_ENTER:
            case KEYCODE_JP12_ENTER:
                mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.INPUT_SOFT_KEY,
                        new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)));
                break;

            case KEYCODE_JP12_REVERSE:
                if (!mNoInput) {
                    mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.TOGGLE_REVERSE_CHAR, mCurrentCycleTable));
                }
                break;

            case KEYCODE_QWERTY_KBD:
                changeKeyboardType(KEYBOARD_12KEY);
                break;

            case KEYCODE_JP12_KBD:
                changeKeyboardType(KEYBOARD_QWERTY);
                break;

            case KEYCODE_JP12_EMOJI:
            case KEYCODE_QWERTY_EMOJI:
                commitText();
                mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.CHANGE_MODE, OpenWnnJAJP.ENGINE_MODE_SYMBOL));
                break;

            case KEYCODE_JP12_1:
            case KEYCODE_JP12_2:
            case KEYCODE_JP12_3:
            case KEYCODE_JP12_4:
            case KEYCODE_JP12_5:
            case KEYCODE_JP12_6:
            case KEYCODE_JP12_7:
            case KEYCODE_JP12_8:
            case KEYCODE_JP12_9:
            case KEYCODE_JP12_0:
            case KEYCODE_JP12_SHARP:
                /* Processing to input by ten key */
                if (mInputType == INPUT_TYPE_INSTANT) {
                    /* Send a input character directly if instant input type is selected */
                    commitText();
                    mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.INPUT_CHAR,
                            mCurrentInstantTable[getTableIndex(primaryCode)]));
                } else {
                    if ((mPrevInputKeyCode != primaryCode)) {
                        if ((mCurrentKeyMode == KEYMODE_JA_HALF_ALPHABET)
                                && (primaryCode == KEYCODE_JP12_SHARP)) {
                            /* Commit text by symbol character (',' '.') when alphabet input mode is selected */
                            commitText();
                        }
                    }

                    /* Convert the key code to the table index and send the toggle event with the table index */
                    String[][] cycleTable = getCycleTable();
                    if (cycleTable == null) {
                        Log.e(TAG, "not founds cycle table");
                    } else {
                        int index = getTableIndex(primaryCode);
                        mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.TOGGLE_CHAR, cycleTable[index]));
                        mCurrentCycleTable = cycleTable[index];
                    }
                    mPrevInputKeyCode = primaryCode;
                }
                break;

            case KEYCODE_JP12_ASTER:
                if (mInputType == INPUT_TYPE_INSTANT) {
                    commitText();
                    mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.INPUT_CHAR,
                            mCurrentInstantTable[getTableIndex(primaryCode)]));
                } else {
                    if (!mNoInput) {
                        /* Processing to toggle Dakuten, Handakuten, and capital */
                        HashMap replaceTable = getReplaceTable();
                        if (replaceTable == null) {
                            Log.e("OpenWnn", "not founds replace table");
                        } else {
                            mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.REPLACE_CHAR, replaceTable));
                            mPrevInputKeyCode = primaryCode;
                        }
                    }
                }
                break;

            case KEYCODE_SWITCH_FULL_HIRAGANA:
                /* Change mode to Full width hiragana */
                changeKeyMode(KEYMODE_JA_FULL_HIRAGANA);
                break;

            case KEYCODE_SWITCH_FULL_KATAKANA:
                /* Change mode to Full width katakana */
                changeKeyMode(KEYMODE_JA_FULL_KATAKANA);
                break;

            case KEYCODE_SWITCH_FULL_ALPHABET:
                /* Change mode to Full width alphabet */
                changeKeyMode(KEYMODE_JA_FULL_ALPHABET);
                break;

            case KEYCODE_SWITCH_FULL_NUMBER:
                /* Change mode to Full width numeric */
                changeKeyMode(KEYMODE_JA_FULL_NUMBER);
                break;

            case KEYCODE_SWITCH_HALF_KATAKANA:
                /* Change mode to Half width katakana */
                changeKeyMode(KEYMODE_JA_HALF_KATAKANA);
                break;

            case KEYCODE_SWITCH_HALF_ALPHABET:
                /* Change mode to Half width alphabet */
                changeKeyMode(KEYMODE_JA_HALF_ALPHABET);
                break;

            case KEYCODE_SWITCH_HALF_NUMBER:
                /* Change mode to Half width numeric */
                changeKeyMode(KEYMODE_JA_HALF_NUMBER);
                break;


            case KEYCODE_SELECT_CASE:
                int shifted = (mShiftOn == 0) ? 1 : 0;
                Keyboard newKeyboard = getShiftChangeKeyboard(shifted);
                if (newKeyboard != null) {
                    mShiftOn = shifted;
                    changeKeyboard(newKeyboard);
                }
                break;

            case KEYCODE_JP12_SPACE:
                if ((mCurrentKeyMode == KEYMODE_JA_FULL_HIRAGANA) && !mNoInput) {
                    mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.CONVERT));
                } else {
                    mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.INPUT_CHAR, ' '));
                }
                break;

            case KEYCODE_EISU_KANA:
                mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.CHANGE_MODE, OpenWnnJAJP.ENGINE_MODE_EISU_KANA));
                break;

            case KEYCODE_JP12_CLOSE:
                mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.INPUT_KEY,
                        new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)));
                break;

            case KEYCODE_JP12_LEFT:
                mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.INPUT_SOFT_KEY,
                        new KeyEvent(KeyEvent.ACTION_DOWN,
                                KeyEvent.KEYCODE_DPAD_LEFT)));
                break;

            case KEYCODE_JP12_RIGHT:
                mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.INPUT_SOFT_KEY,
                        new KeyEvent(KeyEvent.ACTION_DOWN,
                                KeyEvent.KEYCODE_DPAD_RIGHT)));
                break;
            case KEYCODE_NOP:
                break;

            default:
                if (primaryCode >= 0) {
                    if (mKeyboardView.isShifted()) {
                        primaryCode = Character.toUpperCase(primaryCode);
                    }
                    mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.INPUT_CHAR, (char)primaryCode));
                }
                break;
        }

        /* update shift key's state */
        if (!mCapsLock && (primaryCode != DefaultSoftKeyboard.KEYCODE_QWERTY_SHIFT)) {
            setShiftByEditorInfo();
        }

        switch(primaryCode) {
            case KEYCODE_CHANGE_LANG:
                EventBus.getDefault().post(new InputSoftKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_CHANGE_LANG)));
                break;

            case KEYCODE_UP:
                EventBus.getDefault().post(new InputSoftKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP)));
                break;

            case KEYCODE_DOWN:
                EventBus.getDefault().post(new InputSoftKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN)));
                break;

            case KEYCODE_LEFT:
                EventBus.getDefault().post(new InputSoftKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT)));
                break;

            case KEYCODE_RIGHT:
                EventBus.getDefault().post(new InputSoftKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT)));
                break;

            case KEYCODE_JP12_BACKSPACE:
            case KEYCODE_QWERTY_BACKSPACE:
                EventBus.getDefault().post(new InputSoftKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)));
                break;

            case KEYCODE_QWERTY_SHIFT:
                mCapsLock = false;
                toggleShiftLock();
                updateKeyLabels();
                if(mShiftOn == 0) {
                    EventBus.getDefault().post(new InputSoftKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT)));
                } else {
                    EventBus.getDefault().post(new InputSoftKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_RIGHT)));;
                }
                break;

            case KEYCODE_QWERTY_ALT:
                processAltKey();
                break;

            case KEYCODE_JP12_ENTER:
            case KEYCODE_QWERTY_ENTER:
                EventBus.getDefault().post(new InputSoftKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)));
                break;

            case KEYCODE_JP12_SPACE:
            case -10:
                EventBus.getDefault().post(new InputSoftKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE)));
                break;

            default:
                if((primaryCode <= -200 && primaryCode > -300) || (primaryCode <= -2000 && primaryCode > -3000)) {
                    EventBus.getDefault().post(new InputSoftKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, primaryCode)));
                } else if(primaryCode >= 0) {
                    if(mKeyboardView.isShifted()) {
                        primaryCode = Character.toUpperCase(primaryCode);
                    }
                    EventBus.getDefault().post(new InputCharEvent((char) primaryCode));

                    if(mKeyboardView.isShifted()) {
                        if(!mCapsLock) {
                            onKey(KEYCODE_QWERTY_SHIFT);
                            OpenWnnKOKR kokr = (OpenWnnKOKR) mWnn;
                            if(!mHardKeyboardHidden) kokr.resetHardShift(false);
                            kokr.updateMetaKeyStateDisplay();
                        }
                    }
                }
                break;
        }
        if (!mCapsLock && (primaryCode != DefaultSoftKeyboard.KEYCODE_QWERTY_SHIFT)) {

        }
        if(mTimeoutHandler == null && mTimeoutDelay > 0) {
            mTimeoutHandler = new Handler();
            mTimeoutHandler.postDelayed(new TimeOutHandler(), mTimeoutDelay);
        }
    }

    /** @see io.rivmt.keyboard.openwnn.DefaultSoftKeyboard#setPreferences */
    @Override public void setPreferences(SharedPreferences pref, EditorInfo editor) {
        super.setPreferences(pref, editor);

        int inputType = editor.inputType;
        if (inputType == EditorInfo.TYPE_NULL) {
            return;
        }

        mEnableAutoCaps = pref.getBoolean("auto_caps", true);
        mFixedKeyMode = INVALID_KEYMODE;
        mPreferenceKeyMode = INVALID_KEYMODE;

        switch (inputType & EditorInfo.TYPE_MASK_CLASS) {

        case EditorInfo.TYPE_CLASS_NUMBER:
        case EditorInfo.TYPE_CLASS_DATETIME:
            mPreferenceKeyMode = KEYMODE_JA_HALF_NUMBER;
            break;

        case EditorInfo.TYPE_CLASS_PHONE:
            mFixedKeyMode = KEYMODE_JA_HALF_PHONE;
            break;

        case EditorInfo.TYPE_CLASS_TEXT:
            switch (inputType & EditorInfo.TYPE_MASK_VARIATION) {

            case EditorInfo.TYPE_TEXT_VARIATION_PASSWORD:
                mPreferenceKeyMode = KEYMODE_JA_HALF_ALPHABET;
                break;

            case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
            case EditorInfo.TYPE_TEXT_VARIATION_URI:
                mPreferenceKeyMode = KEYMODE_JA_HALF_ALPHABET;
                break;

            default:
                break;
            }
            break;

        default:
            break;
        }

        if (inputType != mLastInputType) {
            setDefaultKeyboard();
            mLastInputType = inputType;
        }

        mShowKeyPreview = pref.getBoolean("popup_preview", true);

        setShiftByEditorInfo();
    }

    /** @see io.rivmt.keyboard.openwnn.DefaultSoftKeyboard#onUpdateState */
    @Override public void onUpdateState(OpenWnn parent) {
        super.onUpdateState(parent);
        setShiftByEditorInfo();
    }

    /**
     * Change the keyboard to default
     */
    public void setDefaultKeyboard() {
        Locale locale = Locale.getDefault();
        int keymode = KEYMODE_JA_FULL_HIRAGANA;

        if (mPreferenceKeyMode != INVALID_KEYMODE) {
            keymode = mPreferenceKeyMode;
        } else {
            if (!locale.getLanguage().equals(Locale.JAPANESE.getLanguage())) {
                keymode = KEYMODE_JA_HALF_ALPHABET;
            }
        }
        changeKeyMode(keymode);
    }

    
    /**
     * Change to the next input mode
     */
    private void nextKeyMode() {
    	/* Search the current mode in the toggle table */
    	boolean found = false;
        int index;
        for (index = 0; index < JP_MODE_CYCLE_TABLE.length; index++) {
            if (JP_MODE_CYCLE_TABLE[index] == mCurrentKeyMode) {
                found = true;
                break;
            }
        }

        if (!found) {
        	/* If the current mode not exists, set the default mode */
        	setDefaultKeyboard();
        } else {
        	/* If the current mode exists, set the next input mode */
        	index++;
            if (JP_MODE_CYCLE_TABLE.length <= index) {
                index = 0;
            }
            changeKeyMode(JP_MODE_CYCLE_TABLE[index]);
        }
    }

    /**
     * Create the keyboard for portrait mode
     * <br>
     * @param parent  The context
     */
    private void createKeyboardsPortrait(OpenWnn parent) {
        Keyboard[][] keyList;
        /* qwerty shift_off (portrait) */
        keyList = mKeyboard[LANG_JA][PORTRAIT][KEYBOARD_QWERTY][KEYBOARD_SHIFT_OFF];
        keyList[KEYMODE_JA_FULL_HIRAGANA][0] = new Keyboard(parent, R.xml.keyboard_qwerty);
        keyList[KEYMODE_JA_FULL_ALPHABET][0] = new Keyboard(parent, R.xml.keyboard_qwerty_full_alphabet);
        keyList[KEYMODE_JA_FULL_NUMBER][0]   = new Keyboard(parent, R.xml.keyboard_qwerty_full_symbols);
        keyList[KEYMODE_JA_FULL_KATAKANA][0] = new Keyboard(parent, R.xml.keyboard_qwerty_full_katakana);
        keyList[KEYMODE_JA_HALF_ALPHABET][0] = new Keyboard(parent, R.xml.keyboard_qwerty_half_alphabet);
        keyList[KEYMODE_JA_HALF_NUMBER][0]   = new Keyboard(parent, R.xml.keyboard_qwerty_half_symbols);
        keyList[KEYMODE_JA_HALF_KATAKANA][0] = new Keyboard(parent, R.xml.keyboard_qwerty_half_katakana);
        keyList[KEYMODE_JA_HALF_PHONE][0]    = new Keyboard(parent, R.xml.keyboard_12key_phone);

        /* qwerty shift_on (portrait) */
        keyList = mKeyboard[LANG_JA][PORTRAIT][KEYBOARD_QWERTY][KEYBOARD_SHIFT_ON];
        keyList[KEYMODE_JA_FULL_HIRAGANA][0] =
            mKeyboard[LANG_JA][PORTRAIT][KEYBOARD_QWERTY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_FULL_HIRAGANA][0];
        keyList[KEYMODE_JA_FULL_ALPHABET][0] = new Keyboard(parent, R.xml.keyboard_qwerty_full_alphabet_shift);
        keyList[KEYMODE_JA_FULL_NUMBER][0]   = new Keyboard(parent, R.xml.keyboard_qwerty_full_symbols_shift);
        keyList[KEYMODE_JA_FULL_KATAKANA][0] =
            mKeyboard[LANG_JA][PORTRAIT][KEYBOARD_QWERTY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_FULL_KATAKANA][0];
        keyList[KEYMODE_JA_HALF_ALPHABET][0] =
            mKeyboard[LANG_JA][PORTRAIT][KEYBOARD_QWERTY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_HALF_ALPHABET][0];
        keyList[KEYMODE_JA_HALF_NUMBER][0]   = new Keyboard(parent, R.xml.keyboard_qwerty_half_symbols_shift);
        keyList[KEYMODE_JA_HALF_KATAKANA][0] =
            mKeyboard[LANG_JA][PORTRAIT][KEYBOARD_QWERTY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_HALF_KATAKANA][0];
        keyList[KEYMODE_JA_HALF_PHONE][0] =
            mKeyboard[LANG_JA][PORTRAIT][KEYBOARD_QWERTY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_HALF_PHONE][0];


        /* 12-keys shift_off (portrait) */
        keyList = mKeyboard[LANG_JA][PORTRAIT][KEYBOARD_12KEY][KEYBOARD_SHIFT_OFF];
        keyList[KEYMODE_JA_FULL_HIRAGANA][0] = new Keyboard(parent, R.xml.keyboard_12keyjp);
        keyList[KEYMODE_JA_FULL_HIRAGANA][1] = new Keyboard(parent, R.xml.keyboard_12keyjp_input);
        keyList[KEYMODE_JA_FULL_ALPHABET][0] = new Keyboard(parent, R.xml.keyboard_12key_alphabet);
        keyList[KEYMODE_JA_FULL_ALPHABET][1] = new Keyboard(parent, R.xml.keyboard_12key_alphabet_input);
        keyList[KEYMODE_JA_FULL_NUMBER][0]   = new Keyboard(parent, R.xml.keyboard_12key_num);
        keyList[KEYMODE_JA_FULL_KATAKANA][0] = new Keyboard(parent, R.xml.keyboard_12key_katakana);
        keyList[KEYMODE_JA_FULL_KATAKANA][1] = new Keyboard(parent, R.xml.keyboard_12key_katakana_input);
        keyList[KEYMODE_JA_HALF_ALPHABET][0] = new Keyboard(parent, R.xml.keyboard_12key_alphabet);
        keyList[KEYMODE_JA_HALF_ALPHABET][1] = new Keyboard(parent, R.xml.keyboard_12key_alphabet_input);
        keyList[KEYMODE_JA_HALF_NUMBER][0]   = new Keyboard(parent, R.xml.keyboard_12key_num);
        keyList[KEYMODE_JA_HALF_KATAKANA][0] = new Keyboard(parent, R.xml.keyboard_12key_katakana);
        keyList[KEYMODE_JA_HALF_KATAKANA][1] = new Keyboard(parent, R.xml.keyboard_12key_katakana_input);
        keyList[KEYMODE_JA_HALF_PHONE][0]    = new Keyboard(parent, R.xml.keyboard_12key_phone);

        /* 12-keys shift_on (portrait) */
        keyList = mKeyboard[LANG_JA][PORTRAIT][KEYBOARD_12KEY][KEYBOARD_SHIFT_ON];
        keyList[KEYMODE_JA_FULL_HIRAGANA]
            = mKeyboard[LANG_JA][PORTRAIT][KEYBOARD_12KEY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_FULL_HIRAGANA];
        keyList[KEYMODE_JA_FULL_ALPHABET]
            = mKeyboard[LANG_JA][PORTRAIT][KEYBOARD_12KEY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_FULL_ALPHABET];
        keyList[KEYMODE_JA_FULL_NUMBER]
            = mKeyboard[LANG_JA][PORTRAIT][KEYBOARD_12KEY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_FULL_NUMBER];
        keyList[KEYMODE_JA_FULL_KATAKANA]
            = mKeyboard[LANG_JA][PORTRAIT][KEYBOARD_12KEY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_FULL_KATAKANA];
        keyList[KEYMODE_JA_HALF_ALPHABET]
            = mKeyboard[LANG_JA][PORTRAIT][KEYBOARD_12KEY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_HALF_ALPHABET];;
        keyList[KEYMODE_JA_HALF_NUMBER]
            = mKeyboard[LANG_JA][PORTRAIT][KEYBOARD_12KEY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_HALF_NUMBER];
        keyList[KEYMODE_JA_HALF_KATAKANA]
            = mKeyboard[LANG_JA][PORTRAIT][KEYBOARD_12KEY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_HALF_KATAKANA];
        keyList[KEYMODE_JA_HALF_PHONE]
            = mKeyboard[LANG_JA][PORTRAIT][KEYBOARD_12KEY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_HALF_PHONE];

    }

    /**
     * Create the keyboard for landscape mode
     * <br>
     * @param parent  The context
     */
    private void createKeyboardsLandscape(OpenWnn parent) {
        Keyboard[][] keyList;
        /* qwerty shift_off (landscape) */
        keyList = mKeyboard[LANG_JA][LANDSCAPE][KEYBOARD_QWERTY][KEYBOARD_SHIFT_OFF];
        keyList[KEYMODE_JA_FULL_HIRAGANA][0] = new Keyboard(parent, R.xml.keyboard_qwerty_landscape);
        keyList[KEYMODE_JA_FULL_ALPHABET][0] = new Keyboard(parent, R.xml.keyboard_qwerty_full_alphabet_landscape);
        keyList[KEYMODE_JA_FULL_NUMBER][0]   = new Keyboard(parent, R.xml.keyboard_qwerty_full_symbols_landscape);
        keyList[KEYMODE_JA_FULL_KATAKANA][0] = new Keyboard(parent, R.xml.keyboard_qwerty_full_katakana_landscape);
        keyList[KEYMODE_JA_HALF_ALPHABET][0] = new Keyboard(parent, R.xml.keyboard_qwerty_half_alphabet_landscape);
        keyList[KEYMODE_JA_HALF_NUMBER][0]   = new Keyboard(parent, R.xml.keyboard_qwerty_half_symbols_landscape);
        keyList[KEYMODE_JA_HALF_KATAKANA][0] = new Keyboard(parent, R.xml.keyboard_qwerty_half_katakana_landscape);
        keyList[KEYMODE_JA_HALF_PHONE][0]    = new Keyboard(parent, R.xml.keyboard_12key_phone_landscape);

        /* qwerty shift_on (landscape) */
        keyList = mKeyboard[LANG_JA][LANDSCAPE][KEYBOARD_QWERTY][KEYBOARD_SHIFT_ON];
        keyList[KEYMODE_JA_FULL_HIRAGANA][0] =
            mKeyboard[LANG_JA][LANDSCAPE][KEYBOARD_QWERTY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_FULL_HIRAGANA][0];
        keyList[KEYMODE_JA_FULL_ALPHABET][0] = new Keyboard(parent, R.xml.keyboard_qwerty_full_alphabet_shift_landscape);
        keyList[KEYMODE_JA_FULL_NUMBER][0]   = new Keyboard(parent, R.xml.keyboard_qwerty_full_symbols_shift_landscape);
        keyList[KEYMODE_JA_FULL_KATAKANA][0] =
            mKeyboard[LANG_JA][LANDSCAPE][KEYBOARD_QWERTY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_FULL_KATAKANA][0];
        keyList[KEYMODE_JA_HALF_ALPHABET][0] =
            mKeyboard[LANG_JA][LANDSCAPE][KEYBOARD_QWERTY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_HALF_ALPHABET][0];
        keyList[KEYMODE_JA_HALF_NUMBER][0]   = new Keyboard(parent, R.xml.keyboard_qwerty_half_symbols_shift_landscape);
        keyList[KEYMODE_JA_HALF_KATAKANA][0] =
            mKeyboard[LANG_JA][LANDSCAPE][KEYBOARD_QWERTY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_HALF_KATAKANA][0];
        keyList[KEYMODE_JA_HALF_PHONE][0] =
            mKeyboard[LANG_JA][LANDSCAPE][KEYBOARD_QWERTY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_HALF_PHONE][0];

        /* 12-keys shift_off (landscape) */
        keyList = mKeyboard[LANG_JA][LANDSCAPE][KEYBOARD_12KEY][KEYBOARD_SHIFT_OFF];
        keyList[KEYMODE_JA_FULL_HIRAGANA][0] = new Keyboard(parent, R.xml.keyboard_12keyjp_landscape);
        keyList[KEYMODE_JA_FULL_HIRAGANA][1] = new Keyboard(parent, R.xml.keyboard_12keyjp_input_landscape);
        keyList[KEYMODE_JA_FULL_ALPHABET][0] = new Keyboard(parent, R.xml.keyboard_12key_alphabet_landscape);
        keyList[KEYMODE_JA_FULL_ALPHABET][1] = new Keyboard(parent, R.xml.keyboard_12key_alphabet_input_landscape);
        keyList[KEYMODE_JA_FULL_NUMBER][0]   = new Keyboard(parent, R.xml.keyboard_12key_num_landscape);
        keyList[KEYMODE_JA_FULL_KATAKANA][0] = new Keyboard(parent, R.xml.keyboard_12key_katakana_landscape);
        keyList[KEYMODE_JA_FULL_KATAKANA][1] = new Keyboard(parent, R.xml.keyboard_12key_katakana_input_landscape);
        keyList[KEYMODE_JA_HALF_ALPHABET][0] = new Keyboard(parent, R.xml.keyboard_12key_alphabet_landscape);
        keyList[KEYMODE_JA_HALF_ALPHABET][1] = new Keyboard(parent, R.xml.keyboard_12key_alphabet_input_landscape);
        keyList[KEYMODE_JA_HALF_NUMBER][0]   = new Keyboard(parent, R.xml.keyboard_12key_num_landscape);
        keyList[KEYMODE_JA_HALF_KATAKANA][0] = new Keyboard(parent, R.xml.keyboard_12key_katakana_landscape);
        keyList[KEYMODE_JA_HALF_KATAKANA][1] = new Keyboard(parent, R.xml.keyboard_12key_katakana_input_landscape);
        keyList[KEYMODE_JA_HALF_PHONE][0]    = new Keyboard(parent, R.xml.keyboard_12key_phone_landscape);

        /* 12-keys shift_on (landscape) */
        keyList = mKeyboard[LANG_JA][LANDSCAPE][KEYBOARD_12KEY][KEYBOARD_SHIFT_ON];
        keyList[KEYMODE_JA_FULL_HIRAGANA]
            = mKeyboard[LANG_JA][LANDSCAPE][KEYBOARD_12KEY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_FULL_HIRAGANA];
        keyList[KEYMODE_JA_FULL_ALPHABET]
            = mKeyboard[LANG_JA][LANDSCAPE][KEYBOARD_12KEY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_FULL_ALPHABET];
        keyList[KEYMODE_JA_FULL_NUMBER]
            = mKeyboard[LANG_JA][LANDSCAPE][KEYBOARD_12KEY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_FULL_NUMBER];
        keyList[KEYMODE_JA_FULL_KATAKANA]
            = mKeyboard[LANG_JA][LANDSCAPE][KEYBOARD_12KEY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_FULL_KATAKANA];
        keyList[KEYMODE_JA_HALF_ALPHABET]
            = mKeyboard[LANG_JA][LANDSCAPE][KEYBOARD_12KEY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_HALF_ALPHABET];;
        keyList[KEYMODE_JA_HALF_NUMBER]
            = mKeyboard[LANG_JA][LANDSCAPE][KEYBOARD_12KEY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_HALF_NUMBER];
        keyList[KEYMODE_JA_HALF_KATAKANA]
            = mKeyboard[LANG_JA][LANDSCAPE][KEYBOARD_12KEY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_HALF_KATAKANA];
        keyList[KEYMODE_JA_HALF_PHONE]
            = mKeyboard[LANG_JA][LANDSCAPE][KEYBOARD_12KEY][KEYBOARD_SHIFT_OFF][KEYMODE_JA_HALF_PHONE];
    }

    /**
     * Convert the key code to the index of table
     * <br>
     * @param keyCode     The key code
     * @return          The index of the toggle table for input
     */
    private int getTableIndex(int keyCode) {
        int index =
            (keyCode == KEYCODE_JP12_1)     ?  0 :
            (keyCode == KEYCODE_JP12_2)     ?  1 :
            (keyCode == KEYCODE_JP12_3)     ?  2 :
            (keyCode == KEYCODE_JP12_4)     ?  3 :
            (keyCode == KEYCODE_JP12_5)     ?  4 :
            (keyCode == KEYCODE_JP12_6)     ?  5 :
            (keyCode == KEYCODE_JP12_7)     ?  6 :
            (keyCode == KEYCODE_JP12_8)     ?  7 :
            (keyCode == KEYCODE_JP12_9)     ?  8 :
            (keyCode == KEYCODE_JP12_0)     ?  9 :
            (keyCode == KEYCODE_JP12_SHARP) ? 10 :
            (keyCode == KEYCODE_JP12_ASTER) ? 11 :
            0;

        return index;
    }

    /**
     * Get the toggle table for input that is appropriate in current mode.
     * 
     * @return      The toggle table for input
     */
    private String[][] getCycleTable() {
        String[][] cycleTable = null;
        switch (mCurrentKeyMode) {
        case KEYMODE_JA_FULL_HIRAGANA:
            cycleTable = JP_FULL_HIRAGANA_CYCLE_TABLE;
            break;

        case KEYMODE_JA_FULL_KATAKANA:
            cycleTable = JP_FULL_KATAKANA_CYCLE_TABLE;
            break;

        case KEYMODE_JA_FULL_ALPHABET:
            cycleTable = JP_FULL_ALPHABET_CYCLE_TABLE;
            break;

        case KEYMODE_JA_FULL_NUMBER:
        case KEYMODE_JA_HALF_NUMBER:
        	/* Because these modes belong to direct input group, No toggle table exists */ 
            break;

        case KEYMODE_JA_HALF_ALPHABET:
            cycleTable = JP_HALF_ALPHABET_CYCLE_TABLE;
            break;

        case KEYMODE_JA_HALF_KATAKANA:
            cycleTable = JP_HALF_KATAKANA_CYCLE_TABLE;
            break;

        default:
            break;
        }
        return cycleTable;
    }

    /**
     * Get the replace table that is appropriate in current mode.
     * 
     * @return      The replace table
     */
    private HashMap getReplaceTable() {
        HashMap hashTable = null;
        switch (mCurrentKeyMode) {
        case KEYMODE_JA_FULL_HIRAGANA:
            hashTable = JP_FULL_HIRAGANA_REPLACE_TABLE;
            break;
        case KEYMODE_JA_FULL_KATAKANA:
            hashTable = JP_FULL_KATAKANA_REPLACE_TABLE;
            break;

        case KEYMODE_JA_FULL_ALPHABET:
            hashTable = JP_FULL_ALPHABET_REPLACE_TABLE;
            break;

        case KEYMODE_JA_FULL_NUMBER:
        case KEYMODE_JA_HALF_NUMBER:
        	/* Because these modes belong to direct input group, No replacing table exists */ 
            break;

        case KEYMODE_JA_HALF_ALPHABET:
            hashTable = JP_HALF_ALPHABET_REPLACE_TABLE;
            break;

        case KEYMODE_JA_HALF_KATAKANA:
            hashTable = JP_HALF_KATAKANA_REPLACE_TABLE;
            break;

        default:
            break;
        }
        return hashTable;
    }
    
    /**
     * Set the status icon that is appropriate in current mode
     */
    private void setStatusIcon() {
        int icon = 0;

        switch (mCurrentKeyMode) {
        case KEYMODE_JA_FULL_HIRAGANA:
            icon = R.drawable.immodeic_hiragana;
            break;
        case KEYMODE_JA_FULL_KATAKANA:
            icon = R.drawable.immodeic_full_kana;
            break;
        case KEYMODE_JA_FULL_ALPHABET:
            icon = R.drawable.immodeic_full_alphabet;
            break;
        case KEYMODE_JA_FULL_NUMBER:
            icon = R.drawable.immodeic_full_number;
            break;
        case KEYMODE_JA_HALF_KATAKANA:
            icon = R.drawable.immodeic_half_kana;
            break;
        case KEYMODE_JA_HALF_ALPHABET:
            icon = R.drawable.immodeic_half_alphabet;
            break;
        case KEYMODE_JA_HALF_NUMBER:
        case KEYMODE_JA_HALF_PHONE:
            icon = R.drawable.immodeic_half_number;
            break;
        default:
            break;
        }

        mWnn.showStatusIcon(icon);
    }

    /**
     * Get the shift key state from the editor.
     * <br>
     * @param editor	The editor information
     * @return			state ID of the shift key (0:off, 1:on)
     */
    protected int getShiftKeyState(EditorInfo editor) {
        int caps = mWnn.getCurrentInputConnection().getCursorCapsMode(editor.inputType);
        return (caps == 0) ? 0 : 1;
    }

    /**
     * Set the shift key state from {@link EditorInfo}.
     */
    private void setShiftByEditorInfo() {
        if (mEnableAutoCaps && (mCurrentKeyMode == KEYMODE_JA_HALF_ALPHABET)) {
            int shift = getShiftKeyState(mWnn.getCurrentInputEditorInfo());
            
            mShiftOn = shift;
            changeKeyboard(getShiftChangeKeyboard(shift));
        }
    }

    /** @see io.rivmt.keyboard.openwnn.DefaultSoftKeyboard#setHardKeyboardHidden */
    @Override public void setHardKeyboardHidden(boolean hidden) {
        if ((mWnn != null) && !mHardKeyboardHidden) {
            mWnn.onEvent(new InputJAJPEvent(InputJAJPEvent.CHANGE_MODE,
            		OpenWnnJAJP.ENGINE_MODE_OPT_TYPE_QWERTY));
        }
        super.setHardKeyboardHidden(hidden);
    }

    public boolean getDisableUP(){
        if(mKeyboardView != null) {
            if(mKeyboardView instanceof  DefaultSoftKeyboardViewJAJP) {
                DefaultSoftKeyboardViewJAJP keyboardView = ((DefaultSoftKeyboardViewJAJP) mKeyboardView);
                return keyboardView.upDisable();
            }
        }
        return false;
    }
}



