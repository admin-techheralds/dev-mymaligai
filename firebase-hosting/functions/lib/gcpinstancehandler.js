const uuid = require('uuid');
const path = require('path');
const os = require('os');
const fs = require('fs');
const Compute = require('@google-cloud/compute');
var request = require('request');

exports.startVMInstance = function(env,  cb) {
    //instance_id: 1822668918688305474
    //zone: us-central1-a
    //project: 

//     curl --request POST \
//   'https://compute.googleapis.com/compute/v1/projects/supplier-customer/zones/us-central1-a/instances/1822668918688305474/start?key=[YOUR_API_KEY]' \
//   --header 'Authorization: Bearer [YOUR_ACCESS_TOKEN]' \
//   --header 'Accept: application/json' \
//   --compressed
    console.log('Cred file:' + env['credFile']);
    console.log('Starting the instance');
    const compute = new Compute({
        projectId : env['project'],
        keyFilename : env['credFile']
    });
    const zone = compute.zone(env['zone']);
    const vm = zone.vm(env['vm_name']);

    vm.start(function(err, operation, apiResponse) {
        if(err) {
            console.log('Failed to start the VM instance. Error:' + err)
            return cb(err, undefined)
        }
        cb(undefined, "SUCCESS")
    });
}

exports.isVMUpAdRunning = function(env,  cb) {
    getVMInstance(env, function(err, details) {
        if(err) {
            console.log("Errr while chekcing whether VM is up and running or not.")
            cb(err, false)
            return
        }
        console.log('Current status is:' + details.metadata.status);
        if(details.metadata.status === "TERMINATED") {
            cb(undefined, false);
            return;
        }
        if(details.metadata.status === "RUNNING") {
            cb(undefined, true);
            return;
        }
        cb(undefined, false);
    })
}

getVMInstance = function(env, cb) {

    //curl \
//   'https://compute.googleapis.com/compute/v1/projects/supplier-customer/zones/us-central1-a/instances/1822668918688305474?key=[YOUR_API_KEY]' \
//   --header 'Authorization: Bearer [YOUR_ACCESS_TOKEN]' \
//   --header 'Accept: application/json' \
//   --compressed
  
    console.log('Getting the instance details')
    const compute = new Compute({
        projectId : env['project'],
        keyFilename : env['credFile']
    });
    const zone = compute.zone(env['zone']);
    const vm = zone.vm(env['vm_name']);
    vm.get(function(err, response) {
        if(err) {
            console.log('Failed to get the VM instance. Error:' + err)
            return cb(err, undefined)
        }
        //console.log("VM instance details:" + JSON.stringify(response));
        cb(undefined, response)
    });
}

getVMExternalIP = function(results) {
    if(results === undefined) {
        return undefined;
    }
    const networkInterfaces = results.metadata.networkInterfaces;
    console.log("Nework interfaces:" + JSON.stringify(networkInterfaces));
    if(networkInterfaces === undefined || networkInterfaces.length <= 0) {
      console.log('Network interfaces are found to be invalid from the VM instance data');
      return undefined;
    }
  
    const accessConfigs = networkInterfaces[0].accessConfigs;
    console.log("Access Configs:" + JSON.stringify(accessConfigs));
    if(accessConfigs === undefined || accessConfigs.length <= 0 ) {
      console.log('Failed to get the external network data(access configs)found to be invalid from the VM instance data');
      return undefined;
    }
    return accessConfigs[0].natIP;
}

exports.waitForVMInstance = function(env, cb) {

    //curl \
//   'https://compute.googleapis.com/compute/v1/projects/supplier-customer/zones/us-central1-a/instances/1822668918688305474?key=[YOUR_API_KEY]' \
//   --header 'Authorization: Bearer [YOUR_ACCESS_TOKEN]' \
//   --header 'Accept: application/json' \
//   --compressed
  
    waitForVMNetwork(env, function(err, ip) {
        if(err) {
            console.log("Error while waiting for the VM external interface to be up and running");
            cb(new Error('Error while waiting for the VM external interface to be up and running'), undefined);
            return;
        }
        cb(undefined, ip);
    })

}


waitForVMNetwork = function(env, cb) {

    //curl \
//   'https://compute.googleapis.com/compute/v1/projects/supplier-customer/zones/us-central1-a/instances/1822668918688305474?key=[YOUR_API_KEY]' \
//   --header 'Authorization: Bearer [YOUR_ACCESS_TOKEN]' \
//   --header 'Accept: application/json' \
//   --compressed
  
    console.log('Awaiting for the instance details with network setup')
    getVMInstance(env, function(err, response) {
        if(err) {
            console.log("Error while waiting for the VM external interface to be up and running");
            cb(new Error('Error while waiting for the VM external interface to be up and running'), undefined);
            return;
        }
        var ip = getVMExternalIP(response);
        if(ip === undefined) {
            console.log("NAT IP is not found. Waiting for 1 second and check it again..");
            setTimeout(function() {
                waitForVMNetwork(env, cb);
            }, 1000)
        } else {
            return cb(undefined, ip);
        }
    })
}

exports.triggerSupplierBuildRequest = function(url, payload, cb) {
    console.log("Triggering the build request to:" + url + " with payload:" + JSON.stringify(payload));
    var options = {
        uri: url,
        method: 'POST',
        formData: payload,
        timeout : 5000
      }

      request(options, function (err, response, body) {
        
        if(err) {
          console.log('Error occurred while sending the trigger to build the supplier. Error:' + JSON.stringify(err));
          return cb(err, undefined);
        }
        cb(undefined, "SUCCESS");
    });
}

exports.waitForBuildToComplete = function(url, payload, cb) {

    sendBuildRequest(url, payload, function(err, results) {
        if(err) {
            console.log("Failed in sending the build to get triggered. Error:" + err);
            cb(err, undefined);
            return;
        }

        isBuildComplete(url, payload, function(err, status) {
            if(err) {
                console.log('Failed while waiting for the build to get it completed')
                cb(err, undefined);
                return;
            }
            console.log("Everything is okay now");
            cb(undefined, status);
        });
    }); 
}

isBuildComplete = function(url, payload, cb) {
    console.log("Sending the build request to:" + url + " with payload:" + JSON.stringify(payload));
    var options = {
        'method': 'POST',
        'url': url,
        'headers': {
        },
        formData: payload,
        timeout : 5000
      }

      request(options, function (err, response, body) {        
        if(err) {
          console.log('Error occurred while sending the trigger to build the supplier. Error:' + JSON.stringify(err));
          if(err.code  === "ETIMEDOUT") {
            console.log('Timeout error. Try after 5 seconds')
            setTimeout(function() {
                isBuildComplete(url, payload, cb)
            }, 5000)
            return;
          }
        }
        console.log("Build status:" + JSON.stringify(body));
        if(body.status === "COMPLETED") {
            console.log("Build is complete.")
            cb(undefined, "SUCCESS");
            return;
        }
        console.log("Build is in-progress.")
        setTimeout(function() {
            isBuildComplete(url, payload, cb)
        }, 5000)
        return;

    });
}


sendBuildRequest = function(url, payload, cb) {
    console.log("Sending the build request to:" + url + " with payload:" + JSON.stringify(payload));
    var options = {
        uri: url,
        method: 'POST',
        formData: payload,
        timeout : 5000
      }

      request(options, function (err, response, body) {
        
        if(err) {
          console.log('Error occurred while sending the trigger to build the supplier. Error:' + JSON.stringify(err));
          if(err.code  === "ETIMEDOUT") {
            console.log('Timeout error. Try after 5 seconds')
            setTimeout(function() {
                sendBuildRequest(url, payload, cb)
            }, 5000)
            return;
          }
          return cb(err, undefined);
        }
        cb(undefined, body);
    });
}



exports.stopVMInstance = function(env, cb) {
    
//     curl --request POST \
//   'https://compute.googleapis.com/compute/v1/projects/supplier-customer/zones/us-central1-a/instances/1822668918688305474/stop?key=[YOUR_API_KEY]' \
//   --header 'Authorization: Bearer [YOUR_ACCESS_TOKEN]' \
//   --header 'Accept: application/json' \
//   --compressed


    console.log('Stopping the instance');
    const compute = new Compute({
        projectId : env['project'],
        keyFilename : env['credFile']
    });
    const zone = compute.zone(env['zone']);
    const vm = zone.vm(env['vm_name']);
    vm.stop(function(err, operation, apiResponse) {
        if(err) {
            console.log('Failed to stop the VM instance. Error:' + err)
            return cb(err, undefined)
        }
        cb(undefined, "SUCCESS")
    });
}

