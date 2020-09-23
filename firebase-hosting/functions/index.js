
// The Cloud Functions for Firebase SDK to create Cloud Functions and setup triggers.
const functions = require('firebase-functions');
const uuid = require('uuid');
const fs = require('fs');
const path = require('path');
var base64 = require('base-64');
var request = require('request');
const os = require('os'); 


// The Firebase Admin SDK to access Cloud Firestore.
const admin = require('firebase-admin');
var env =  "supplier-customer";
if(process.env.GCLOUD_PROJECT !== undefined) {
  env =  process.env.GCLOUD_PROJECT;
}
const { parseRequestAndDownloadFiles } = require('./lib/downloadFiles.js');
const { processSupplierInventoryFiles } = require('./lib/processInventoryFile.js');
const { startVMInstance, waitForVMInstance, stopVMInstance, waitForBuildToComplete, isVMUpAdRunning, triggerSupplierBuildRequest  } = require('./lib/gcpinstancehandler.js');

const development = (env === "supplier-customer")
const credFile = env + "-service-account.json"
const database = "https://" + env + ".firebaseio.com/"; 
const bucket_name = env + ".appspot.com";

// process.env.GOOGLE_APPLICATION_CREDENTIALS=JSON.parse(base64.decode(fs.readFileSync(credFile)))
fs.writeFileSync(os.tmpdir() + path.sep + env + "-service-account-decoded.json", base64.decode(fs.readFileSync(credFile)))

admin.initializeApp({
  credential: admin.credential.cert(JSON.parse(base64.decode(fs.readFileSync(credFile)))),
  databaseURL: database,
  storageBucket : bucket_name
});
const auth = admin.auth();

const build_env = require('./build_env.json');
credFile['credFile'] = env + "-service-account-decoded.json"
process.env.GOOGLE_APPLICATION_CREDENTIALS=os.tmpdir() + path.sep + env + "-service-account-decoded.json";

const express = require('express');
const { networkInterfaces } = require('os');
// const fileUpload = require('express-fileupload');
const cors = require('cors')({origin: true});

const app = express();
// Automatically allow cross-origin requests
app.use(cors);

var BUILD_TRIGGER_URI="http://<EXTERNAL_IP>/triggerSupplierAppBuild"

findSupplierByName = function (name, cb) {
  console.log('Finding the Supplier by name:' + name);
  var db = admin.database().ref('suppliers');
  db.orderByKey().once('value', function (snapshot) {
    //console.log('Snapshot is:' + JSON.stringify(snapshot));
    supplier_found = false;
    snapshot.forEach(function (data) {
      var supplier_key = data.key;
      var snapshot_value = data.val();
      if (name === snapshot_value['name']) {
        console.log("Found the supplier name and its key is:" + supplier_key);
        supplier_found = true;
        return cb(undefined, supplier_key);
      }
    });
    if (supplier_found === false) {
      cb(new Error('Supplier with name:' + name + ' is not found'), undefined);
      return;
    }
  });
}

updateSupplierInventoryCategories = function (supplier_id, categories, cb) {
  console.log('Updating the categories under Suppliers tag for the new categories for supplier:' + supplier_id + " with categories:" + JSON.stringify(categories));
  var tags = admin.database().ref('suppliers/' + supplier_id + '/tags');
  var categories_to_be_added = [];
  var existing_categories = []
  var db_categories = {}
  tags.once('value', function (snapshot) {
    snapshot.forEach(function (data) {
      console.log('Tag Key:' + data.key + ", value is:" + data.val());
      existing_categories.push(data.val());
      db_categories[data.key] = data.val()
    });
    console.log('Original data tags is:' + JSON.stringify(db_categories))

    categories.forEach(function (category) {
      if (existing_categories.indexOf(category) === -1) {
        categories_to_be_added.push(category)
      }
    })
    console.log('List of new categories to be added is:' + JSON.stringify(categories_to_be_added));
    if (categories_to_be_added.length <= 0) {
      console.log('New categories are not present in the inventory list to be added to the supplier');
      cb(undefined, db_categories);
      return;
    }
    categories_to_be_added.forEach(function (newcategory) {
      var id = uuid.v4();
      db_categories[id] = newcategory
    });
    console.log('data to be added in tags is:' + JSON.stringify(db_categories))
    //cb(undefined, "SUCCESS");
    tags.set(db_categories).then(function () {
      console.log('Successfully added the categories..');
      cb(undefined, db_categories);
      return;
    }).catch(function (err) {
      console.log('Failed to add the categories:' + JSON.stringify(categories_to_be_added) + " with error:" + err)
      cb(new Error('Failed to add the categories:' + JSON.stringify(categories_to_be_added)), "FAIL");
      return;
    })
  });
}

getCategoryIDByName = function (name, db_categories) {
  var ids = Object.keys(db_categories);
  var category_id = undefined;
  for (i = 0; i < ids.length; i++) {
    id = ids[i]
    var categoryName = db_categories[id];
    if (categoryName === name) {
      category_id = id;
      break;
    }
  }
  return category_id;
}

isFileExistsIndexPage = function (filepath) {
  console.log('Checking for file availability:' + filepath)
  flag = true;
  try {
    fs.accessSync(filepath, fs.constants.F_OK);
  } catch (e) {
    console.log('Given file is not accessible. error:' + e)
    flag = false;
  }
  return flag;
}

uploadSingleInventoryImage = function(supplier_id, bulk_import_id, inventories, current_item_index, statusFlag, errorCount, cb) {

  console.log('For supplier:' + supplier_id + ', Uploading the single inventory item image file at index:' + current_item_index + "/" + inventories.length)
  var bucket = admin.storage().bucket();
  item = inventories[current_item_index]
  var item_name = item['itemname']
  var item_image_file = item['itemimagefile'];
  if (item_image_file.length > 0) {
    console.log('Uploading Item image file:' + item_image_file);
    if (isFileExistsIndexPage(item_image_file)) {
      bucket.upload(item_image_file, {
        gzip: true,
        public: true,
        destination: 'images/' + supplier_id + '/' + item['itemimagefilename'],
        resumable: false,
        metadata: {
          cacheControl: 'public, max-age=31536000',
        },
      }
      ).then(function (uploadedFileDetails) {
        var link = uploadedFileDetails[1].mediaLink;
        console.log('Link to download is:' + link);
        item['itemimageurl'] = link;
        console.log('Successfully uploaded the item image file');
        statusFlag = true
        return uploadInventoryImages(supplier_id, bulk_import_id, inventories, current_item_index, statusFlag, errorCount, cb)
      }).catch(function (err) {
        console.log('Failed to upload the item image file. Error:' + err)
        statusFlag = false
        errorCount++;
        return uploadInventoryImages(supplier_id, bulk_import_id, inventories, current_item_index, statusFlag, errorCount, cb)
      })
    } else {
      //file doesnt exists in the file system
      console.log('Failed to upload the item image file as the given file:' +  item_image_file + ' doesnot exist')
      statusFlag = false
      errorCount++;
      return uploadInventoryImages(supplier_id, bulk_import_id, inventories, current_item_index, statusFlag, errorCount, cb)
    }
  } else {
    //file name lengh is empty
    console.log('Failed to upload the item image file as the given file is invalid')
    statusFlag = false
    errorCount++;
    return uploadInventoryImages(supplier_id, bulk_import_id, inventories, current_item_index, statusFlag, errorCount, cb)
  }
}

uploadInventoryImages = function (supplier_id, bulk_import_id, inventories, current_item_index, statusFlag, errorCount, cb) {

  if(current_item_index >= inventories.length - 1) {
    console.log('All the items might have been uploaded.')
    if(errorCount > 0) {
      console.log('Some items might have been failed to upload.')
      return cb(new Error('Some of the item images are failed to upload'), "FAIL")
    } else  {
      console.log('All items have been uploaded successfully')
      return cb(undefined, "SUCCESS")
    }
  }
  setTimeout(function() {
    current_item_index++;
    uploadSingleInventoryImage(supplier_id, bulk_import_id, inventories, current_item_index, statusFlag, errorCount, cb)
  }, 200)
}

getMaxComponentForSKU = function (value) {
  value = value + ""
  if (value.length >= 3) {
    return value.substr(0, 3)
  } else {
    while (value.length < 3) {
      value = "0" + value
    }
  }
  return value;
}

getsku = function (supplier_id, item) {
  return "sku" +
    "-" + getMaxComponentForSKU(supplier_id) +
    "-" + getMaxComponentForSKU(item['categoryname']) +
    "-" + getMaxComponentForSKU(item['itemname']) +
    "-" + getMaxComponentForSKU(item['attr1']) +
    "-" + getMaxComponentForSKU(item['attr2']) +
    "-" + getMaxComponentForSKU(item['attr3'])
}

updateInventoryItems = function (supplier_id, bulk_import_id, inventories, db_categories, cb) {
  console.log("Updating the Inventory Items...");
  var inventory = admin.database().ref('inventory');
  var update_data = {};
  inventories.forEach(function (item) {
    item['sku'] = getsku(supplier_id, item)
    console.log('Updating the item sku:' + item['sku'])
    var category_id = getCategoryIDByName(item['categoryname'], db_categories);

    update_data[supplier_id + '/' + category_id + '/' + item['sku'] + '/sku'] = item['sku'];
    if (item['itemimageurl'] !== undefined) {
      update_data[supplier_id + '/' + category_id + '/' + item['sku'] + '/img'] = item['itemimageurl'];
    }
    update_data[supplier_id + '/' + category_id + '/' + item['sku'] + '/name'] = item['itemname'];
    if (item['itemprice'] !== undefined) {
      update_data[supplier_id + '/' + category_id + '/' + item['sku'] + '/price'] = parseFloat(item['itemprice']);
    }
    if (item['itemquantitytype'] !== undefined) {
      update_data[supplier_id + '/' + category_id + '/' + item['sku'] + '/quantity_type'] = parseInt(item['itemquantitytype']);
    }
    update_data[supplier_id + '/' + category_id + '/' + item['sku'] + '/active'] = 1;
    update_data[supplier_id + '/' + category_id + '/' + item['sku'] + '/in_stock'] = 1;
    update_data[supplier_id + '/' + category_id + '/' + item['sku'] + '/attr1'] = item['attr1'];
    update_data[supplier_id + '/' + category_id + '/' + item['sku'] + '/attr2'] = item['attr2'];
    update_data[supplier_id + '/' + category_id + '/' + item['sku'] + '/attr3'] = item['attr3'];
    update_data[supplier_id + '/' + category_id + '/' + item['sku'] + '/created_date'] = new Date().getTime();
    update_data[supplier_id + '/' + category_id + '/' + item['sku'] + '/created_mode'] = 'BULK-IMPORT';
    update_data[supplier_id + '/' + category_id + '/' + item['sku'] + '/bulk_import_id'] = bulk_import_id;
  })

  // console.log('Updating the inventories with data as:' + JSON.stringify(update_data));

  inventory.update(update_data).then(function () {
    console.log("Successfully updated the inventories")
    return cb(undefined, "SUCCESS");
  }).catch(function (err) {
    console.log("Failed to update the inventories. Error:" + err)
    return cb(err, "FAIL")
  })
}

updateSupplierInventories = function (supplier_id, bulk_import_id, inventories, db_categories, cb) {
  console.log('Updating the supplier inventories for the id:' + supplier_id);

  //upload the images first for all the inventories
  //supplier_id, bulk_import_id, inventories, current_item_index, statusFlag, errorCount
  errorCount = 0;
  uploadInventoryImages(supplier_id, bulk_import_id, inventories,  -1, false, errorCount, function (err, result) {
    if (err) {
      console.log("Failed to upload the items images. Error:" + err);
      return cb(err, "FAIL")
    }
    updateInventoryItems(supplier_id, bulk_import_id, inventories, db_categories, function (err, results) {
      if (err) {
        console.log("Failed while updating the inventories. Error:" + err);
        return cb(err, "FAIL")
      }
      return cb(undefined, results);
    });
  })
}

saveUploadedSupplierInventoryFile = function (supplier_id, bulk_import_id, file, cb) {
  var bucket = admin.storage().bucket();
  console.log('Uploading supplier inventory file:' + file);
  var filename = path.basename(file);
  if (isFileExistsIndexPage(file)) {
    bucket.upload(file, {
      gzip: true,
      public: true,
      destination: 'supplier_uploaded_inventory_files/' + supplier_id + '/' + bulk_import_id + '/' + filename,
      resumable: false,
      metadata: {
        cacheControl: 'public, max-age=31536000',
        bulk_import_id: bulk_import_id
      },
    }
    ).then(function (uploadedFileDetails) {
      var link = uploadedFileDetails[1].mediaLink;
      console.log('Link to download is:' + link);
      return cb(undefined, link)
    }).catch(function (err) {
      console.log('Failed to uploaded the supplier inventory zip file. Error:' + err)
      return cb(new Error("Failed to upload supplier inventory zip file"), "FAIL")
    })
  }
}

storeUploadedSupplierInventoryFileIntoDatabase = function (supplier_id, bulk_import_id, link, cb) {
  var supplier = admin.database().ref();
  var update_data = {};
  update_data['bulkimport/' + supplier_id + '/' + bulk_import_id + '/uploaded_inventory_file'] = link;
  update_data['bulkimport/' + supplier_id + '/' + bulk_import_id + '/imported_on'] = new Date().getTime();
  update_data['bulkimport/' + supplier_id + '/' + bulk_import_id + '/status'] = "INPROGRESS";

  supplier.update(update_data).then(function () {
    console.log("Successfully updated the inventory zip file link")
    return cb(undefined, "SUCCESS");
  }).catch(function (err) {
    console.log("Failed to update the inventory file link. Error:" + err)
    return cb(err, "FAIL")
  })
}

updateUploadStatusSupplierInventoryFileIntoDatabase = function (supplier_id, bulk_import_id, err, res) {
  var supplier = admin.database().ref();
  var update_data = {};
  if (err !== undefined) {
    update_data['bulkimport/' + supplier_id + '/' + bulk_import_id + '/status'] = "FAILED";
    update_data['bulkimport/' + supplier_id + '/' + bulk_import_id + '/error_details'] = err;
  } else {
    update_data['bulkimport/' + supplier_id + '/' + bulk_import_id + '/status'] = "SUCCESS";
  }
  update_data['bulkimport/' + supplier_id + '/' + bulk_import_id + '/result_updated_on'] = new Date().getTime();

  supplier.update(update_data).then(function () {
    console.log("Successfully updated the bulk import status in DB for the bulk import id:" + bulk_import_id);
    return;
  }).catch(function (ex) {
    console.log("Error while updating the status for the bulk import id:" + bulk_import_id + ", Err:" + ex);
    return;
  })
}

updateBuildResultsInToDatabase = function (supplier_id, build_id, build_results, cb) {
  var supplier = admin.database().ref();
  var update_data = {};

  var keys = Object.keys(build_results);
  keys.forEach(function(k) {
    update_data['suppliers/' + supplier_id + '/build_details/' + build_id + '/' + k] =  build_results[k];
  });

  update_data['suppliers/' + supplier_id + '/build_details/' + 'current_build_id'] =  build_id
  if(build_results['status'] !== undefined) {
    update_data['suppliers/' + supplier_id + '/build_details/' + 'status'] =  build_results['status']
  }
  if(build_results['apk_file'] !== undefined) {
    update_data['suppliers/' + supplier_id + '/build_details/' + 'apk_file'] =  build_results['apk_file']
  }

  supplier.update(update_data).then(function () {
    console.log("Successfully updated the build status");
    return cb(undefined, "SUCCESS");
  }).catch(function (err) {
    console.log("Failed to update the build status. Error:" + err)
    return cb(err, "FAIL")
  });
}

getSupplierAPKFileFromFolder = function(supplier_name, folder, cb) {
  console.log("Searching for supplier:" + supplier_name + " apk file in folder:" + folder);
  console.log("Updating spaces if it is present in the file name");
  supplier_name = supplier_name.replace(" ", "_");
  console.log("Updating supplier name to look for apk file is:" + supplier_name);
  
  fs.readdir(folder, function (err, files) {
    if (err) {
      console.log("Error occurred while listing/reading the folder content from folder:" + folder + ", Error:" + err);
      return cb(err, "FAIL");
    } 
    var found = false;
    var supplier_apk_file = undefined;
    files.forEach(function (file) {
      if(file.toLocaleLowerCase().startsWith(supplier_name.toLocaleLowerCase())
        && file.toLocaleLowerCase().endsWith(".apk")) {
          found = true;
          supplier_apk_file = file;
          return true;
      }
    });
    if(found) {
      console.log("Found the supplier apk file:" + supplier_apk_file);
      return cb(undefined, supplier_apk_file);
    }
    console.log("Failed to find the supplier apk files for the supplier:" + supplier_name + " in folder:" + folder);
    return cb(new Error("Failed to find the supplier apk file"), undefined);
  });
}

getFileNameAlone = function(file) {
  var index = file.lastIndexOf('\\');
  if(index === -1) {
      index = file.lastIndexOf('/');
  }
  if(index === -1) {
      return file;
  }
  return file.substring(index + 1);
}

uploadSupplierBuildAPKLogFiles = function (supplier_id, build_id, files, cb) {
  var bucket = admin.storage().bucket();
  count = 0;
  errFilesList = []
  files.forEach(function (item) {
    var file = item['file'];
    console.log('Uploading build file:' + file);
    if (isFileExistsIndexPage(file)) {
      bucket.upload(file, {
        gzip: true,
        public: true,
        destination: 'apks/' + supplier_id + '/' + build_id + '/' + getFileNameAlone(file),
        metadata: {
          cacheControl: 'public, max-age=31536000',
        },
      }
      ).then(function (uploadedFileDetails) {
        var link = uploadedFileDetails[1].mediaLink;
        console.log('Link to download is:' + link);
        item['file_url'] = link;
        count = count + 1
        console.log('Successfully uploaded the file');
        if (count >= files.length) {
          if (errFilesList.length > 0) {
            cb(new Error("Failed to upload files for " + errFilesList.length + " items"), "FAIL")
            return;
          }
          return cb(undefined, "SUCCESS")
        }
        return;
      }).catch(function (err) {
        console.log('Failed to upload the file. Error:' + err)
        count = count + 1
        errFilesList.push(item)
        if (count >= files.length) {
          return cb(new Error("Failed to upload files for " + errFilesList.length + " items"), "FAIL")
        }
      })
    } else {
      console.log('Item file:' + file + ' does NOT exist');
      count = count + 1
      if (count >= files.length) {
        if (errFilesList.length > 0) {
          return cb(new Error("Failed to upload files for " + errFilesList.length + " items"), "FAIL")
        }
        return cb(undefined, "SUCCESS");
      }
    }
  });
}

getFileURLById = function(id, files) {
  var url = undefined;
  files.forEach(function(item) {
    if(item['id'] === id) {
      url = item['file_url'];
      return true;
    }
  });
  return url;
}

getSupplierDetails = function(supplier_id, cb) {
  console.log("Getting the supplier details for the given id:" + supplier_id);
  var suppliers = admin.database().ref('suppliers');
  var found = false;
  var supplier_details = undefined;
  suppliers.once('value', function(snapshot) {
    snapshot.forEach(function(supplier) {
      var id = supplier.key;
      var details = supplier.val();
      if(id === supplier_id && details['status']) {
        console.log('Found the supplier details for the given id:' + supplier_id);
        supplier_details = details;
        found = true;
        return true;
      }
    });
    if(found) {
      return cb(undefined, supplier_details);
    } else {
      return cb(new Error("Supplier is not found for the given supplier_id:" + supplier_id), undefined);
    }
  }).catch(function(err) {
    console.log('Failed to get the suppliers details. Error:' + err);
    return cb(err, undefined);
  });
}

app.post('/uploadAndProcessSupplierInventories', function (req, res) {
  console.log('Function has been called now');
  if (req.method !== 'POST') {
    return res.status(405).json({
      'error': "Unsupported method. This method:" + req.method + " is not allowed"
    });
  }
  var bulk_import_id = new Date().getTime() + "";

  parseRequestAndDownloadFiles(req, bulk_import_id, function (err, fields, filesList) {
    if (err) {
      console.log('Error while parse/download the file. Error:' + err)
      return res.status(400).json({
        error: 'Error occurred while downloading the the selected file. Error:' + err
      })
    }
    console.log('Successfully parsed the input parameters...and uploaded files...');
    console.log('List of parameters passed is:' + JSON.stringify(fields))
    console.log('List of files uploaded is:' + JSON.stringify(filesList))
    var supplier_id = fields['supplier_id'];
    console.log('Supplier id:' + supplier_id);
    if (supplier_id === undefined || filesList === undefined || filesList.length <= 0) {
      console.log('Error as no files are present in the input request')
      return res.status(400).json({
        error: 'Error as no files or supplier details are present in the input request'
      })
    }
    saveUploadedSupplierInventoryFile(supplier_id, bulk_import_id, filesList['supplier_inventory_file'], function (err, link) {
      if (err) {
        console.log('Error while saving the supplier uploaded zip file. Error:' + err)
        return res.status(400).json({
          error: 'Error while saving the supplier uploaded zip file. ' + err
        })
      }
      storeUploadedSupplierInventoryFileIntoDatabase(supplier_id, bulk_import_id, link, function (err, result) {
        if (err) {
          console.log('Error while storing the supplier uploaded zip file to database. Error:' + err)
          return res.status(400).json({
            error: 'Error while saving the supplier uploaded zip file to database. ' + err
          })
        }
        res.status(200).json({
          bulk_import_id: bulk_import_id
        });
        setTimeout(function () {

          processSupplierInventoryFiles(filesList, bulk_import_id, function (err, fileData) {
            if (err) {
              console.log('Error while processing the Supplier Inventory Files. Error:' + err)
              // return res.status(400).json( {
              //   error : 'Error while processing the Supplier Inventory Files. ' + err
              // })
              return updateUploadStatusSupplierInventoryFileIntoDatabase(supplier_id, bulk_import_id, err);
            }
            if (fileData === undefined || fileData['name'] === undefined || fileData['inventories_list'] === undefined) {
              console.log('Error as supplier name / inventories could not be retrieved from the uploaded file')
              // return res.status(400).json( {
              //   error : 'Error as supplier name / inventories could not be retrieved from the uploaded file'
              // })
              return updateUploadStatusSupplierInventoryFileIntoDatabase(supplier_id, bulk_import_id, "Error as supplier name / inventories could not be retrieved from the uploaded file");
            }

            var supplier_key = supplier_id;

            //Removed it as we are passing the supplier id as one of the request parameters;
            // findSupplierByName(fileData['name'], function(err, supplier_key) {
            //   if(err) {
            //     console.log('Error while processing the Supplier Inventory Files. Error:' + err)
            //     return res.status(400).json( {
            //       error : 'Error while processing the Supplier Inventory Files. ' + err
            //     })
            //   }

            //upload the file which is active

            inventories_list = fileData['inventories_list'];
            uniqueCategories = inventories_list['categories']
            inventories = inventories_list['inventories'];
            updateSupplierInventoryCategories(supplier_key, uniqueCategories, function (err, db_categories) {
              if (err) {
                // console.log('Error while updating the supplier inventory actegories. Error:' + err)
                // return res.status(400).json( {
                //   error : 'Error while processing the Supplier Inventory Files. ' + err
                // })
                return updateUploadStatusSupplierInventoryFileIntoDatabase(supplier_key, bulk_import_id, err, res)
              }

              updateSupplierInventories(supplier_key, bulk_import_id, inventories, db_categories, function (err, result) {
                return updateUploadStatusSupplierInventoryFileIntoDatabase(supplier_key, bulk_import_id, err, res)
              })
            })
          });
        }, 500);
      });
    });
  });
});

getVMExternalIP = function(results) {
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

app.get('/isBuildEnvironmentRunning', function(req, res) {
  isVMUpAdRunning(build_env, function(err, result) {
    if(err) {
      console.log('Failed in checking the build environment. Error:' + err)
      return res.status(400).json( {
        error : err
      })
    }
    console.log("Build Environment up/running:" + result);
    res.status(200).json( {
      status : result
    });
  });
})

app.post('/startBuildEnvironment', function(req, res) {
  startVMInstance(build_env, function(err, result) {
    if(err) {
      console.log('Failed in starting the build environment. Error:' + err)
      return res.status(400).json( {
        error : err
      })
    }
    console.log("Success in starting the build environment:" + result);
    res.status(200).json( {
      status : result
    });
  });
});

app.post('/stopBuildEnvironment', function(req, res) {
  stopVMInstance(build_env, function(err, result) {
    if(err) {
      console.log('Failed in stopping the build environment. Error:' + err)
      return res.status(400).json( {
        error : err
      })
    }
    console.log("Success in stopping the build environment:" + result);
    res.status(200).json( {
      status : result
    });
  });
});

app.post('/waitBuildEnvironment', function(req, res) {
  waitForVMInstance(build_env, function(err, result) {
    if(err) {
      console.log('Failed in waiting for the build environment. Error:' + err)
      return res.status(400).json( {
        error : err
      })
    }
    console.log("Success in getting the build environment:" + result);
    res.status(200).json( {
      status : result
    });
  });
});

app.post('/triggerSupplierBuild', function(req, res) {
  var supplier_id = req.body['supplier_id']
  console.log("Triggering the supplier app for id:" + supplier_id);
  if(supplier_id === undefined || supplier_id.trim().length <= 0) {
    console.log('Error as no supplier id is found in the request for triggering the build')
    return res.status(400).json({
      error: 'Error as no supplier id is found in the request for triggering the build'
    })
  }
  getSupplierDetails(supplier_id, function(err, supplier_details) {
    if(err) {
        console.log('Error while fetching the supplier details. Error:' + err);
        return res.status(400).json({
            error: JSON.stringify(err)
        })
    }
    var build_details = supplier_details['build_details']
    if(build_details !== undefined) {
        var status = build_details['status']
        if(status === "INPROGRESS") {
            console.log("The build is in-progress...")
            return res.status(200).json( {
                status : "INPROGRESS"
            });
        } else if(status === "COMPLETED")  {
            console.log("The build is completed...")
            return res.status(200).json( {
                apk_file : build_details['apk_file'],
                status : "COMPLETED"
            });
        }
    }
    isVMUpAdRunning(build_env, function(err, result) {
      if(err) {
        console.log('Failed in checking the build environment. Error:' + err)
        return res.status(400).json({
          error: 'Build Environment is down. Start it and wait for some time to be up/running'
        });
      }
      console.log('Current VM running status:' + result);
      if(! result) {
        console.log("Results about the VM instance is down / not found.")
        return res.status(400).json({
          error: 'Build Environment is down. Start it and wait for some time to be up/running'
        });
      }

      waitForVMInstance(build_env, function(err, external_ip) {
        if(err) {
          console.log('Failed in getting the VM instance Details. Error:' + err)
          return res.status(400).json({
            error: 'Error as trigger the supplier build. Error:' + err
          });
        
        }
        
        console.log("External IP found as:" + external_ip);
        var build_trigger_uri = BUILD_TRIGGER_URI.replace("<EXTERNAL_IP>", external_ip);
        console.log("triiger url is:" + build_trigger_uri );
        var payload = {
          "supplier_id" : supplier_id,
          "build_type" : "Debug"
        }
        console.log("Ready to tigger the build now");
        triggerSupplierBuildRequest(build_trigger_uri, payload, function(err, status) {
          if(err) {
            console.log("Failed while waiting for the build to get it completed. Error:" + err);
            return res.status(400).json({
              error: 'Error as trigger the supplier build. Error:' + err
            });
      
          }
          res.status(200).json( {
            status : 'SUCCESS'
          });
        });
      });
    });
  });
});

app.get('/checkSupplierExists', function(req, res) {
  
  var phonenumber = req.query.phonenumber;
  phonenumber = phonenumber.trim();
  var decodedPhoneNumber = Buffer.from(phonenumber, 'base64').toString(); 

  var suppliers = admin.database().ref('suppliers');
  var supplier_id = undefined;
  var found = false;
  console.log("Checking the decoded phone number:" + decodedPhoneNumber);
  suppliers.once('value', function(snapshot) {
    snapshot.forEach(function(supplier) {
      var id = supplier.key;
      var details = supplier.val();
      //console.log('current supplier details:' + details['phoneNumber'] + " comparing with:" + decodedPhoneNumber);
      // &&
      if(details['status'] && details['phoneNumber'] !== undefined && details['phoneNumber'] === decodedPhoneNumber) {
        supplier_id = id;
        found = true;
      }
    });
    if(found) {
      console.log("Supplier is found for the given phone number and ID:" + supplier_id);
      return res.status(200).json( {
        status : 'Supplier exists with the given number',
        id : supplier_id
      })
    } else {
      console.log("Supplier is NOT found for the given phone number " + decodedPhoneNumber);

      return res.status(400).json( {
        error : 'Supplier doesnt with the given number',
      })
    }
  }).catch(function(err) {
    console.log('Failed to get the suppliers details. Error:' + err);
    return res.status(400).json( {
      error : "Error while checking the supplier availability"
    })
  });
});

// exports.uploadAndProcessSupplierInventories = functions.https.onRequest(uploadAndProcessSupplierInventories);
// exports.sampleFileUpload = functions.https.onRequest(sampleFileUpload)
exports.app = functions.https.onRequest(app);

//Send Push Notifications
exports.sendNotify = functions.database.ref("demands/{demandId}").onUpdate((snap, context) => {
  const data = snap.after.val();
  var status = data['status'];
  var deliveryTime = data['deliveryTime'];
  var consumer = data['consumer'];
  var orderId = data['key'];

  if (status === "Accepted") {
      const payload = {
          notification: {
              title: "Order Accepted" + " (Order ID: " + orderId + ")",
              body: "Order is expected to deliver on " + deliveryTime,
          }
      };

      return admin.database().ref('tokens/' + consumer).once("value", tokenSnap => {
          var token = tokenSnap.val();
          return admin.messaging().sendToDevice(token, payload);
      });

  } else if (status === "Rejected") {
      const payload = {
          notification: {
              title: "Order Rejected" + " (Order ID: " + orderId + ")",
              body: "Order rejected on " + deliveryTime,
          }
      };

      return admin.database().ref('tokens/' + consumer).once("value", tokenSnap => {
          var token = tokenSnap.val();
          return admin.messaging().sendToDevice(token, payload);
      });
  } else if (status === "Delivered") {
      const payload = {
          notification: {
              title: "Order Delivered" + " (Order ID: " + orderId + ")",
              body: "Order delivered on " + deliveryTime,
          }
      };

      return admin.database().ref('tokens/' + consumer).once("value", tokenSnap => {
          var token = tokenSnap.val();
          return admin.messaging().sendToDevice(token, payload);
      });
  }
});