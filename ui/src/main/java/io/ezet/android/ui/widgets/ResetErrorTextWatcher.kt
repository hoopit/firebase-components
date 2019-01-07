package io.ezet.android.ui.widgets

import android.text.Editable
import android.text.TextWatcher

class ResetErrorTextWatcher(
    private val view: com.google.android.material.textfield.TextInputLayout
) : TextWatcher {
    override fun afterTextChanged(s: Editable?) {
        view.error = null
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }
}
