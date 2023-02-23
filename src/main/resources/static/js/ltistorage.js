"use strict";
//  This code originates from 1EdTech's LTI Postmessage Example Library.
//  We have edited this code for our use.
//  The original can be found here: https://github.com/1EdTech/lti-postmessage-library

var __classPrivateFieldSet =
  (this && this.__classPrivateFieldSet) ||
  function (receiver, state, value, kind, f) {
    if (kind === "m") throw new TypeError("Private method is not writable");
    if (kind === "a" && !f)
      throw new TypeError("Private accessor was defined without a setter");
    if (
      typeof state === "function"
        ? receiver !== state || !f
        : !state.has(receiver)
    )
      throw new TypeError(
        "Cannot write private member to an object whose class did not declare it"
      );
    return (
      kind === "a"
        ? f.call(receiver, value)
        : f
        ? (f.value = value)
        : state.set(receiver, value),
      value
    );
  };
var __classPrivateFieldGet =
  (this && this.__classPrivateFieldGet) ||
  function (receiver, state, kind, f) {
    if (kind === "a" && !f)
      throw new TypeError("Private accessor was defined without a getter");
    if (
      typeof state === "function"
        ? receiver !== state || !f
        : !state.has(receiver)
    )
      throw new TypeError(
        "Cannot read private member from an object whose class did not declare it"
      );
    return kind === "m"
      ? f
      : kind === "a"
      ? f.call(receiver)
      : f
      ? f.value
      : state.get(receiver);
  };
var _LtiStorage_instances,
  _LtiStorage_debug,
  _LtiStorage_setStateAndNonceCookies,
  _LtiPostMessage_instances,
  _LtiPostMessage_debug,
  _LtiPostMessage_getTargetWindow,
  _LtiPostMessage_getTargetFrame,
  _LtiPostMessageLog_debug;
class LtiStorage {
  constructor(debug) {
    _LtiStorage_instances.add(this);
    _LtiStorage_debug.set(this, void 0);
    __classPrivateFieldSet(this, _LtiStorage_debug, debug, "f");
  }
  async initToolLogin(
    platformOidcLoginUrl,
    oidcLoginData,
    launchFrame,
    hasPlatformStorage
  ) {
    return this.setStateAndNonce(
      platformOidcLoginUrl,
      oidcLoginData,
      launchFrame,
      hasPlatformStorage
    )
    .then(this.doLoginInitiationRedirect);
  }
  async setStateAndNonce(
    platformOidcLoginUrl,
    oidcLoginData,
    launchFrame,
    hasPlatformStorage
  ) {
    let launchWindow = launchFrame || window;
    let targetOrigin = new URL(platformOidcLoginUrl).origin;

    return new Promise((resolve, reject) => {
      return resolve(hasPlatformStorage);
    })
      .then(async (hasPlatformStorage) => {
        if (hasPlatformStorage) {
          let platformStorage = this.ltiPostMessage(targetOrigin, launchWindow);

          return platformStorage
            .putData(
              LtiStorage.cookiePrefix + "_state_" + oidcLoginData.state,
              oidcLoginData.state
            )
            .then((stateData) => {
              localStorage.setItem("state", JSON.stringify(stateData)); //storing state in local storage
              window.parent.postMessage(
                //sending state in post message
                stateData,
                new URL(platformOidcLoginUrl).origin
              );
            })
            .then(() => {
              platformStorage
                .putData(
                  LtiStorage.cookiePrefix + "_nonce_" + oidcLoginData.nonce,
                  oidcLoginData.nonce
                )
                .then((nonceData) => {
                  localStorage.setItem("nonce", JSON.stringify(nonceData)); //storing nonce in local storage
                  window.parent.postMessage(
                    //sending nonce in post message
                    nonceData,
                    new URL(platformOidcLoginUrl).origin
                  );
                });
            });
        }
        return Promise.reject();
      })
      .catch((err) => {
        err && console.log(err);
        return __classPrivateFieldGet(
          this,
          _LtiStorage_instances,
          "m",
          _LtiStorage_setStateAndNonceCookies
        ).call(this, oidcLoginData.state, oidcLoginData.nonce);
      })
      .then((hasState) => {
        let data = {
          ...oidcLoginData,
          scope: "openid",
          response_type: "id_token",
          response_mode: "form_post",
          prompt: "none", // Don't prompt user on redirect.
        };
        return {
          url: platformOidcLoginUrl,
          params: data,
          target: hasState ? "_self" : "_blank",
        };
      });
  }
  doLoginInitiationRedirect(formData) {
    //uses formData returned from setStateAndNonce function
    //TODO: when do we need to do this redirect?
    let form = document.createElement("form");
    for (let key in formData.params) {
      let element = document.createElement("input");
      element.type = "hidden";
      element.value = formData.params[key];
      element.name = key;
      form.appendChild(element);
    }
    form.method = "POST";
    form.action = formData.url.toString();
    document.body.appendChild(form);
    form.submit();
  }
  async validateStateAndNonce(state, nonce, platformOrigin, launchFrame) {
    //TODO: this function is not used - do we need it?
    // Check cookie first
    if (
      document.cookie
        .split("; ")
        .includes(LtiStorage.cookiePrefix + "_state_" + state + "=" + state) &&
      document.cookie
        .split("; ")
        .includes(LtiStorage.cookiePrefix + "_nonce_" + nonce + "=" + nonce)
    ) {
      // Found state in cookie, return true
      return Promise.resolve(true);
    }
    let platformStorage = this.ltiPostMessage(platformOrigin, launchFrame);
    return platformStorage
      .getData(LtiStorage.cookiePrefix + "_state_" + state)
      .then((value) => {
        if (!value || state !== value) {
          return Promise.reject();
        }
        return platformStorage.getData(
          LtiStorage.cookiePrefix + "_nonce_" + nonce
        );
      })
      .then((value) => {
        if (!value || nonce !== value) {
          return Promise.reject();
        }
        return true;
      })
      .catch(() => {
        return false;
      });
  }
  ltiPostMessage(targetOrigin, launchFrame) {
    return new LtiPostMessage(
      targetOrigin,
      launchFrame,
      __classPrivateFieldGet(this, _LtiStorage_debug, "f")
    );
  }
}
(_LtiStorage_debug = new WeakMap()),
  (_LtiStorage_instances = new WeakSet()),
  (_LtiStorage_setStateAndNonceCookies =
    function _LtiStorage_setStateAndNonceCookies(state, nonce) {
      document.cookie =
        LtiStorage.cookiePrefix +
        "_state_" +
        state +
        "=" +
        state +
        "; path=/; samesite=none; secure; expires=" +
        new Date(Date.now() + 300 * 1000).toUTCString();
      document.cookie =
        LtiStorage.cookiePrefix +
        "_nonce_" +
        nonce +
        "=" +
        nonce +
        "; path=/; samesite=none; secure; expires=" +
        new Date(Date.now() + 300 * 1000).toUTCString();
      return (
        document.cookie
          .split("; ")
          .includes(
            LtiStorage.cookiePrefix + "_state_" + state + "=" + state
          ) &&
        document.cookie
          .split("; ")
          .includes(LtiStorage.cookiePrefix + "_nonce_" + nonce + "=" + nonce)
      );
    });

LtiStorage.cookiePrefix = "lti";

class LtiPostMessage {
  constructor(targetOrigin, launchFrame, debug) {
    _LtiPostMessage_instances.add(this);
    _LtiPostMessage_debug.set(this, false);
    __classPrivateFieldSet(this, _LtiPostMessage_debug, debug, "f");
    this._targetOrigin = targetOrigin;
    this._launchFrame = launchFrame || window;
  }

  async sendPostMessage(data, targetWindow, originOverride, targetFrameName) {
    return new Promise((resolve, reject) => {
      let log = new LtiPostMessageLog(
        __classPrivateFieldGet(this, _LtiPostMessage_debug, "f")
      );
      let timeout;
      let targetOrigin = originOverride || this._targetOrigin.origin;
      let targetFrame;
      try {
        targetFrame = __classPrivateFieldGet(
          this,
          _LtiPostMessage_instances,
          "m",
          _LtiPostMessage_getTargetFrame
        ).call(this, targetWindow, targetFrameName);
      } catch (e) {
        log.error({
          message:
            "Failed to access target frame with name: [" +
            targetFrameName +
            "] falling back to use target window - Error: [" +
            e +
            "]",
        });
        targetFrameName = undefined;
        targetFrame = targetWindow;
      }
      const messageHandler = (event) => {
        if (event.data.message_id !== data.message_id) {
          log.error({
            message:
              "Ignoring message, invalid message_id: [" +
              event.data.message_id +
              "] expected: [" +
              data.message_id +
              "]",
          });
          return;
        }
        log.response(event);
        if (targetOrigin !== "*" && event.origin !== targetOrigin) {
          log.error({
            message: "Ignoring message, invalid origin: " + event.origin,
          });
          return log.print();
        }
        if (event.data.subject !== data.subject + ".response") {
          log.error({
            message:
              "Ignoring message, invalid subject: [" +
              event.data.subject +
              "] expected: [" +
              data.subject +
              ".response]",
          });
          return log.print();
        }
        window.removeEventListener("message", messageHandler);
        clearTimeout(timeout);
        if (event.data.error) {
          log.error(event.data.error);
          log.print();
          return reject(event.data.error);
        }
        log.print();
        resolve(event.data);
      };
      window.addEventListener("message", messageHandler);
      log.request(targetFrameName, data, targetOrigin);
      targetFrame.postMessage(data, targetOrigin);
      timeout = setTimeout(() => {
        window.removeEventListener("message", messageHandler);
        let timeout_error = {
          code: "timeout",
          message: "No response received after 1000ms",
        };
        log.error(timeout_error);
        log.print();
        reject(timeout_error);
      }, 1000);
    });
  }
  async sendPostMessageIfCapable(data) {
    // Call capability service
    return Promise.any([
      this.sendPostMessage(
        { subject: "lti.capabilities" },
        __classPrivateFieldGet(
          this,
          _LtiPostMessage_instances,
          "m",
          _LtiPostMessage_getTargetWindow
        ).call(this),
        "*"
      ),
      // Send new and old capabilities messages for support with pre-release subjects
      this.sendPostMessage(
        { subject: "org.imsglobal.lti.capabilities" },
        __classPrivateFieldGet(
          this,
          _LtiPostMessage_instances,
          "m",
          _LtiPostMessage_getTargetWindow
        ).call(this),
        "*"
      ),
    ]).then((capabilities) => {
      if (typeof capabilities.supported_messages == "undefined") {
        return Promise.reject({
          code: "not_found",
          message: "No capabilities",
        });
      }
      for (let i = 0; i < capabilities.supported_messages.length; i++) {
        if (
          ![data.subject, "org.imsglobal." + data.subject].includes(
            capabilities.supported_messages[i].subject
          )
        ) {
          continue;
        }
        // Use subject specified in capabilities for backwards compatibility
        data.subject = capabilities.supported_messages[i].subject;
        return this.sendPostMessage(
          data,
          __classPrivateFieldGet(
            this,
            _LtiPostMessage_instances,
            "m",
            _LtiPostMessage_getTargetWindow
          ).call(this),
          undefined,
          capabilities.supported_messages[i].frame
        );
      }
      return Promise.reject({
        code: "not_found",
        message: "Capabilities not found",
      });
    });
  }
  async putData(key, value) {
    function secureRandom(length) {
      let random = new Uint8Array(length||63);
      crypto.getRandomValues(random);
      return btoa(String.fromCharCode(...random)).replace(/\//g, '_').replace(/\+/g, '-');
    }
    return {
      subject: "lti.put_data",
      key: key,
      value: value,
      message_id: 'message-' + secureRandom(15)
    };
  }

  async getData(key) {
    return this.sendPostMessageIfCapable({
      subject: "lti.get_data",
      key: key,
    }).then((response) => {
      return response.value;
    });
  }
}
(_LtiPostMessage_debug = new WeakMap()),
  (_LtiPostMessage_instances = new WeakSet()),
  (_LtiPostMessage_getTargetWindow =
    function _LtiPostMessage_getTargetWindow() {
      return this._launchFrame.opener || this._launchFrame.parent;
    }),
  (_LtiPostMessage_getTargetFrame = function _LtiPostMessage_getTargetFrame(
    targetWindow,
    frameName
  ) {
    if (frameName && targetWindow.frames[frameName]) {
      return targetWindow.frames[frameName];
    }
    return targetWindow;
  });
class LtiPostMessageLog {
  constructor(debug) {
    _LtiPostMessageLog_debug.set(this, false);
    this._request = {};
    this._response = {};
    this._error = [];
    this._start_time = Date.now();
    __classPrivateFieldSet(this, _LtiPostMessageLog_debug, debug, "f");
  }

  request(targetFrameName, data, targetOrigin) {
    this._request = {
      timestamp: Date.now(),
      targetFrameName: targetFrameName,
      data: data,
      targetOrigin: targetOrigin,
    };
  }
  response(event) {
    this._response = {
      timestamp: Date.now(),
      origin: event.origin,
      data: event.data,
      event: event,
    };
  }
  error(error) {
    this._error[this._error.length] = {
      error: error,
      timestamp: Date.now(),
    };
  }
  print() {
    if (!__classPrivateFieldGet(this, _LtiPostMessageLog_debug, "f")) {
      return;
    }
    let reqTime = Date.now() - this._start_time;
    console.groupCollapsed(
      "%c %c request time: " +
        reqTime +
        (this._request.timestamp ? "ms\t " + this._request.data.subject : ""),
      "padding-left:" +
        Math.min(reqTime, 100) * 6 +
        "px; background-color: " +
        (this._error.length
          ? this._response.timestamp
            ? "orange"
            : "red"
          : "green") +
        ";",
      "padding-left:" +
        (10 + Math.max(0, 10 - reqTime) * 6) +
        "px; background-color: transparent"
    );
    if (this._request.timestamp) {
      console.groupCollapsed(
        "Request " +
          " - timestamp: " +
          this._request.timestamp +
          " - message_id: " +
          this._request.data.message_id +
          " - action: " +
          this._request.data.subject +
          " - origin: " +
          this._request.targetOrigin +
          (this._request.targetFrameName
            ? " - target: " + this._request.targetFrameName
            : "")
      );
      console.log("Sent from: " + window.location.href);
      console.log(JSON.stringify(this._request.data, null, "    "));
      console.groupEnd();
    }
    if (this._response.timestamp) {
      console.groupCollapsed(
        "Response" +
          " - timestamp: " +
          this._response.timestamp +
          " - message_id: " +
          this._response.data.message_id +
          " - action: " +
          this._response.data.subject +
          " - origin: " +
          this._response.origin
      );
      console.log(JSON.stringify(this._response.data, null, "    "));
      console.groupEnd();
    }
    if (this._error.length) {
      console.groupCollapsed(
        this._error.length + " Error" + (this._error.length > 1 ? "s" : "")
      );
      for (let i = 0; i < this._error.length; i++) {
        console.log(this._error[i].error.message || this._error[i].error);
      }
      console.groupEnd();
    }
    console.groupEnd();
  }
}
_LtiPostMessageLog_debug = new WeakMap();
