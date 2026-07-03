package com.hinglish.cipherkeyboard;

import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

public class HinglishCipherKeyboard extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {

    private static final String TAG = "HinglishCipherKeyboard";

    private KeyboardView keyboardView;
    private Keyboard qwertyKeyboard;

    private Button btnModeConvert;
    private Button btnModeDecode;
    private Button btnConvertAction;
    private ScrollView previewScrollContainer;
    private TextView tvPreviewPane;

    // Mode tracking
    private static final int MODE_CONVERT = 0;
    private static final int MODE_DECODE = 1;
    private int currentMode = MODE_CONVERT;

    @Override
    public View onCreateInputView() {
        View rootView = LayoutInflater.from(this).inflate(R.layout.keyboard_layout, null);

        // Bind views
        keyboardView = rootView.findViewById(R.id.keyboard_view);
        btnModeConvert = rootView.findViewById(R.id.btn_mode_convert);
        btnModeDecode = rootView.findViewById(R.id.btn_mode_decode);
        btnConvertAction = rootView.findViewById(R.id.btn_convert_action);
        previewScrollContainer = rootView.findViewById(R.id.preview_scroll_container);
        tvPreviewPane = rootView.findViewById(R.id.tv_preview_pane);

        // Setup keyboard
        qwertyKeyboard = new Keyboard(this, R.xml.qwerty);
        keyboardView.setKeyboard(qwertyKeyboard);
        keyboardView.setOnKeyboardActionListener(this);

        // Mode switch listeners
        btnModeConvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToConvertMode();
            }
        });

        btnModeDecode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToDecodeMode();
            }
        });

        // Main action button listener
        btnConvertAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentMode == MODE_CONVERT) {
                    handleConvertAction();
                } else {
                    handleDecodeAction();
                }
            }
        });

        // Default state on view creation
        switchToConvertMode();

        return rootView;
    }

    // ---------------- MODE SWITCHING ----------------

    private void switchToConvertMode() {
        currentMode = MODE_CONVERT;
        previewScrollContainer.setVisibility(View.GONE);
        btnConvertAction.setText("⇄ CONVERT TEXT TO NUMBER");
    }

    private void switchToDecodeMode() {
        currentMode = MODE_DECODE;
        previewScrollContainer.setVisibility(View.VISIBLE);
        btnConvertAction.setText("⇄ DECODE FROM CLIPBOARD");
        // Auto-read clipboard the moment user enters Decode mode
        autoReadClipboardAndDecode();
    }

    // ---------------- CONVERT MODE LOGIC ----------------

    private void handleConvertAction() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            showPreviewError("Error: No active input connection found.");
            return;
        }

        // Read all text currently present in the input field before the cursor
        CharSequence beforeCursor = ic.getExtractedText(
                new android.view.inputmethod.ExtractedTextRequest(), 0) != null
                ? ic.getExtractedText(new android.view.inputmethod.ExtractedTextRequest(), 0).text
                : null;

        String sourceText = (beforeCursor != null) ? beforeCursor.toString() : "";

        if (sourceText.trim().isEmpty()) {
            showPreviewError("Error: No text found in input field to convert.");
            return;
        }

        String cipherResult = convertTextToCipher(sourceText);

        // Replace entire field content with cipher result
        ic.beginBatchEdit();
        ic.performContextMenuAction(android.R.id.selectAll);
        ic.commitText(cipherResult, 1);
        ic.endBatchEdit();
    }

    private String convertTextToCipher(String input) {
        StringBuilder result = new StringBuilder();
        String[] words = input.trim().split("\\s+");

        for (int w = 0; w < words.length; w++) {
            String word = words[w];
            StringBuilder wordCodes = new StringBuilder();

            for (int i = 0; i < word.length(); i++) {
                char c = Character.toLowerCase(word.charAt(i));
                if (c >= 'a' && c <= 'z') {
                    int code = (c - 'a') + 1;
                    if (wordCodes.length() > 0) {
                        wordCodes.append(",");
                    }
                    wordCodes.append(code);
                }
            }

            if (wordCodes.length() > 0) {
                result.append(wordCodes);
                if (w < words.length - 1) {
                    result.append("/");
                }
            }
        }

        return result.toString();
    }

    // ---------------- DECODE MODE LOGIC ----------------

    private void handleDecodeAction() {
        autoReadClipboardAndDecode();
    }

    private void autoReadClipboardAndDecode() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        if (clipboard == null || !clipboard.hasPrimaryClip()) {
            showPreviewError("Error: Clipboard is empty. Copy cipher text first.");
            return;
        }

        ClipData clipData = clipboard.getPrimaryClip();
        if (clipData == null || clipData.getItemCount() == 0) {
            showPreviewError("Error: Clipboard has no readable content.");
            return;
        }

        CharSequence clipText = clipData.getItemAt(0).coerceToText(this);
        String cipherInput = (clipText != null) ? clipText.toString().trim() : "";

        if (cipherInput.isEmpty()) {
            showPreviewError("Error: Clipboard text is empty.");
            return;
        }

        try {
            String decoded = decodeCipherToText(cipherInput);
            if (decoded.trim().isEmpty()) {
                showPreviewError("Error: Clipboard text does not match cipher format.");
            } else {
                showPreviewSuccess(decoded);
            }
        } catch (Exception e) {
            showPreviewError("Error: Invalid cipher syntax. " + e.getMessage());
        }
    }

    private String decodeCipherToText(String cipherInput) {
        StringBuilder result = new StringBuilder();
        String[] words = cipherInput.split("/");

        for (int w = 0; w < words.length; w++) {
            String word = words[w].trim();
            if (word.isEmpty()) {
                continue;
            }

            String[] codes = word.split(",");
            StringBuilder decodedWord = new StringBuilder();

            for (String codeStr : codes) {
                codeStr = codeStr.trim();
                if (codeStr.isEmpty()) {
                    continue;
                }

                int code;
                try {
                    code = Integer.parseInt(codeStr);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Non-numeric value '" + codeStr + "' found.");
                }

                if (code < 1 || code > 26) {
                    throw new IllegalArgumentException("Code " + code + " is out of A1Z26 range (1-26).");
                }

                char letter = (char) ('a' + (code - 1));
                decodedWord.append(letter);
            }

            result.append(decodedWord);
            if (w < words.length - 1) {
                result.append(" ");
            }
        }

        return result.toString();
    }

    // ---------------- PREVIEW PANE HELPERS ----------------

    private void showPreviewSuccess(String message) {
        tvPreviewPane.setTextColor(Color.parseColor("#4CAF50")); // Green
        tvPreviewPane.setText(message);
        previewScrollContainer.setVisibility(View.VISIBLE);
    }

    private void showPreviewError(String message) {
        tvPreviewPane.setTextColor(Color.parseColor("#F44336")); // Red
        tvPreviewPane.setText(message);
        previewScrollContainer.setVisibility(View.VISIBLE);
    }

    // ---------------- KeyboardView.OnKeyboardActionListener ----------------

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return;
        }

        switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                ic.deleteSurroundingText(1, 0);
                break;
            case Keyboard.KEYCODE_DONE:
                ic.sendKeyEvent(new android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER));
                break;
            default:
                char code = (char) primaryCode;
                ic.commitText(String.valueOf(code), 1);
        }
    }

    @Override
    public void onPress(int primaryCode) {}

    @Override
    public void onRelease(int primaryCode) {}

    @Override
    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(text, 1);
        }
    }

    @Override public void swipeLeft() {}
