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
  var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      var r = (dt + Math.random()*16)%16 | 0;
      dt = Math.floor(dt/16);
      return (c=='x' ? r :(r&0x3|0x8)).toString(16);
  });
  return uuid;
}

// Generate a UUID for use in the logic
const uuid = create_UUID();
const platformOrigin = new URL(oidcAuthorizationUri).origin


//  -------------- APP LOGIC --------------- //

// Validation function before sending postMessage requests
const validateSendConditions = () => {
  if (state && nonce && target && platformOrigin) {
    return true
  }
  return false
}

const validateResponseConditions = () => {
  // flesh me out
  return true
}

// PostMessage request to store the state/nonce in the LMS
const postStateAndNonce = () => {
    if (validateSendConditions()){
        let parent = window.parent || window.opener;
        let targetFrame = target === "_parent" ? parent : parent.frames[target];
        targetFrame.postMessage(
          {
            subject: 'org.imsglobal.lti.put_data',
            key: 'state' + state,
            value: state,
            message_id: uuid
          },
          platformOrigin
        )
        targetFrame.postMessage(
          {
            subject: 'org.imsglobal.lti.put_data',
            key: 'nonce' + nonce,
            value: nonce,
            message_id: uuid
          },
          platformOrigin
        )
    }
}

// PostMessage request to get the state/nonce stored in the LMS
const getStateAndNonce = () => {
    if (validateSendConditions()){
        let parent = window.parent || window.opener;
        let targetFrame = target === "_parent" ? parent : parent.frames[target];
        targetFrame.postMessage(
          {
            subject: 'org.imsglobal.lti.get_data',
            key: 'nonce' + nonce,
            message_id: uuid
          },
          platformOrigin
        )
        targetFrame.postMessage(
          {
            subject: 'org.imsglobal.lti.get_data',
            key: 'state' + state,
            message_id: uuid
          },
          platformOrigin
        )
    }
}

// Handler for receiving PostMessages
const handlePostMessageResponse = (event) => {
  if (!validateResponseConditions()) return false;
  // This isn't a message we're expecting
  console.log("origin", event.origin)
  console.log("data", event.data)
  console.log("data.subject", event.data.subject)
  console.log("data.message_id", data.message_id)

  if (typeof event.data !== "object"){
      return;
  }

  // Validate it's the response type you expect
  if (event.data.subject !== "lti.put_data.response") {
      return;
  }

  // Validate the message id matches the id you sent
  if (event.data.message_id !== uuid) {
      // this is not the response you're looking for
      return;
  }

  // Validate that the event's origin is the same as the derived platform origin
  //if (event.origin !== platformOrigin) {
  //    return;
  //}

  // handle errors
  if (event.data.error){
      // handle errors
      console.log(event.data.error.code)
      console.log(event.data.error.message)
      return;
  }

  return;
  // It's the response we expected
  // The state and nonce values were successfully stored, redirect to Platform
  //redirect_to_platform(platformOIDCLoginURL);

}

//  -------------- EVENT LISTENERS --------------- //

// Listens for postMessages coming from the LMS
window.addEventListener('message', (event) => handlePostMessageResponse(event));

// listens for page load, then calls the postStateAndNonce function
window.addEventListener('load', () => postStateAndNonce());


