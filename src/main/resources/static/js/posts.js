// TODO:
// 1. Message ID we sent needs to match response
// 2. Origin of the response should match the oidc auth URI origin
// 3. verify `data` response is an object
// 4. verify data.subject = lti.put_data.response

// https://www.imsglobal.org/spec/lti-cs-oidc/v0p1#js-example-listen-for-put_data-response

//  -------------- UTILITY FUNCTIONS --------------- //

// Simple UUID generator
const create_UUID = () => {
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

//  -------------- CONSTANTS AND PLACEHOLDERS --------------- //

// Defined here so we only have to change them in one place
const PUT_SUBJECT = "org.imsglobal.lti.put_data";
const GET_SUBJECT = "org.imsglobal.lti.get_data";

// Generate a UUID for use in sending and verifying responses
const uuid = create_UUID();

// Flag to auto-redirect
const autoRedirect = true;
// redirectUrl if we want to use it
let redirectUrl = null;

// placeholder for plaftormOrigin
let platformOrigin = null;

// placeholder for mesage type sent
let messageType = null;

// placeholder for expected state and nonce
let expected_state = null;
let expected_nonce = null;

// placeholder for successful 'get' responses
let stateResponseData = null;
let nonceResponseData = null;

//  -------------- APP LOGIC --------------- //

// Call from the template passing the url for the next step
// set autoRedirect=true above if we want to proceed to the next step after responses and validation
const setAutoRedirectUrl = (oidcEndpointComplete) => {
  redirectUrl = oidcEndpointComplete;
};

// Validation function before sending postMessage requests
const validateSendConditions = (state, nonce, target) => {
  if (state && nonce && target && platformOrigin) {
    console.log("✅ Passed send conditions");
    return true;
  }
  return false;
};

const validateResponseConditions = (origin, data) => {
  // Conditions in the spec
  // https://www.imsglobal.org/spec/lti-cs-oidc/v0p1#js-example-listen-for-put_data-response

  // should this be a get or put response subject?
  const subjectResponse =
    messageType === "get"
      ? `${GET_SUBJECT}.response`
      : `${PUT_SUBJECT}.response`;

  if (
    // verify `data` is an object
    typeof data === "object" &&
    // Message ID we sent needs to match response
    data.message_id === uuid &&
    // Origin of the response should match the oidc auth URI origin
    origin === platformOrigin &&
    // verify data.subject = lti.put/get_data.response
    data.subject === subjectResponse
  ) {
    console.log(
      `✅ ${
        data.key.includes("state") ? "State" : "Nonce"
      } passed ${subjectResponse} response conditions`
    );
    return true;
  }
  return false;
};

// PostMessage request to store the state/nonce in the LMS
const putStateAndNonce = (state, nonce, target, oidcAuthorizationUri) => {
  // set the platformOrigin we're sending for validation before and after the request
  platformOrigin = new URL(oidcAuthorizationUri).origin;

  // validate the data we're sending is not null/undefined
  if (validateSendConditions(state, nonce, target)) {
    // set the message type we're sending for comparisson once we get a response
    messageType = "put";

    // get the target frame/window to send the postmessage to
    const parent = window.parent || window.opener;
    const targetFrame = target === "_parent" ? parent : parent.frames[target];

    console.log(`↖️ Sending ${messageType} postMessages`);
    // send postMessage to set the state
    targetFrame.postMessage(
      {
        subject: PUT_SUBJECT,
        key: "state" + state,
        value: state,
        message_id: uuid,
      },
      platformOrigin
    );

    // send postMessage to set the nonce
    targetFrame.postMessage(
      {
        subject: PUT_SUBJECT,
        key: "nonce" + nonce,
        value: nonce,
        message_id: uuid,
      },
      platformOrigin
    );
  }
};

// PostMessage request to get the state/nonce stored in the LMS
const getStateAndNonce = (
  expectedState,
  expectedNonce,
  target,
  oidcAuthorizationUri
) => {
  // set the platformOrigin we're sending for comparisson once we get a response
  platformOrigin = new URL(oidcAuthorizationUri).origin;

  if (validateSendConditions(expectedState, expectedNonce, target)) {
    // stare expected state and nonce for the response
    expected_state = expectedState;
    expected_nonce = expectedNonce;

    // set the message type we're sending for comparisson once we get a response
    messageType = "get";

    // get the target frame/window to send the postmessage to
    let parent = window.parent || window.opener;
    let targetFrame = target === "_parent" ? parent : parent.frames[target];

    console.log(`↖️ Sending ${messageType} postMessages`);
    // send postMessage to get the state
    targetFrame.postMessage(
      {
        subject: GET_SUBJECT,
        key: "nonce" + expectedNonce,
        message_id: uuid,
      },
      platformOrigin
    );
    // send postMessage to get the state
    targetFrame.postMessage(
      {
        subject: GET_SUBJECT,
        key: "state" + expectedState,
        message_id: uuid,
      },
      platformOrigin
    );
  }
};

const handlePutResponse = () => {
  console.log(`🤘🏽 ${PUT_SUBJECT} Complete! Proceed to the next step.`);
  console.log("State log 🪵", stateResponseData);
  console.log("Nonce log 🪵", nonceResponseData);
  // Anything else to do here? How about auto-forwarding:
  if (autoRedirect) {
    console.log(`====== ↪️ AUTO-REDIRECTING TO THE NEXT STEP =======`);
    // redirects without adding the the browser history (back button)
    window.location.replace(redirectUrl);
    // redirects while adding the the browser history (back button)
    // window.location.href = redirectUrl;
  }
};

const handleGetResponse = () => {
  console.log(
    `🤘🏽 ${GET_SUBJECT} Complete! Now working on populating the form for submission...`
  );

  console.log("State log 🪵", stateResponseData);
  console.log("Nonce log 🪵", nonceResponseData);

  document.getElementById("pmState").value = stateResponseData.value;
  document.getElementById("pmNonce").value = nonceResponseData.value;
  document.getElementById("pmExpected_state").value = expected_state;
  document.getElementById("pmExpected_nonce").value = expected_nonce;
  document.getElementById("pmId_token").value = uuid;
  // document.getElementById("pmToken").value = data.value;
  // document.getElementById("pmLink").value = data.value;
  // pmState
  // pmNonce
  // pmExpected_state
  // pmExpected_nonce
  // pmId_token
  // pmToken
  // pmLink
};

// Handler for receiving PostMessages
const handlePostMessageResponse = ({ origin, data }) => {
  // handle errors first and formost
  if (data.error) {
    // handle errors
    console.log("❌ PostMessage response error 😵");
    console.log(data.error);
    return;
  }

  // validate all the things we need to about the response data
  if (!validateResponseConditions(origin, data)) {
    console.log("❌ PostMessage response did not meet validation criteria 😵");
    return;
  }

  // if state data, set the stateResponseData with that data
  if (data.key.includes("state")) {
    stateResponseData = data;
  }

  // if nonce data, set the nonceResponseData with that data
  if (data.key.includes("nonce")) {
    nonceResponseData = data;
  }

  // ensure we have response data for both state and nonce
  if (stateResponseData && nonceResponseData) {
    // should only hit this if both nonceResponseData and stateResponseData are set
    // so that we have all the data collected we need and don't call the functions below more than once.
    if (messageType === "get") {
      // hits on step 2
      handleGetResponse();
    } else {
      // hits on step 1
      handlePutResponse();
    }
  }

  // This isn't a message we're expecting
  // console.log("origin", origin)
  // console.log("data", data)
  // console.log("data.subject", data.subject)
  // console.log("data.message_id", data.message_id)

  // if (data.subject.includes("get") && data.key.includes("state")) {
  //   document.getElementById("pmState").value = data.value;
  // }

  // // Validate it's the response type you expect
  // if (data.subject !== "lti.put_data.response") {
  //   return;
  // }

  // // Validate the message id matches the id you sent
  // if (data.message_id !== uuid) {
  //   // this is not the response you're looking for
  //   return;
  // }

  // Validate that the event's origin is the same as the derived platform origin
  //if (event.origin !== platformOrigin) {
  //    return;
  //}

  return;
  // It's the response we expected
  // The state and nonce values were successfully stored, redirect to Platform
  //redirect_to_platform(platformOIDCLoginURL);
};

//  -------------- EVENT LISTENERS --------------- //

// Listens for postMessages coming from the LMS
window.addEventListener("message", (event) => handlePostMessageResponse(event));

// listens for page load, then calls the postStateAndNonce function
// window.addEventListener('load', () => postStateAndNonce());
