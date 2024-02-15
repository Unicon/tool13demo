/**
 * A class for handling LTI post messages.
 */
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class LTIPostMessage {
  /**
   * Constructs an instance of LTIPostMessage.
   */
  constructor(target) {
    /**
     * The message ID.
     * @type {string|null}
     */
    this.mId = null;

    /**
     * The subject of the message.
     * @type {string|null}
     */
    this.subject = null;

    /**
     * The error message.
     * @type {string|null}
     */
    this.error = null;

    /**
     * The response message.
     * @type {object|null}
     */
    this.response = null;

    this.target = target;

    /**
     * Binds the handlePostMessages method to the current instance.
     */
    this.handlePostMessages = this.handlePostMessages.bind(this);

    // Setup the event listener
    window.addEventListener('message', this.handlePostMessages);
  }

  async create_UUID() {
    var dt = new Date().getTime();
    var uuid = "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(
      /[xy]/g,
      function (c) {
        var r = (dt + Math.random() * 16) % 16 | 0;
        dt = Math.floor(dt / 16);
        return (c == "x" ? r : (r & 0x3) | 0x8).toString(16);
      }
    );
    return uuid;
  };

  /**
   * Handles post messages received from the window.
   * @param {Event} event - The message event.
   */
  handlePostMessages(event) {
    /**
     * The subject response.
     * @type {string}
     */
    const subjectResponse = `${this.subject}.response`;

    if (
      event.data.subject === subjectResponse &&
      event.data.message_id === this.mId
    ) {
      if (event.data.error) {
        this.error = event.data.error;
      } else {
        this.response = event.data;
      }
    }
  }

  /**
   * Sends a message using postMessage.
   * @param {object} message - The message to be sent.
   * @param {string} [origin='*'] - The target origin.
   * @returns {Promise<object>} - A promise that resolves with the response or rejects with an error.
   */
  async sendMessage(message, platformOrigin = '*') {
    // save the message id and subject for validating the response
    if (platformOrigin !== '*'){
        platformOrigin = new URL(platformOrigin).origin;
    }
    this.mId = message.message_id;
    this.subject = message.subject;

    // send the postMessage
    if (this.target !== '_parent'){
        window.parent.frames[this.target].postMessage(
          message,
          platformOrigin
        );
    }else{
        window.parent.postMessage(
          message,
          platformOrigin
        );
    }
    // wait for the validation response to return the data
    return await this.checkErrorAndResponseChanges();
  }

  /**
   * Checks for error and response changes.
   * @returns {Promise<object>} - A promise that resolves with the response or rejects with an error.
   */
  async checkErrorAndResponseChanges() {
    return new Promise((resolve, reject) => {
      const topTimeout = 5000; // the max time we'll wait for LMS to respond
      const intervalTime = 1000; // frequence to check for value changes
      let timeout = 0; // timeout placeholder
      const interval = setInterval(() => {
        // check if we've reached the topTimeout
        if (timeout >= topTimeout) {
          // if so, clear the interval and reject with an error
          clearInterval(interval);
          reject({ error: 'Timeout reached, no response from LMS' });
        }

        // check if we have an error
        if (this.error !== null) {
          // if so, clear the interval and reject with the error
          clearInterval(interval);
          reject({ error: this.error });
        }

        // check if we have a response
        if (this.response !== null) {
          // if so, clear the interval and resolve with the response
          clearInterval(interval);
          resolve(this.response);
        }

        // increment the timeout
        timeout += intervalTime;
      }, intervalTime);
    });
  }

  /**
   * Removes the event listener for post messages.
   */
  destroy() {
    window.removeEventListener('message', this.handlePostMessages);
  }
}