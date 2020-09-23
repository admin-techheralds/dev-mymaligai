var selectedCategoryForDeletion = undefined;


loadCategories = function() {
    hideElement('#inventories_section')
    showElement('#categories_section')
    hideElement('#dashboard-section');
    loadCategoriesInUI();
}
initCategoryUI=function(){
    hideElement('#categories_section #addCategory')
    hideElement('#categories_section #editCategory')
    hideElement('#categories_section #categories_list')
}
closeEditCategory=function(){
    hideElement('#categories_section #addCategory')
    hideElement('#categories_section #editCategory')
    showElement('#categories_section #categories_list')
}

closeAddCategory=function(){
    hideElement('#categories_section #addCategory')
    hideElement('#categories_section #editCategory')
    showElement('#categories_section #categories_list')
}

loadCategoriesInUI = function() {

    closeEditCategory();
    closeAddInventory();

    $("#categories_edit_grid").jsGrid({
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
        noDataContent : "No Categories are found",
        fields: [
            { title : "Name", name: "category_name", type: "text", width: 250,
                cellRenderer  : function(value, item) {
                    return '<td><h5><span class="badge badge-secondary">' + value + ' </span></h5></td>';
                } 
            },
            {  
                type: "control",
                modeSwitchButton: false,
                editButton: true,
                headerTemplate: function() {
                    return $("<button>").addClass("btn btn-block btn-info").attr("type", "button").text("Add")
                            .on("click", function () {
                                showAddCategoryView();
                            });
                }
            }
        ],
        data: categories,
        deleteConfirm: function(item) {
            return "Category item \"" + item.category_name + "\" will be removed. Are you sure?";
        },
        editItem : function(item) {
            var $row = $("#categories_edit_grid").jsGrid("rowByItem", item);
            editSelectedCategoryItem(item, $row);
        },
        deleteItem : function(item) {
            deleteSelectedCategoryItem(item);       
        }
    });
}

deleteSelectedCategoryItem = function(item) {
    selectedCategoryForDeletion = item;
    showConfirmationDialog("Do you want to delete the Category: <strong>" + item.category_name + "</strong> ?",
                            deleteSelectedCategoryConfirmation);

}

deleteSelectedCategoryConfirmation = function() {
    
    var category_id = selectedCategoryForDeletion['category_id'];
    // ale                                                                                         rt0.('ID:' + category_id);
    db.ref('inventory/' + loggedinUser['uid'] + '/' + category_id).once('value', function(snapshot){
        var skus = []
        snapshot.forEach(function(sku) {
            skus.push(sku);
        });
        console.log('SKU:' + skus);
        if(skus.length > 0) {
            showErrorAlert("Cannot delete the selected category as Invenory items are mapped to this category.");
            return;
        }
        db.ref('suppliers/' + loggedinUser['uid'] + '/tags/' + category_id).remove(function() {

            //reload the categories
            loadCategoryItemsFromDB(true, function() {
                showInfoAlert("Successfully deleted the selected category.");
                $('#categories_edit_grid').jsGrid('refresh');
                updateInventoriesUI();
            })
        }).catch(function(err) {
            console.log('Failed to delete the selected category. Error:' + err);
            showErrorAlert("Failed to delete the Category.");    
            return;
        })
    }).catch(function(err) {
        console.log('Failed to delete the selected category. Error:' + err);
        showErrorAlert("Failed to delete the Category.");
        return;
    });
}

showAddCategoryView = function() {
    
    initCategoryUI();
    $('#addCategory #category_name').val('')
    showElement('#categories_section #addCategory')
    resetLoadingBtn('#addCategory #btnSaveInventory', 'Save');
}
isCategoryAlreadyExist=function(name){
    var found=false;
    for(var i=0;i<categories.length;i++){
        var cat=categories[i]
        if(cat['category_name'].toLowerCase()==name){
            found=true;
            break;
        }
    }
    return found;
}

addCategory=function(){
    var category_name = $.trim($('#addCategory #category_name').val())
    if(category_name.length <= 0) {
        showErrorAlert("Category name cannot be empty");
        $('#addCategory #category_name').focus();
        return;
    }
    if(isCategoryAlreadyExist(category_name)){
        showErrorAlert("Category name already exists.");
        $('#addCategory #category_name').focus();
        return;
    }
    var id=new Date().getTime()+""
    db.ref('suppliers/' + loggedinUser['uid'] + '/tags').update({
        id: category_name
    }).then(function() {
        console.log('Successfully saved the category item.');
        categories.push({
            "category_id": id,
            "category_name": category_name
        })
        resetLoadingBtn('#addCategory #btnSaveCategory', 'Save');
        $("#categories_edit_grid").jsGrid('refresh');
        closeAddCategory();
    }).catch(function(err) {
        console.log('Failed to save the Category. Error:' + err);
        showErrorAlert("Failed to save the Category.");
        resetLoadingBtn('#addCategory #btnSaveCaetgory', 'Save');
    });

}


editSelectedCategoryItem = function(item, $row) {
    console.log("Editing item:" + JSON.stringify(item));
    if(item == undefined || item['category_name'] == undefined) {
        return;
    }

    
    $('#editCategory #category_name').val(item['category_name']);
    $('#editCategory #selected_category_to_edit').html(item['category_name']);
    $('#editCategory #selected_row_for_edit_category').val(JSON.stringify(item));
    
    resetLoadingBtn('#editCategory #btnSaveICategory', 'Save');

    initCategoryUI();
    showElement('#categories_section #editCategory')
}

isThereAnyCategoryNameMatchingForEdit=function(id, name) {
    var found=false;
    for(var i=0;i<categories.length;i++){
        var cat=categories[i]
        if(id == cat['category_id']) {
            continue;
        }
        if(cat['category_name'].toLowerCase()==name){
            found=true;
            break;
        }
    }
    return found;
}

editSaveCategory=function(){
    var category_name = $.trim($('#editCategory #category_name').val())
    if(category_name.length <= 0) {
        showErrorAlert("Category name cannot be empty");
        $('#editCategory #category_name').focus();
        return;
    }
    var selected_category=JSON.parse($('#editCategory #selected_row_for_edit_category').val())
    if(selected_category == undefined) {
        showErrorAlert("Something went wrong while editing the category name");
        $('#editCategory #category_name').focus();
        return;
    }
    if(isThereAnyCategoryNameMatchingForEdit(selected_category['category_id'], category_name)) {
        showErrorAlert("Given category name already exists");
        $('#editCategory #category_name').focus();
        return;
    }
    var update_value = {

    }
    for(var i=0;i<categories.length;i++){
        var cat=categories[i];
        if(selected_category['category_id'] == cat['category_id']) {
           cat['category_name'] = category_name
        }
        update_value[cat['category_id']] = cat['category_name'];
    }


    db.ref('suppliers/' + loggedinUser['uid'] + '/tags').update(update_value).then(function() {
        console.log('Successfully updated the category item.');
        resetLoadingBtn('#editCategory #btnSaveCategory', 'Save');
        $("#categories_edit_grid").jsGrid('refresh');
        closeEditCategory();
    }).catch(function(err) {
        console.log('Failed to edit/ save the Category. Error:' + err);
        showErrorAlert("Failed to edit/save the Category.");
        resetLoadingBtn('#editCategory #btnSaveCaetgory', 'Save');
    });

}
/*
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
            updateUI();
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

*/