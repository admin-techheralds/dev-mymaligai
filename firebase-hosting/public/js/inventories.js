logged_in_supplier_details = undefined;

categories = []
inventories = []
active_inventory_count = 0;
instock_inventory_count = 0;
inventories_distribution_type_list = [
    "250g, 500g, 1kg, 2kg, 5kg, 10kg",
    "5g, 10g, 25g, 50g, 100g, 150g",
    "200ml, 250ml, 500ml, 1L, 2L",
    "10ml, 25ml, 50ml, 100ml, 150ml",
    "1pc, 2pcs, 5pcs,10pcs, 20pcs"
]

inventories_distribution_type_mapping = [
    {
        "distribution_id" : 0,
        "distribution_text" : "250g, 500g, 1kg, 2kg, 5kg, 10kg"
    },
    {
        "distribution_id" : 1,
        "distribution_text" : "5g, 10g, 25g, 50g, 100g, 150g"
    },
    {
        "distribution_id" : 2,
        "distribution_text" : "200ml, 250ml, 500ml, 1L, 2L"
    },
    {
        "distribution_id" : 3,
        "distribution_text" : "10ml, 25ml, 50ml, 100ml, 150ml"
    },
    {
        "distribution_id" : 4,
        "distribution_text" : "1pc, 2pcs, 5pcs,10pcs, 20pcs"
    }
]


var callbackOnConfirmation = undefined;
var selectedItemForDeletion = undefined;

showErrorAlert = function(msg) {
    $(document).Toasts('create', {
        title:'Error',
        class: 'bg-danger',
        autohide : true,
        delay : 2000,
        body : msg
    })
}

showInfoAlert = function(msg) {
    $(document).Toasts('create', {
        title:'Info',
        class: 'bg-info',
        autohide : true,
        delay : 2000,
        body : msg
    })
}

initInventoryUI=function(){
    hideElement('#inventory_list');
    closeBulkUploadInvenoriesUI();
    closeEditInventory();
    closeAddInventory();
}

loadSupplierDetails = function(cb) {
    console.log("Loading supplier details for id:" + loggedinUser['uid']);
    logged_in_supplier_details = {}
    db.ref('suppliers/' + loggedinUser['uid']).once('value', function(snapshot) {
        // console.log('Supplier details had come now.. value:' + JSON.stringify(snapshot));
        snapshot.forEach(function(k) {
            logged_in_supplier_details[k.key] = k.val();
        })
        if(cb != undefined) {
            cb(undefined, "SUCCESS");
        }
    })
}

downloadAndroidApp = function() {
    var build_details = logged_in_supplier_details['build_details'];
    if(build_details !== undefined) {
        var status = build_details['status'];
        if(status !== undefined && status === "COMPLETED") {
            download(build_details['apk_file'], logged_in_supplier_details['name'])
        } else if(status !== undefined && status === "INPROGRESS") {
            showInfoAlert("Currently your Android app build is being created. Reload this page later to get the Androind App.");
            return;
        }
    } else {
        showConfirmationDialog("Your Android App is not available. Do you want to create one for you ?",
                            triggerSupplierAppBuild);
    }
}

download = function(url, filename) {
    
    showInfoAlert('Android application download in-progress...');
    // setTimeout(function() {
    //     closeStatusDialog()
    // }, 3000)
    fetch(url).then(function(t) {
        return t.blob().then(function(b) {
            console.log("Blob data has been received");
            var a = document.createElement("a");
            a.href = URL.createObjectURL(b);
            a.setAttribute("download", filename);
            a.click();
            showInfoAlert('Download completed...');
        });
    });
}

triggerSupplierAppBuild = function() {
    alert('Got signal to Build the Android app');
}

loadDashboard = function() {
    hideElement('#inventories_section')
    hideElement('#categories_section')
    showElement('#dashboard-section');

    console.log('Loading categories...')
    loadCategoryItemsFromDB(true, function(err, result) {
        console.log("Category loading result:" + result)
        loadInventoryItemsFromDB(true, function(err, result) {
            console.log('Inventory loading result:' + result);
            updateInventoriesUI();
        })
    });
}

updateInventoriesUI = function() {

    $('#category-widget-dashboard .overlay').remove();
    $('#inventory-widget-dashboard .overlay').remove();
    $('.inventories_count').text( instock_inventory_count + " / " + inventories.length)
    $('.categories_count').text(categories.length)
    // $('#active_inventories_desc').html('Items available for Customers to Order: <span class="badge">' + active_inventory_count + ' </span>')
}

filterInventories = function(event ) {

    var search=$.trim($('#inventories_grid #search_inventory').val());
    if(search.length<=0){
        console.log("Resettheinventory...");
        $("#inventories_edit_grid").jsGrid("loadData").done(function(){
            console.log('Resetloadingisdone')
        });
        return;    
    }
    console.log("Search item:"+search);
    $("#inventories_edit_grid").jsGrid("search", { "item_name": search }).done(function() {
        console.log("filtering completed");
        $("#inventories_edit_grid").jsGrid("refresh");
    });

    /*

    if(event.keyCode===27){
        $('#inventories_grid #search_inventory').val('');
        console.log("Resettheinventory...");
        $("#inventories_edit_grid").jsGrid("loadData").done(function(){
            console.log('Resetloadingisdone')
        });
        return;
    }
    if(event.keyCode===13){
        var search=$.trim($('#inventories_grid #search_inventory').val());
        if(search.length<=0){
            console.log("Resettheinventory...");
            $("#inventories_edit_grid").jsGrid("loadData").done(function(){
                console.log('Resetloadingisdone')
            });
            return;    
        }
        console.log("Search item:"+search);
        $("#inventories_edit_grid").jsGrid("search", { "item_name": search }).done(function() {
            console.log("filtering completed");
            $("#inventories_edit_grid").jsGrid("refresh");
        });
    }
    */
}


loadInventories = function() {

    showElement('#inventories_section')
    hideElement('#categories_section')
    hideElement('#dashboard-section');

    showElement('#inventory_list');
    hideElement('#addInventory');
    hideElement('#editInventory');
    hideElement('#bulkUploadInventories');
    
    //Hide the grid
    hideElement('#bulk_upload_inventories_status');
    showElement("#inventories_grid");

    $("#inventories_edit_grid").jsGrid({
        // height: "70%",
        width: "100%",
        sorting: true,
        paging: true,

        filtering: false,
        editing: true,
        sorting: true,
        paging: true,
        autoload: true,
 
        pageSize: 100,
        pageButtonCount: 5,
        noDataContent : "No Inventories are found",
        fields: [
            { 
                title: "Image", name: "img", width : 80, 
                cellRenderer  : function(value, item) {
                    return "<td><img src=" + value + " /></td>";
                }
            },
            { title : "Category", name: "category_id", type: "select", items: categories, valueField: "category_id", textField: "category_name", width: 75 },
            { title : "Item Name", name: "item_name", type: "text", width: 150 },
            // { title : "Distribution", name: "quantity_type", type: "number", width: 75 },
            { title : "Distribution", name: "quantity_type",  type: "select", items: inventories_distribution_type_mapping, valueField: "distribution_id", textField: "distribution_text", width: 75  },
            
            { title: "Price", name: "price", type: "number", width: 50,
                itemTemplate : function(value) {
                    return "Rs: " + value.toFixed(2);
                } },
            { title : "Is Stock Available ?", name: "in_stock", 

                cellRenderer  : function(value, item) {
                    if(value !== undefined && value == 1) {
                        return "<td align='center'><input type='checkbox' class='instock-inventories'  data-size='mini' name='instock-inventories-checkbox'  data-on-text='Yes' data-off-text='No' checked id='" + item['sku']  + "' category_id='" + item['category_id'] + "'></td>";
                    } else {
                        return "<td align='center'><input type='checkbox' class='instock-inventories'  data-size='mini'  name='instock-inventories-checkbox' data-on-text='Yes' data-off-text='No' id='" + item['sku']  + "' category_id='" + item['category_id'] + "'></td>";
                    }
                } 
            },
            {  
                type: "control",
                modeSwitchButton: false,
                editButton: true,
                headerTemplate: function() {
                    return $("<button>").addClass("btn btn-block btn-info").attr("type", "button").text("Add")
                            .on("click", function () {
                                showAddInventoryView();
                            });
                }
            }
        ],
        controller: {
            data:inventories,
            loadData: function (filter) {
                return $.grep(this.data, function (item) {
                    return (!filter.item_name || item.item_name.toLowerCase().indexOf(filter.item_name.toLowerCase()) >= 0);
                });
            },          
        },

        onRefreshed : function(grid, data) {
            $("[name='instock-inventories-checkbox']").bootstrapSwitch();
        },
        
        deleteConfirm: function(item) {
            return "Inventory item \"" + item.item_name + "\" will be removed. Are you sure?";
        },
        editItem : function(item) {
            var $row = $("#inventories_edit_grid").jsGrid("rowByItem", item);
            editSelectedInventoryItem(item, $row);
            
        },
        deleteItem : function(item) {
            deleteSelectedItem(item);       
        }
    });

    $('#inventories_grid #search_inventory').focus();
    $("[name='instock-inventories-checkbox']").bootstrapSwitch();

}

enableDisableInventoryItem = function(cat_id, sku_id, enable) {
    console.log('cat id:' + cat_id + ', sku:' + sku_id + " with state:" + enable);
    value = 0
    if(enable) {
        value = 1
    }
    console.log('Setting item: inventory/' + loggedinUser['uid'] + '/' + cat_id + '/' + sku_id)
    db.ref('inventory/' + loggedinUser['uid'] + '/' + cat_id + '/' + sku_id).update( {
        'in_stock' : value
    }).then(function() {
        console.log('Successfully updated the item.');
        updateInventoriesWithStockOptions(cat_id, sku_id, value, function() {
            reCalculateInventories(function() {
                updateInventoriesUI();
                showInfoAlert('Successfuly updated the instock option for the selected inventory');
            })
        });
    }).catch(function(err) {    
        console.log('Failed to update the in-stock option for the selected item. Error:' + err);
        showErrorAlert("Failed to update the in-stock option for the selected Inventory item.");
    });
}

updateInventoriesWithStockOptions =  function(cat_id, sku_id, value, cb) {
    for(var i = 0 ; i < inventories.length; i++) {
        var item = inventories[i];
        if(item['sku'] == sku_id && item['category_id'] == cat_id ) {
            item['in_stock'] = value
            break;
        }
    }
    $('#inventories_edit_grid').jsGrid('refresh')
    cb();
}

uploadBulkInventories = function(e) {

    //e.preventDefault();

    var selected_file = $.trim($('#bulkUploadInventories #supplier_inventory_file').val());
    console.log('Selected file to upload:' + selected_file);
    if(selected_file.length <= 0) {
        console.log("File to be uploaded is found to be invalid");
        showErrorAlert("Select the zip file to be uploaded");
        return;
    }
    var formData = $("#bulkUploadInventories #bulkUploadForm").submit(function (e) {
        console.log('Form data inside submitting...');
        return;
    });

    setLoadingBtn('#bulkUploadInventories #btnBulkUpload', 'Uploading');
    console.log('Submitting the data to:' + $('#bulkUploadInventories #bulkUploadForm').attr('action'))
    var formData = new FormData(formData[0]);
    $.ajax({
        url: $('#bulkUploadInventories #bulkUploadForm').attr('action'),
        type: 'POST',
        data: formData,
        success: function (response) {
            resetLoadingBtn('#bulkUploadInventories #btnBulkUpload', 'Upload');
            console.log("Response for the POST request is\n:" + JSON.stringify(response));
            if(response != undefined && response['bulk_import_id'] != undefined){
                showInfoAlert("Successfully uploaded the inventory file list to server for processing. ID:" + response['bulk_import_id']);
                
                loadDashboard();
            } else {
                showErrorAlert("Failed to upload the inventory list to server.");
            }
        },
        contentType: false,
        processData: false,
        cache: false
    });

    return false;

}

showBulkUploadInvenoriesUI = function() {
    hideElement('#inventory_list');
    hideElement('#addInventory');
    hideElement('#editInventory');
    $('#bulkUploadInventories #supplier_id').val(loggedinUser['uid']);
    showElement('#bulkUploadInventories');
    $('#bulkUploadInventories #supplier_inventory_file_display').text('Browse to select a zip file')
    $('#bulkUploadInventories #supplier_inventory_file').val('')

    resetLoadingBtn('#bulkUploadInventories #btnBulkUpload', 'Upload');
}

showBulkUploadInvenoriesStatusUI = function() {
    hideElement('#inventory_list');
    hideElement('#addInventory');
    hideElement('#editInventory');
    $('#bulkUploadInventories #supplier_id').val(loggedinUser['uid']);
    hideElement('#bulkUploadInventories');
    
    showElement('#bulk_upload_inventories_status');
    hideElement("#inventories_grid");

    showElement('#inventory_list');
    loadBulkUploadStatusList(function(err, result) {
        console.log("Daa to display is:" + JSON.stringify(result));
        $("#bulk_upload_inventories_status").jsGrid({
            // height: "70%",
            width: "100%",
            sorting: true,
            paging: true,
    
            filtering: false,
            editing: false,
            sorting: true,
            paging: true,
            autoload: true,
     
            pageSize: 100,
            pageButtonCount: 5,
            noDataContent : "No Bulk Upload details found",
            fields: [
                { title : "Status", name: "status", width: 75, type : "text", 
                    itemTemplate : function(value, item) {
                        if(value != undefined && value.trim().length > 0) {
                            var result = value.trim().toLowerCase();
                            if(result == "success") {
                                return '<span class="badge badge-success">' + value.trim() + '</span>';
                            } else if(result == "fail" || result == 'failed') {
                                return '<span class="badge badge-danger">' + value.trim() + '</span>';
                            }
                            return '<span class="badge badge-secondary">' + value.trim() + '</span>';
                        }
                        return '<span class="badge badge-warning"> UnKnown </span>';
                    }
                },
                { title : "Uploaded On", name: "imported_on", type: "text", width: 100,
                    itemTemplate : function(value, item) {
                        return moment(value).format("DD/MMM/YYYY hh:mm:ss a");
                    } 
                },
                { title : "Processing Completed On", name: "result_updated_on", type: "text", width: 100,
                    itemTemplate : function(value, item) {
                        return moment(value).format("DD/MMM/YYYY hh:mm:ss a");
                    } 
                },
                { title : "Download Invenory File Used", name: "uploaded_inventory_file", type: "text", width: 50, 
                    itemTemplate : function(value, item) {
                        return '<a href="' + value + '" alt="Download"><i class="fa fa-cloud-download-alt"></i></a>';
                    } 
                },
            ],
            data: result
        });
        $("#bulk_upload_inventories_status").jsGrid("render").done(function() {
            console.log('Rendering the bulk upload status is complete');
        });
    })
}

loadBulkUploadStatusList = function(cb) {
    console.log('Loading the bulk upload status..');
    db.ref('bulkimport/' + loggedinUser['uid']).once('value', function(snapshot) {
        console.log('bulk upload status had come now. results:' + JSON.stringify(snapshot));
        var bulk_import_details = []
        snapshot.forEach(function(i) {
            
            var bulk_import_id = i.key;
            var details = i.val();
            details['bulk_import_id'] = bulk_import_id;
            console.log("Details:" + JSON.stringify(details))
            bulk_import_details.push(details);
        });
        if(cb != undefined) {
            cb(undefined, bulk_import_details);
        }
    })
}

closeBulkUploadInvenoriesUI = function() {
    resetLoadingBtn('#bulkUploadInventories #btnBulkUpload', 'Upload');
    showElement('#inventory_list');
    hideElement('#addInventory');
    hideElement('#editInventory');
    hideElement('#bulkUploadInventories');
}

yesOnConfirmationDialog = function() {
    callbackOnConfirmation();
}

showConfirmationDialog = function(confirmationTxt, cb) {
    callbackOnConfirmation = cb;
    $('#confirmationDialog .modal-body p').html(confirmationTxt);
    $('#confirmationDialog').modal('show');
}

deleteSelectedItem = function(item) {
    selectedItemForDeletion = item;
    showConfirmationDialog("Do you want to delete the Inventory: <strong>" + item.item_name + "</strong> ?",
                            deleteSelectedItemConfirmation);
}

deleteSelectedItemConfirmation = function() {
    var item = selectedItemForDeletion;
    console.log('Confirmaton is received for the selected item..');
    var sku = item['sku']
    var item_name = item['item_name'];
    if(sku === undefined) {
        sku = item_name.toLowerCase();
    }
    console.log('Deleting item: inventory/' + loggedinUser['uid'] + '/' + item['category_id'] + '/' + sku)
    db.ref('inventory/' + loggedinUser['uid'] + '/' + item['category_id'] + '/' + sku).update( {
        'active' : 0
    }).then(function() {
        console.log('Successfully deleted the item.');
        item['active'] = 0;
        removeInventoryItemToTable(item, function() {
            updateInventoriesUI();
            showInfoAlert('Successfuly deleted the selected item');
        })
    }).catch(function(err) {    
        console.log('Failed to delete the item. Error:' + err);
        showErrorAlert("Failed to delete the Inventory item.");
    });
}


editSelectedInventoryItem = function(item, $row) {
    console.log("Editing item:" + JSON.stringify(item));
    if(item == undefined || item['name'] == undefined) {
        return;
    }

    var options = []
    var index = 0;
    var selecteditem = undefined;
    categories.forEach(function(c) {
        if(item['category_id'] ==  c['category_id']) {
            selecteditem = c['category_id'];
        }
        options.push('<option value="' + c['category_id'] + '">' + c['category_name'] + '</option>');
        index = index + 1;
    });
    $('#editInventory #category_id').html(options.join(''));
    $('#editInventory #category_id').selectpicker('refresh');
    $('#editInventory #category_id').selectpicker('val', selecteditem);

    index = 0;
    options.length = 0;
    selecteditem = undefined;
    inventories_distribution_type_list.forEach(function(c) {
        if(item['quantity_type'] ==  (index + "")) {
            selecteditem = index + ""
        }
        options.push('<option value="' + index + '">' + c + '</option>');
        index = index + 1;
    });
    console.log("Selected item distribution type is:" + selecteditem);
    $('#editInventory #distribution_type').html(options.join(''));
    $('#editInventory #distribution_type').selectpicker('refresh');
    $('#editInventory #distribution_type').selectpicker('val', selecteditem);
    $('#editInventory #distribution_type').selectpicker('refresh');

    $('#editInventory #item_name').val(item['item_name']);
    $('#editInventory #item_price').val(item['price']);
    $('#editInventory #selected_item').val(JSON.stringify(item));
    $('#editInventory #selected_row_for_edit').val(JSON.stringify($row));
    
    var edit_item_heading = item['item_name'];
    if(item['sku'] != undefined) {
        edit_item_heading = item['sku']
    }
    $('#editInventory #selected_item_sku').text(edit_item_heading);

    resetLoadingBtn('#editInventory #btnSaveInventory', 'Save');

    hideElement('#inventory_list');
    hideElement('#addInventory');
    hideElement('#bulkUploadInventories');
    
    showElement('#editInventory');
}


saveEditInventory = function() {
    var item_name = $.trim($('#editInventory #item_name').val())
    var item_price = $.trim($('#editInventory #item_price').val())
    var item_category_id = $.trim($('#editInventory #category_id').selectpicker('val'))
    var item_distribution_type = $.trim($('#editInventory #distribution_type').selectpicker('val'))
    if(item_price.length <= 0) {
        showErrorAlert("Inventory item Price cannot be empty");
        $('#editInventory #item_price').focus();
        return;
    }
    if(item_distribution_type.length <= 0) {
        showErrorAlert("Inventory item Distribution Type cannot be empty");
        $('#editInventory #distribution_type').focus();
        return;
    }    

    var selected_file = $.trim($('#addInventory #item_image_file').val());
    // if(selected_file.length <= 0) {
    //     showErrorAlert("Image for the Inventory item is not found / invalid.");
    //     $('#addInventory #item_image_file').focus();
    //     return;
    // }
    var selected_file = $.trim($('#editInventory #item_image_file').val());
    var saveWithImage = selected_file.length > 0;
    console.log('Save With Image:' + saveWithImage);
    console.log('Selected file is:' + selected_file);
    var item_details = {
        item_name : item_name,
        category_id : item_category_id,
        price : parseFloat(item_price),
        quantity_type : parseInt(item_distribution_type)
    }
    if(saveWithImage) {
        var index = selected_file.lastIndexOf('\\');
        if(index == -1) {
            index = selected_file.lastIndexOf('/');
        }
        if(index == -1) {
            showErrorAlert("Failed to get the file name from the uploaded file");
            $('#editInventory #item_image_file').focus();
            return;
        }
        var filename = selected_file.substring(index + 1);
        console.log("File name alone is:" + filename);
        var file =$("#editInventory #item_image_file").get(0).files[0];
        var destination_file_path = 'images/' + loggedinUser['uid'] + '/' + item_category_id + '/' + filename;
        console.log('Destination path is:' + destination_file_path)
        var image_ref = storage.child(destination_file_path);

        setLoadingBtn('#editInventory #btnSaveInventory', 'Saving');
        image_ref.put(file).then(function(snapshot) {
            
            image_ref.getDownloadURL().then(function(url) {
                item_details['img'] =  url;
                resetLoadingBtn('#editInventory #btnSaveInventory', 'Save');
                saveUpdatedInventory(item_details);
            }).catch(function(err) {
                console.log('Failed to  retrieve the downloadable link for the uploaded image file  while editing the Inventory. Error:' + err);
                showErrorAlert("Failed to save the Inventory Image file while editing the Inventory.");
                resetLoadingBtn('#editInventory #btnSaveInventory', 'Save');
            });
        }).catch(function(err) {
            console.log('Failed to store the item image file to server while editing the Inventory. Error:' + err);
            showErrorAlert("Failed to save the Inventory Image file file while editing the Inventory.");
            resetLoadingBtn('#editInventory #btnSaveInventory', 'Save');
        });
    } else {
        saveUpdatedInventory(item_details);
    }
}

saveUpdatedInventory = function(item) {
    setLoadingBtn('#editInventory #btnSaveInventory', 'Saving');
    var org_item_details = JSON.parse($('#editInventory #selected_item').val());
    var item_details = {
        price : org_item_details['price'],
        item_name : org_item_details['item_name'],
        category_id : org_item_details['category_id'],
        img : org_item_details['img'],
        name : org_item_details['name'],
        active : org_item_details['active'],
        sku : org_item_details['sku'],
    }
    var update_details = {
        price : item.price,
        quantity_type : item['quantity_type']
    }
    item_details['price'] = item['price'];
    item_details['quantity_type'] = item['quantity_type'];

    if(item['img'] != undefined) {
        update_details['img'] = item['img']
        item_details['img'] = item['img'];
    }
    var sku = org_item_details['sku'];
    if(sku == undefined) {
        sku = item['item_name'].toLowerCase()
    }

    db.ref('inventory/' + loggedinUser['uid'] + '/' + item['category_id'] + '/' + sku).update(
        update_details).then(function() {
        console.log('Successfully updated the item.');
        
        // loadInventoryItemsFromDB(true, function() {
        //     closeEditInventory();
        // })
        updateInventoryItemToTable(item_details, function() {
            resetLoadingBtn('#editInventory #btnSaveInventory', 'Save');
            closeEditInventory();
        })
    }).catch(function(err) {
        console.log('Failed to update the item. Error:' + err);
        showErrorAlert("Failed to update the Inventory item.");
        resetLoadingBtn('#editInventory #btnSaveInventory', 'Save');
    });
}

//Not working as expected.
updateInventoryItemToTable =  function(replace_with_this, cb) {
    var indexToUpdate = -1;
    for(var i = 0 ; i < inventories.length; i++) {
        var item = inventories[i];
        if(item['item_name'] == replace_with_this['item_name'] 
            && item['category_id'] == replace_with_this['category_id'] ) {
                indexToUpdate = i;
                break;
            }
    }
    if(indexToUpdate != -1) {
        console.log('Updated the data into the table');
        inventories[indexToUpdate] = replace_with_this;
    }
    $('#inventories_edit_grid').jsGrid('refresh')
    cb();
}

removeInventoryItemToTable =  function(replace_with_this, cb) {
    var indexToRemove = -1;
    for(var i = 0 ; i < inventories.length; i++) {
        var item = inventories[i];
        if(item['item_name'] == replace_with_this['item_name'] 
            && item['category_id'] == replace_with_this['category_id'] ) {
                indexToRemove = i;
                break;
            }
    }
    if(indexToRemove != -1) {
        console.log('Removing the data into the table');
        inventories.splice(indexToRemove, 1);
    }
    $('#inventories_edit_grid').jsGrid('refresh')
    cb();
}

var substringMatcher = function(items) {
    return function findMatches(q, cb) {
      var matches, substringRegex;
  
      // an array that will be populated with substring matches
      matches = [];
  
      // regex used to determine if a string contains the substring `q`
      substringRegex = new RegExp(q, 'i');
  
      // iterate through the pool of strings and for any string that
      // contains the substring `q`, add it to the `matches` array
      $.each(items, function(i, item) {
        if (substringRegex.test(item['item_name'])) {
          matches.push(item['item_name']);
        }
      });
  
      cb(matches);
    };
  };

showAddInventoryView = function() {
    
    hideElement('#inventory_list');
    showElement('#addInventory');
    hideElement('#editInventory');
    hideElement('#bulkUploadInventories');

    var options = []
    var index = 0;



    
    var selecteditem = undefined;
    categories.forEach(function(c) {
        if(index == 0) {
            options.push('<option value="' + c['category_id'] + '">' + c['category_name'] + '</option>');
            selecteditem = c['category_id'];
        } else {
            options.push('<option value="' + c['category_id'] + '">' + c['category_name'] + '</option>');
        }
        index = index + 1;
    });

   
    $('#addInventory #category_id').html(options.join(''));
    $('#addInventory #category_id').selectpicker('refresh');
    $('#addInventory #category_id').selectpicker('val', selecteditem);

    var options1 = [];
    index = 0;    
    inventories_distribution_type_list.forEach(function(c) {
        if(index == 0) {
            options1.push('<option value="' + index + '">' + c + '</option>');
            selecteditem = c;
        } else {
            options1.push('<option value="' + index + '">' + c + '</option>');
        }
        index = index + 1;
    });
    console.log("List of options found for distribution type is:" + options1.join(", "));
    $('#addInventory #distribution_type').html(options1.join(''));
    $('#addInventory #distribution_type').selectpicker('refresh');
    $('#addInventory #distribution_type').selectpicker('val', selecteditem);


    resetLoadingBtn('#addInventory #btnSaveInventory', 'Save');
}

uploadSelectedFileToServer = function() {

    var item_name = $.trim($('#addInventory #item_name').val())
    if(item_name.length <= 0) {
        showErrorAlert("Inventory item name cannot be empty before uploaded the item image");
        $('#addInventory #item_name').focus();
        return;
    }
    var item_category_id = $.trim($('#addInventory #category_id').selectpicker('val'))
    if(item_category_id.length <= 0) {
        showErrorAlert("Inventory item Category cannot be empty before uploaded the item image");
        $('#addInventory #category_id').focus();
        return;
    }

    var selected_file = $.trim($('#addInventory #item_image_file').val());
    if(selected_file.length <= 0) {
        showErrorAlert("Browse the file to be uploaded");
        $('#addInventory #item_image_file').focus();
        return;
    }
    console.log('Selected file is:' + selected_file);
    var index = selected_file.lastIndexOf('\\');
    if(index == -1) {
        index = selected_file.lastIndexOf('/');
    }
    if(index == -1) {
        showErrorAlert("Failed to get the file name from the uploaded file");
        $('#addInventory #item_image_file').focus();
        return;
    }
    var filename = selected_file.substring(index + 1);
    console.log("File name alone is:" + filename);
    var file =$("#addInventory #item_image_file").get(0).files[0];
    var destination_file_path = 'images/' + loggedinUser['uid'] + '/' + item_category_id + '/' + filename;
    console.log('Destination path is:' + destination_file_path)
    var image_ref = storage.child(destination_file_path);
    image_ref.put(file).then(function(snapshot) {
        image_ref.getDownloadURL().then(function(url) {
            showInfoAlert('File has been uploaded successfully..!!!');
            $('#addInventory #item_image_url').val(url);        
            $('#addInventory #btnSaveInventory').attr('disabled', false);
        })
    });
}

isItemNameAvailableInSelectedCategory = function(name, category_id) {
    var found = false;
    for(var i = 0; i < inventories.length; i++) {
        var item = inventories[i];
        if(item['item_name'] == name && item['category_id'] == category_id) {
            found = true;
            break;
        }
    }
    return found;
}

getMaxComponentForSKU = function(value) {
    value = value + ""
    if(value.length >= 3) {
      return value.substr(0, 3)
    } else {
      while(value.length < 3) {
        value = "0" + value
      }
    }
    return value;
}
  
getsku = function(supplier_id, item) {
    return "sku" + 
            "-" + getMaxComponentForSKU(supplier_id) + 
            "-" + getMaxComponentForSKU(item['categoryname']) +
            "-" + getMaxComponentForSKU(item['itemname']) +
            "-" + getMaxComponentForSKU(item['attr1']) +
            "-" + getMaxComponentForSKU(item['attr2']) +
            "-" + getMaxComponentForSKU(item['attr3'])
}

setLoadingBtn = function(id, text) {
    $(id).html('<i class="spinbtn fas fa-3x fa-sync-alt fa-spin"></i>&nbsp;&nbsp;&nbsp;' + text + '...');
}

resetLoadingBtn = function(id, text) {
    $(id).html(text);
}

addInventory = function() {

    var item_name = $.trim($('#addInventory #item_name').val())
    var item_price = $.trim($('#addInventory #item_price').val())
    var item_category_id = $.trim($('#addInventory #category_id').selectpicker('val'))
    var item_category_name = $("#addInventory #category_id option:selected" ).text();
    var item_distribution_type = $.trim($('#addInventory #distribution_type').selectpicker('val'))

    var item_attr1 = $.trim($('#addInventory #item_attr1').val())
    var item_attr2 = $.trim($('#addInventory #item_attr2').val())
    var item_attr3 = $.trim($('#addInventory #item_attr3').val())

    if(item_name.length <= 0) {
        showErrorAlert("Inventory item cannot be empty");
        $('#addInventory #item_name').focus();
        return;
    }
    if(item_price.length <= 0) {
        showErrorAlert("Inventory item Price cannot be empty");
        $('#addInventory #item_price').focus();
        return;
    }
    console.log('Category ID:' + item_category_id)
    if(item_category_id.length <= 0) {
        showErrorAlert("Inventory item Category cannot be empty");
        $('#addInventory #category_id').focus();
        return;
    }
    if(isItemNameAvailableInSelectedCategory(item_name,item_category_id)){
        showErrorAlert("Inventory item is aleady available in the selected Category");
        $('#addInventory #item_name').focus();
        return;
    }
    if(item_distribution_type.length <= 0) {
        showErrorAlert("Inventory item Distribution Type cannot be empty");
        $('#addInventory #distribution_type').focus();
        return;
    }

    var selected_file = $.trim($('#addInventory #item_image_file').val());
    if(selected_file.length <= 0) {
        showErrorAlert("Image for the Inventory item is not found / invalid.");
        $('#addInventory #item_image_file').focus();
        return;
    }
    var selected_file = $.trim($('#addInventory #item_image_file').val());
    if(selected_file.length <= 0) {
        showErrorAlert("Browse the file to be uploaded");
        $('#addInventory #item_image_file').focus();
        return;
    }
    console.log('Selected file is:' + selected_file);
    var index = selected_file.lastIndexOf('\\');
    if(index == -1) {
        index = selected_file.lastIndexOf('/');
    }
    if(index == -1) {
        showErrorAlert("Failed to get the file name from the uploaded file");
        $('#addInventory #item_image_file').focus();
        return;
    }

    setLoadingBtn('#addInventory #btnSaveInventory', 'Saving');

    var filename = selected_file.substring(index + 1);
    console.log("File name alone is:" + filename);
    var file =$("#addInventory #item_image_file").get(0).files[0];
    var destination_file_path = 'images/' + loggedinUser['uid'] + '/' + item_category_id + '/' + filename;
    console.log('Destination path is:' + destination_file_path)
    var image_ref = storage.child(destination_file_path);
    
    var sku = getsku(loggedinUser['uid'], { 
                        'categoryname' : item_category_name,
                        'itemname' : item_name,
                        'attr1' : item_attr1,
                        'attr2' : item_attr2,
                        'attr3' : item_attr3,
                })
    
    image_ref.put(file).then(function(snapshot) {
        
        image_ref.getDownloadURL().then(function(url) {

            var item_details = {
                img : url,
                price : parseFloat(item_price),
                quantity_type : parseInt(item_distribution_type),
                name : item_name,
                active : 1,
                in_stock : 1,
                sku : sku,
                attr1 : item_attr1,
                attr2 : item_attr2,
                attr3 : item_attr3,
            }
            db.ref('inventory/' + loggedinUser['uid'] + '/' + item_category_id + '/' + sku).set(item_details).then(function() {
                console.log('Successfully saved the item.');
                item_details['category_id'] = item_category_id
                item_details['item_name'] = item_name;
                addInventoryItemToTable(item_details, function() {
                    resetLoadingBtn('#addInventory #btnSaveInventory', 'Save');
                    closeAddInventory();
                })
            }).catch(function(err) {
                console.log('Failed to save the item. Error:' + err);
                showErrorAlert("Failed to save the Inventory item.");
                resetLoadingBtn('#addInventory #btnSaveInventory', 'Save');
            });        
        }).catch(function(err) {
            console.log('Failed to  retrieve the downloadable link for the uploaded image file. Error:' + err);
            showErrorAlert("Failed to save the Inventory Image file.");
            resetLoadingBtn('#addInventory #btnSaveInventory', 'Save');
        });
    }).catch(function(err) {
        console.log('Failed to store the item image file to server. Error:' + err);
        showErrorAlert("Failed to save the Inventory Image file.");
        resetLoadingBtn('#addInventory #btnSaveInventory', 'Save');
    });
}

addInventoryItemToTable =  function(item, cb) {
    $("#inventories_edit_grid").jsGrid("insertItem", item).done(function() {
        console.log('Inserted the data into the table');
        cb();
    })
}

closeAddInventory = function() {

    $('#addInventory #item_name').val('')
    $('#addInventory #item_price').val('')
    $('#addInventory #item_image_file').val('')

    showElement('#inventory_list');
    hideElement('#addInventory');
    hideElement('#editInventory')
    hideElement('#bulkUploadInventories');

}

closeEditInventory = function() {
    $('#editInventory #item_name').val('')
    $('#editInventory #item_price').val('')
    $('#editInventory #item_image_file').val('')

    showElement('#inventory_list');
    hideElement('#addInventory');
    hideElement('#editInventory');
    hideElement('#bulkUploadInventories');
}


loadInventoryItemsFromDB = function(force, cb) {
    if(force) {
        inventories.length = 0; 
    }
    if(inventories.length <= 0) {
        console.log('Looking inventories from DB');
        instock_inventory_count = 0;
        db.ref('inventory/' + loggedinUser['uid']).once('value', function(snapshot) {
            console.log('Inventories had come now..');
            snapshot.forEach(function(c) {
                var category_id = c.key
                var child_list = c.val();
                var item_names = Object.keys(child_list)
                item_names.forEach(function(item_name) {
                    var item = child_list[item_name];
                    if(item['sku'] == undefined) {
                        item['item_name'] = item_name;
                    } else {
                        item['item_name'] = item['name'];
                    }
                    item['category_id'] = category_id;
                    if(item['active'] == 1) {
                        inventories.push(item);
                    }
                    if(item['in_stock'] == 1 && item['active'] == 1) {
                        instock_inventory_count++;
                    }
                })
            });
            console.log('List of inventories:' + JSON.stringify(inventories));
            if(cb != undefined) {
                cb(undefined, "SUCCESS");
            }
        })
    } else {
        console.log('List of inventories already loaded.')
        if(cb != undefined) {
            cb(undefined, "SUCCESS")
        }
    }
}

reCalculateInventories = function(cb) {
    instock_inventory_count = 0;
    inventories.forEach(function(item) {
        if(item['in_stock'] == 1 && item['active'] == 1) {
            instock_inventory_count++;
        }
    });
    cb()
}

loadCategoryItemsFromDB = function(force, cb) {
    if(force) {
        categories.length = 0; 
    }
    if(categories.length <= 0) {
        console.log('Loading categories from DB');
        db.ref('suppliers/' + loggedinUser['uid'] + '/tags').once('value', function(snapshot) {
            console.log('Categories had come now..');
            snapshot.forEach(function(c) {
                var item = {}
                item['category_id'] = c.key
                item['category_name'] = c.val();
                categories.push(item)
            });
            console.log('List of categories:' + JSON.stringify(categories));
            if(cb != undefined) {
                cb(undefined, "SUCCESS");
            }
        })
    } else {
        console.log('List of categories already loaded.');
        if(cb != undefined) {
            cb(undefined, "SUCCESS")
        }
    }
}

