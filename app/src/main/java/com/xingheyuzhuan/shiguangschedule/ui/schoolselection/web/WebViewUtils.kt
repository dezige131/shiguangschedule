package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import android.webkit.WebView

const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

/**
 * 采集网页上的用户名和密码
 */
const val JS_CAPTURE_CREDENTIALS = """
(function() {
    var passwordField = document.querySelector('input[type="password"]');
    if (!passwordField) {
        AndroidBridge.showToast("未找到密码输入框");
        return;
    }
    
    var form = passwordField.form;
    var usernameField = null;
    
    if (form) {
        var inputs = form.querySelectorAll('input');
        for (var i = 0; i < inputs.length; i++) {
            if (inputs[i] !== passwordField && (inputs[i].type === 'text' || inputs[i].type === 'email')) {
                usernameField = inputs[i];
                break;
            }
        }
    }
    
    if (!usernameField) {
        var allInputs = document.querySelectorAll('input');
        var pIdx = Array.from(allInputs).indexOf(passwordField);
        if (pIdx > 0) {
            usernameField = allInputs[pIdx - 1];
        }
    }
    
    var username = usernameField ? usernameField.value : "";
    var password = passwordField.value;
    
    if (!password) {
        AndroidBridge.showToast("密码框为空");
        return;
    }
    
    AndroidBridge.onCredentialsCaptured(username, password);
})();
"""

/**
 * 将解密后的凭据填充到网页
 */
fun getAutofillJs(username: String, password: String): String {
    val escapedUser = username.replace("'", "\\'")
    val escapedPass = password.replace("'", "\\'")
    return """
    (function() {
        var passwordField = document.querySelector('input[type="password"]');
        if (!passwordField) return;
        
        var form = passwordField.form;
        var usernameField = null;
        
        if (form) {
            var inputs = form.querySelectorAll('input');
            for (var i = 0; i < inputs.length; i++) {
                if (inputs[i] !== passwordField && (inputs[i].type === 'text' || inputs[i].type === 'email')) {
                    usernameField = inputs[i];
                    break;
                }
            }
        }
        
        if (!usernameField) {
            var allInputs = document.querySelectorAll('input');
            var pIdx = Array.from(allInputs).indexOf(passwordField);
            if (pIdx > 0) usernameField = allInputs[pIdx - 1];
        }
        
        function setElementValue(el, value) {
            if (!el) return;
            el.value = value;
            el.dispatchEvent(new Event('input', { bubbles: true }));
            el.dispatchEvent(new Event('change', { bubbles: true }));
        }
        
        setElementValue(usernameField, '$escapedUser');
        setElementValue(passwordField, '$escapedPass');
        AndroidBridge.showToast("已填充账号密码");
    })();
    """.trimIndent()
}

/**
 * 注入网页端交互所需的所有业务 JavaScript 代码。
 */
fun WebView.injectAllJavaScript(isDesktopMode: Boolean) {
    evaluateJavascript("""
        window._androidPromiseResolvers = {};
        window._androidPromiseRejectors = {};

        window._resolveAndroidPromise = function(promiseId, result) {
            if (window._androidPromiseResolvers[promiseId]) {
                window._androidPromiseResolvers[promiseId](result);
                delete window._androidPromiseResolvers[promiseId];
                delete window._androidPromiseRejectors[promiseId];
            }
        };

        window._rejectAndroidPromise = function(promiseId, error) {
            if (window._androidPromiseRejectors[promiseId]) {
                window._androidPromiseRejectors[promiseId](new Error(error));
                delete window._androidPromiseResolvers[promiseId];
                delete window._androidPromiseRejectors[promiseId];
            }
        };

        window.AndroidBridgePromise = {
            showAlert: function(title, content, confirmText) {
                return new Promise((resolve, reject) => {
                    const promiseId = 'alert_' + Date.now() + Math.random().toString(36).substring(2);
                    window._androidPromiseResolvers[promiseId] = resolve;
                    window._androidPromiseRejectors[promiseId] = reject;
                    AndroidBridge.showAlert(title, content, confirmText, promiseId);
                });
            },
            showPrompt: function(title, tip, defaultText, validatorJsFunction) {
                return new Promise((resolve, reject) => {
                    const promiseId = 'prompt_' + Date.now() + Math.random().toString(36).substring(2);
                    window._androidPromiseResolvers[promiseId] = resolve;
                    window._androidPromiseRejectors[promiseId] = reject;
                    AndroidBridge.showPrompt(title, tip, defaultText, validatorJsFunction, promiseId);
                });
            },
            showSingleSelection: function(title, itemsJsonString, defaultSelectedIndex) {
                return new Promise((resolve, reject) => {
                    const promiseId = 'singleSelect_' + Date.now() + Math.random().toString(36).substring(2);
                    window._androidPromiseResolvers[promiseId] = resolve;
                    window._androidPromiseRejectors[promiseId] = reject;
                    AndroidBridge.showSingleSelection(title, itemsJsonString, defaultSelectedIndex, promiseId);
                });
            },
            saveImportedCourses: function(coursesJsonString) {
                return new Promise((resolve, reject) => {
                    const promiseId = 'saveCourses_' + Date.now() + Math.random().toString(36).substring(2);
                    window._androidPromiseResolvers[promiseId] = resolve;
                    window._androidPromiseRejectors[promiseId] = reject;
                    AndroidBridge.saveImportedCourses(coursesJsonString, promiseId);
                });
            },
            saveCourseConfig: function(configJsonString) {
                return new Promise((resolve, reject) => {
                    const promiseId = 'saveConfig_' + Date.now() + Math.random().toString(36).substring(2);
                    window._androidPromiseResolvers[promiseId] = resolve;
                    window._androidPromiseRejectors[promiseId] = reject;
                    AndroidBridge.saveCourseConfig(configJsonString, promiseId);
                });
            },
            savePresetTimeSlots: function(timeSlotsJsonString) {
                return new Promise((resolve, reject) => {
                    const promiseId = 'saveTimeSlots_' + Date.now() + Math.random().toString(36).substring(2);
                    window._androidPromiseResolvers[promiseId] = resolve;
                    window._androidPromiseRejectors[promiseId] = reject;
                    AndroidBridge.savePresetTimeSlots(timeSlotsJsonString, promiseId);
                });
            }
        };
    """, null)
}