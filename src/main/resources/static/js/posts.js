var uuid = create_UUID();



function postStateAndNonce(state, nonce, target){

    if (target !=null && target !=""){
        let parent = window.parent || window.opener;
        let targetFrame = target === "_parent" ? parent : parent.frames[target];
        targetFrame.postMessage(
          {
            subject: 'org.ims_global.lti.put_data',
            key: 'state' + state,
            value: state,
            message_id: uuid
          },
          '*'
        )
        targetFrame.postMessage(
          {
            subject: 'org.ims_global.lti.put_data',
            key: 'nonce' + nonce,
            value: nonce,
            message_id: uuid
          },
          '*'
        )
    }
}

function getStateAndNonce(state, nonce, target){

    if (target !=null && target !=""){
        let parent = window.parent || window.opener;
        let targetFrame = target === "_parent" ? parent : parent.frames[target];
        targetFrame.postMessage(
          {
            subject: 'org.ims_global.lti.get_data',
            key: 'state' + state
          },
          '*'
        )

    }
}

function getStateAndNonce(state, nonce, target){

    if (target !=null && target !=""){
        let parent = window.parent || window.opener;
        let targetFrame = target === "_parent" ? parent : parent.frames[target];
        targetFrame.postMessage(
          {
            subject: 'org.ims_global.lti.get_data',
            key: 'nonce' + nonce
          },
          '*'
        )
    }
}

window.addEventListener('message', function (event) {
    // This isn't a message we're expecting
    console.log("origin" + event.origin)
    console.log("data" + event.data)
    console.log("data.subject" + event.data.subject)
    console.log("data.message_id" + data.message_id)

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
});

function create_UUID(){
    var dt = new Date().getTime();
    var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = (dt + Math.random()*16)%16 | 0;
        dt = Math.floor(dt/16);
        return (c=='x' ? r :(r&0x3|0x8)).toString(16);
    });
    return uuid;
}